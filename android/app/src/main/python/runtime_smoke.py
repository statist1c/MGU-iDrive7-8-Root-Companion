# SPDX-License-Identifier: GPL-3.0-only
"""Local runtime/asset checks only: never opens a socket or sends vehicle data."""
import json
import sys


def run(root, callback):
    if root not in sys.path:
        sys.path.insert(0, root)
    import cryptography
    import ssh_enable
    from cryptography import x509
    from mgu7 import assets, enroll, flash, identity
    material = assets.Assets()
    enrollment_key = enroll.RsaKeyPair.from_pem_bytes(material.raw(1), expect_pkcs8=True)
    pmo_key = enroll.RsaKeyPair.from_pem_bytes(material.raw(4), expect_pkcs8=False)
    certificate = enroll.build_certificate('0' * 64, enrollment_key, material.raw(3), pmo_key)
    x509.load_der_x509_certificate(certificate.der)
    bundle = identity.Identity.from_pem(material.raw(5)).formats()
    assert 'id_rsa.pkcs1.pem' in bundle and 'id_rsa.ppk' in bundle
    flash.SecurityOperands(material.raw(7))
    contracts = {}
    for arch, contract in flash.CONTRACTS.items():
        data = flash.FlashData.build(arch, 'WBA00000000000000', material.raw(int(contract[3])))
        contracts[arch] = len(data.frames)
        assert len(data.frames) > 100
    callback.onLine('Embedded Python-to-Java callback OK')
    return json.dumps(dict(python=sys.version.split()[0], cryptography=cryptography.__version__,
                           certificate_bytes=len(certificate.der), contracts=contracts, vehicle_traffic=False))
