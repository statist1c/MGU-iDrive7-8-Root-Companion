"""Offline coordinator tests. No phone, Java SSH, sockets or vehicle are used."""
import base64
import json
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import MagicMock, patch

sys.path.insert(0,str(Path(__file__).parents[1]/'android/app/src/main/python'))
import standalone_backend as app

VIN='WBA12345678901234'


class StandaloneTests(unittest.TestCase):
    def setUp(self):
        app._check=None

    def request(self,action='flash',**changes):
        req=dict(action=action,arch='id7',vin=VIN,source='169.254.250.10',gateway='169.254.254.36',interface='eth0',confirm='I AUTHORIZE '+VIN,backup=True)
        req.update(changes); return req

    def check(self,**changes):
        value=self.request(); value['time']=100; value.update(changes); return value

    def test_requires_fresh_matching_check(self):
        for check in [None,self.check(time=-301),self.check(vin='WBA00000000000000'),self.check(arch='id8-bcp'),self.check(source='169.254.250.11'),self.check(interface='eth1'),self.check(gateway='169.254.254.35')]:
            with self.assertRaises(ValueError): app.validate(self.request(),check,now=100)
        app.validate(self.request(),self.check(),now=100)

    def test_rejects_unsafe_arguments(self):
        for change in [dict(action='shell'),dict(action='enter-prog'),dict(arch='guess'),dict(source='192.168.1.1'),dict(source='169.254.0.1'),dict(source='169.254.255.1'),dict(source='169.254.254.36'),dict(gateway='8.8.8.8'),dict(vin='bad'),dict(confirm='yes'),dict(backup=False),dict(backup='true')]:
            with self.assertRaises(ValueError): app.validate(self.request(**change),self.check(),now=100)

    def test_read_only_has_no_mutation_authorization(self):
        app.validate(self.request('inspect',confirm='',backup=False,vin=''))
        app.validate(self.request('status',confirm='',backup=False))
        app.validate(self.request('preflight',confirm='',backup=False))

    def test_wifi_policy_and_gate_identity(self):
        req=self.request(transport='wifi',source='192.168.4.2',gateway='192.168.4.1',interface='wlan0',network_id='123',ssh_override='192.168.4.3',wifi_ack=True)
        check=dict(req,time=100)
        app.validate(req,check,now=100)
        for field,value in [('wifi_ack',False),('network_id','124'),('ssh_override','192.168.4.4'),('transport','ethernet'),('gateway','8.8.8.8'),('source','127.0.0.1')]:
            with self.assertRaises(ValueError):app.validate(dict(req,**{field:value}),check,now=100)
        for ip in ['10.1.2.3','172.16.0.1','172.31.255.1','192.168.1.2','169.254.2.3']:self.assertTrue(app.local_ip(ip))
        for ip in ['8.8.8.8','127.0.0.1','0.0.0.0','224.0.0.1','172.32.0.1','169.254.0.1']:self.assertFalse(app.local_ip(ip))

    def test_lost_network_precedes_diagnostics(self):
        guard=MagicMock(unsafe=True);guard.assertAvailable.side_effect=OSError('selected Wi-Fi lost')
        backend,mgu=self.fixture()
        with patch.dict('sys.modules',{'ssh_enable':backend,'mgu7':mgu}),patch('standalone_backend.socket.create_connection') as connect:
            result=json.loads(app.run('/mock',json.dumps(self.request('inspect')),MagicMock(),MagicMock(),guard))
        self.assertEqual(result['rc'],1);connect.assert_not_called();backend.Tool.assert_not_called()

    def test_wifi_ssh_override_reaches_upstream(self):
        backend,mgu=self.fixture()
        req=self.request(transport='wifi',source='192.168.4.2',gateway='192.168.4.1',interface='wlan0',network_id='123',ssh_override='192.168.4.3',wifi_ack=True)
        app._check=dict(req,time=app.time.monotonic())
        result,_=self.execute(req,backend,mgu)
        self.assertEqual(result['rc'],0)
        self.assertEqual(backend.Tool.call_args.args[0].mgu_ip,'192.168.4.3')
        self.assertIn('--mgu-ip',backend.main.call_args.args[0])

    def fixture(self):
        backend=MagicMock(); backend.main.return_value=0
        tool=backend.Tool.return_value
        tool.svk.is_supported_mgu.return_value=True
        tool.svk.summary.return_value='BTLD 00003F36'
        tool.mgu_ip='169.254.36.47'; tool.probe.return_value=True
        mgu=types.ModuleType('mgu7')
        mgu.diag=types.SimpleNamespace(debug_state=lambda c:{'dtc_debug':False})
        mgu.preflight=MagicMock(); mgu.preflight.read_vin.return_value=VIN
        mgu.enroll=types.SimpleNamespace()
        mgu.hsfz=types.SimpleNamespace(EnetClient=type('Client',(),{}))
        mgu.sshcli=types.SimpleNamespace()
        return backend,mgu

    def execute(self,req,backend,mgu,ssh=None):
        callback=MagicMock()
        with patch.dict('sys.modules',{'ssh_enable':backend,'mgu7':mgu}),patch('standalone_backend.socket.create_connection') as connect:
            response=app.run('/mock/backend',json.dumps(req),callback,ssh or MagicMock())
            return json.loads(response),connect

    def test_inspection_records_physical_identity_without_backend_mutation(self):
        backend,mgu=self.fixture()
        result,connect=self.execute(self.request('inspect',vin=''),backend,mgu)
        self.assertEqual(result['rc'],0); self.assertEqual(result['result']['vin'],VIN)
        self.assertEqual(app._check['interface'],'eth0'); backend.main.assert_not_called()
        connect.assert_called_once_with(('169.254.254.36',6801),timeout=5,source_address=('169.254.250.10',0))

    def test_route_failure_precedes_all_diagnostics(self):
        backend,mgu=self.fixture()
        with patch.dict('sys.modules',{'ssh_enable':backend,'mgu7':mgu}),patch('standalone_backend.socket.create_connection',side_effect=OSError('unreachable')):
            result=json.loads(app.run('/mock/backend',json.dumps(self.request('inspect')),MagicMock(),MagicMock()))
        self.assertEqual(result['rc'],1); backend.Tool.assert_not_called(); backend.main.assert_not_called(); self.assertIsNone(app._check)

    def test_live_vin_mismatch_blocks_backend_and_clears_check(self):
        backend,mgu=self.fixture(); app._check=self.check(time=app.time.monotonic())
        mgu.preflight.read_vin.return_value='WBA00000000000000'
        result,_=self.execute(self.request(),backend,mgu)
        self.assertEqual(result['rc'],1); backend.main.assert_not_called(); self.assertIsNone(app._check)

    def test_unsupported_btld_blocks_writes(self):
        backend,mgu=self.fixture(); backend.Tool.return_value.svk.is_supported_mgu.return_value=False
        result,_=self.execute(self.request('inspect'),backend,mgu)
        self.assertEqual(result['rc'],1); backend.main.assert_not_called(); self.assertIsNone(app._check)

    def test_mutation_consumes_check_and_passes_only_expected_flags(self):
        backend,mgu=self.fixture(); app._check=self.check(time=app.time.monotonic()); ssh=MagicMock()
        result,_=self.execute(self.request(),backend,mgu,ssh)
        self.assertEqual(result['rc'],0); self.assertIsNone(app._check)
        self.assertEqual(backend.main.call_args.args[0],['flash','--gateway','169.254.254.36','--arch','id7','--vin',VIN,'--persist'])
        ssh.setVehicle.assert_called_once_with(VIN,True)
        second,_=self.execute(self.request(),backend,mgu)
        self.assertEqual(second['rc'],1)

    def test_native_ssh_boundary_preserves_binary_stdin_and_exit_status(self):
        backend,mgu=self.fixture(); app._check=self.check(time=app.time.monotonic())
        ssh=MagicMock(); ssh.run.return_value=json.dumps(dict(rc=7,out='output',err='error'))
        observed=[]
        def main(argv):
            observed.append(mgu.sshcli.run('169.254.36.47','/mock/key','cat',stdin_data=b'\x00\xff\n'))
            return 1
        backend.main.side_effect=main
        result,_=self.execute(self.request('enable'),backend,mgu,ssh)
        self.assertEqual(result['rc'],1); self.assertEqual(observed,[(7,'output','error')])
        self.assertEqual(base64.b64decode(ssh.run.call_args.args[-1]),b'\x00\xff\n')
        ssh.setVehicle.assert_called_once_with(VIN,False)

    def test_line_writer_streams_lines_and_flushes_tail(self):
        callback=MagicMock(); writer=app.LineWriter(callback)
        writer.write('one\ntwo'); writer.flush()
        self.assertEqual([call.args[0] for call in callback.onLine.call_args_list],['one','two'])


if __name__=='__main__': unittest.main()
