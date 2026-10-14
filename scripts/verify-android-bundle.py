#!/usr/bin/env python3
"""Verify JAR integrity and match the upload certificate without logging secrets."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile


def run_public_tool(args):
    result = subprocess.run(args, capture_output=True, text=True)
    if result.returncode:
        raise SystemExit(f'{args[0]} certificate inspection failed; check the path, alias and passwords.')
    return result.stdout


def fingerprint(output):
    matches = re.findall(r'SHA256:\s*([0-9A-F:]+)', output)
    if len(matches) != 1:
        raise SystemExit('Expected exactly one signing certificate SHA-256 fingerprint.')
    return matches[0]


bundle = Path(sys.argv[1]).resolve()
with zipfile.ZipFile(bundle) as archive:
    names = archive.namelist()
    if 'META-INF/MANIFEST.MF' not in names or not any(
        name.startswith('META-INF/') and name.endswith(('.RSA', '.EC', '.DSA')) for name in names
    ):
        raise SystemExit('Bundle has no JAR signing metadata.')

# Strict verification reports unsigned entries and integrity errors as failures.
# Exit 4 is the expected Android self-signed upload certificate trust warning.
result = subprocess.run(
    ['jarsigner', '-J-Duser.language=en', '-J-Duser.country=US', '-verify', '-strict', str(bundle)],
    capture_output=True, text=True,
)
report = result.stdout + result.stderr
if result.returncode not in (0, 4) or not re.search(r'jar verified[. ]', report):
    raise SystemExit('Bundle signature verification failed.')
if result.returncode == 4 and ('self-signed' not in report or 'expired' in report or 'not yet valid' in report):
    raise SystemExit('Unexpected signing certificate validation error.')

locale = ['-J-Duser.language=en', '-J-Duser.country=US']
bundle_fingerprint = fingerprint(run_public_tool(['keytool', *locale, '-printcert', '-jarfile', str(bundle)]))
key_fingerprint = fingerprint(run_public_tool([
    'keytool', *locale, '-list', '-v', '-keystore', os.environ['OH_MY_HAZE_KEYSTORE_PATH'],
    '-alias', os.environ['OH_MY_HAZE_KEY_ALIAS'], '-storepass:env', 'OH_MY_HAZE_STORE_PASSWORD',
]))
if bundle_fingerprint != key_fingerprint:
    raise SystemExit('Bundle certificate does not match the configured upload key.')
print('Bundle signature verified; certificate matches the upload keystore.')
print(f'Upload certificate SHA-256: {bundle_fingerprint}')
print(f'Bundle SHA-256: {hashlib.file_digest(bundle.open("rb"), "sha256").hexdigest()}')
