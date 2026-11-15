#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Package a signed APK with verified matching source and notices. Does not publish."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path, required=True)
parser.add_argument('--tag', required=True)
parser.add_argument('--out', type=Path, default=root.parent)
args = parser.parse_args()
out = args.out.resolve()
if out == root or root in out.parents:
    parser.error('--out must be outside the source project')
if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?', args.tag):
    parser.error('Use a version tag such as v0.10.0')

def digest(data):
    return hashlib.sha256(data).hexdigest()

with zipfile.ZipFile(args.apk) as apk:
    if 'assets/source-manifest.json' not in apk.namelist():
        parser.error('APK predates source stamping. Preserve its original source ZIP; do not guess a match.')
    stamp = json.loads(apk.read('assets/source-manifest.json'))
    if stamp.get('format') != 1 or args.tag != 'v' + stamp['versionName']:
        parser.error('APK source stamp/version does not match the requested tag')
    for name, expected in stamp['files'].items():
        path = (root / name).resolve()
        if root not in path.parents or not path.is_file() or digest(path.read_bytes()) != expected:
            parser.error('Source differs from APK: ' + name)
    for name in ['LICENSE', 'NOTICE', 'THIRD-PARTY.md', 'LICENSING.md']:
        if apk.read('assets/license-notices/' + name) != (root / name).read_bytes():
            parser.error('Packaged licensing file differs: ' + name)

out.mkdir(parents=True, exist_ok=True)
prefix = 'MGU-iDrive7-8-Root-Companion-' + args.tag
source_zip = out / (prefix + '-source.zip')
skip = {'.git', '.gradle', 'build', '__pycache__', '.idea', 'logs', 'enrollments'}
with zipfile.ZipFile(source_zip, 'w', zipfile.ZIP_DEFLATED) as z:
    for path in sorted(root.rglob('*')):
        rel = path.relative_to(root)
        if (not path.is_file() or any(x in skip for x in rel.parts)
                or path.suffix.lower() in {'.apk', '.pyc', '.keystore', '.jks', '.idsig'}
                or path.name in {'local.properties', 'key.properties', 'signing.properties'}):
            continue
        z.write(path, Path('MGU-iDrive7-8-Root-Companion') / rel)
with zipfile.ZipFile(source_zip) as z:
    for name, expected in stamp['files'].items():
        if digest(z.read('MGU-iDrive7-8-Root-Companion/' + name)) != expected:
            raise SystemExit('Source archive verification failed: ' + name)
    if z.testzip() is not None:
        raise SystemExit('Corrupt source archive')
notice_zip = out / (prefix + '-licenses.zip')
with zipfile.ZipFile(notice_zip, 'w', zipfile.ZIP_DEFLATED) as z:
    for name in ['LICENSE', 'NOTICE', 'THIRD-PARTY.md', 'LICENSING.md']:
        z.write(root / name, name)
    for path in sorted((root / 'licenses').rglob('*')):
        if path.is_file():
            z.write(path, path.relative_to(root))
files = [args.apk.resolve(), source_zip, notice_zip]
sums = out / (prefix + '-SHA256SUMS.txt')
sums.write_text(''.join(digest(p.read_bytes()) + '  ' + p.name + '\n' for p in files), encoding='utf-8')
print('Verified source matches the APK. Upload these beside the signed APK:')
for path in files[1:] + [sums]:
    print(path)
