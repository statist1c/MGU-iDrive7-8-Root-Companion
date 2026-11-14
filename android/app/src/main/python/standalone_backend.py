"""Single-app coordinator. Upstream flash/enrollment bytes remain unchanged."""
import argparse
import contextlib
import io
import ipaddress
import json
import os
import re
import socket
import sys
import time

READ_ONLY = {'inspect', 'status', 'preflight'}
ACTIONS = READ_ONLY | {'flash', 'fix-key', 'enable', 'persist', 'close', 'disable'}
_check = None


def local_ip(value):
    ip = ipaddress.IPv4Address(value)
    return (ip.is_link_local and ip.packed[2] not in (0,255)) or any(ip in ipaddress.IPv4Network(n) for n in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16'))


def validate(request, check=None, now=None):
    action = request.get('action')
    if action not in ACTIONS:
        raise ValueError('Unsupported action')
    if request.get('arch') not in {'id7', 'id8-bdc3', 'id8-bcp'}:
        raise ValueError('Choose the real head-unit variant')
    source = ipaddress.IPv4Address(request['source'])
    gateway = ipaddress.IPv4Address(request['gateway'])
    transport = request.get('transport', 'ethernet')
    if transport not in ('ethernet', 'wifi'):
        raise ValueError('Unsupported transport')
    valid = local_ip(str(source)) and local_ip(str(gateway)) if transport == 'wifi' else source.is_link_local and gateway.is_link_local and source.packed[2] not in (0,255)
    if not valid or source == gateway:
        raise ValueError('A usable assigned link-local Ethernet source and different gateway are required')
    override = request.get('ssh_override', '')
    if override and (transport != 'wifi' or not local_ip(override)):
        raise ValueError('Invalid Wi-Fi SSH override')
    vin = request.get('vin', '')
    if action != 'inspect' and not re.fullmatch(r'[A-HJ-NPR-Z0-9]{17}', vin):
        raise ValueError('Enter the exact VIN')
    if action not in READ_ONLY:
        if request.get('confirm') != 'I AUTHORIZE ' + vin:
            raise ValueError('Typed vehicle authorization is required')
        if action in {'flash', 'fix-key'} and request.get('backup') is not True:
            raise ValueError('Confirm a verified restorable backup and stable power')
        if transport == 'wifi' and action in {'flash','fix-key'} and request.get('wifi_ack') is not True:
            raise ValueError('Acknowledge Wi-Fi flash interruption risk')
        now = time.monotonic() if now is None else now
        if not check or not 0 <= now - check['time'] <= 300:
            raise ValueError('Run a successful compatibility check within five minutes')
        for name in ('vin', 'arch', 'source', 'gateway', 'interface', 'transport', 'network_id', 'ssh_override'):
            if request.get(name) != check.get(name):
                raise ValueError('Compatibility check no longer matches ' + name)


class LineWriter(io.TextIOBase):
    def __init__(self, callback):
        self.callback, self.pending = callback, ''

    def write(self, value):
        self.pending += value
        while '\n' in self.pending:
            line, self.pending = self.pending.split('\n', 1)
            self.callback.onLine(line[:8192])
        return len(value)

    def flush(self):
        if self.pending:
            self.callback.onLine(self.pending[:8192]); self.pending = ''


def bind_transport(source, hsfz, enroll, guard=None):
    def check(host):
        if not local_ip(host):
            raise ValueError("Only private or usable link-local vehicle endpoints are permitted")
        if guard is not None:
            guard.assertAvailable()
    def connect(client):
        client.close()
        check(client.gateway)
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            sock.settimeout(8)
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.bind((source, 0)); sock.connect((client.gateway, client.port))
            sock.settimeout(None)
            client.sock = sock; client.buf = b''
        except BaseException:
            sock.close(); raise
    def probe(host, port=22, timeout=3.0):
        check(host)
        start = time.monotonic()
        try:
            with socket.create_connection((host, port), timeout=timeout, source_address=(source, 0)):
                return True, (time.monotonic() - start) * 1000
        except OSError:
            return False, (time.monotonic() - start) * 1000
    hsfz.EnetClient.connect = connect
    enroll.probe_port = probe


def run(root, request_json, callback, ssh_adapter, route_guard=None):
    global _check
    writer = LineWriter(callback)
    with contextlib.redirect_stdout(writer), contextlib.redirect_stderr(writer):
        try:
            req = json.loads(request_json)
            validate(req, _check)
            action, source, gateway = req['action'], req['source'], req['gateway']
            # An unsuccessful or interrupted new check cannot retain an older gate.
            if action == 'inspect':
                _check = None
            if route_guard is not None:
                route_guard.assertAvailable()
            # Read-only TCP route check; no diagnostic programming traffic.
            with socket.create_connection((gateway, 6801), timeout=5, source_address=(source, 0)):
                pass
            if root not in sys.path:
                sys.path.insert(0, root)
            import ssh_enable as backend
            from mgu7 import diag, enroll, hsfz, preflight, sshcli
            bind_transport(source, hsfz, enroll, route_guard)

            def native_ssh(host, key_path, command, user='root', port=22, timeout=45, stdin_data=None):
                if isinstance(stdin_data, str):
                    stdin_data = stdin_data.encode('utf-8')
                # Base64 is a lossless boundary, with no byte-array signedness ambiguity.
                import base64
                payload = base64.b64encode(stdin_data or b'').decode('ascii')
                result = json.loads(str(ssh_adapter.run(host, key_path, command, user, port, int(timeout), payload)))
                return result['rc'], result['out'], result['err']

            sshcli.available = lambda: True
            sshcli.run = native_ssh
            args = argparse.Namespace(gateway=gateway, arch=req['arch'], vin=None, force=False, mgu_ip=req.get('ssh_override') or None, verbose=False)
            tool = backend.Tool(args)
            try:
                tool.connect()
                if not tool.svk or not tool.svk.is_supported_mgu():
                    raise ValueError('Unsupported MGU BTLD; no force override exists')
                actual = preflight.read_vin(tool.client)
                if not actual or not re.fullmatch(r'[A-HJ-NPR-Z0-9]{17}', actual):
                    raise ValueError('Cannot read a valid physical head-unit VIN')
                if action != 'inspect' and actual != req['vin']:
                    raise ValueError('Live head-unit VIN differs from the authorized VIN')
                if action == 'inspect':
                    result = dict(vin=actual, arch=req['arch'], source=source, gateway=gateway,
                                  interface=req['interface'], transport=req.get('transport'), network_id=req.get('network_id'), ssh_override=req.get('ssh_override'), svk=tool.svk.summary(), mgu_ip=tool.mgu_ip,
                                  debug=diag.debug_state(tool.client),
                                  ssh_port_open=bool(tool.mgu_ip and tool.probe(tool.mgu_ip)))
                    _check = dict(result, time=time.monotonic())
                    return json.dumps(dict(rc=0, result=result))
            finally:
                if tool.client:
                    tool.client.close()
            # Consume the check before mutation; a repeat requires another inspection.
            if action not in READ_ONLY:
                _check = None
            ssh_adapter.setVehicle(req['vin'], action in {'flash', 'fix-key'})
            argv = [action, '--gateway', gateway, '--arch', req['arch'], '--vin', actual]
            if req.get('ssh_override'):
                argv.extend(['--mgu-ip', req['ssh_override']])
            if action in {'flash', 'fix-key', 'enable'}:
                argv.append('--persist')
            code = backend.main(argv) or 0
            return json.dumps(dict(rc=code, result=None))
        except BaseException as exc:
            if not isinstance(exc, (Exception, SystemExit)):
                raise
            _check = None
            message = str(exc)
            print('FAILED:', message)
            return json.dumps(dict(rc=1, error=message))
        finally:
            writer.flush()
