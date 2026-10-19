#!/usr/bin/env python3
"""Build and verify Foyer locally using Java 17 and the Android SDK."""
import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'app.foyer.launcher'
ANDROID = '{http://schemas.android.com/apk/res/android}'

class BuildError(Exception):
    pass

def read_apps(path):
    apps = []
    for line in Path(path).read_text().splitlines():
        value = line.strip()
        if not value or value.startswith('#'):
            continue
        if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+', value):
            raise BuildError('Invalid package in apps file: ' + value)
        if value not in apps:
            apps.append(value)
    return apps

def run(*args, env=None):
    p = subprocess.run([str(a) for a in args], env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode:
        raise BuildError(p.stdout.strip() or ('Command failed: ' + str(args[0])))
    return p.stdout.strip()

def write_private(path, value):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd = os.open(str(path), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as f:
        f.write(value + '\n')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apps', default=str(ROOT / 'apps.txt'))
    parser.add_argument('--keystore', help='Existing keystore; use together with --password-file.')
    parser.add_argument('--password-file', help='Private file containing the keystore password on its first line.')
    parser.add_argument('--alias', default='foyer')
    args = parser.parse_args()
    apps = read_apps(args.apps)
    manifest = ET.parse(ROOT / 'AndroidManifest.xml').getroot()
    if manifest.get('package') != PACKAGE:
        raise BuildError('Manifest package does not match Foyer.')
    if manifest.findall('uses-permission'):
        raise BuildError('This build expects a launcher with no requested Android permissions.')
    sdk_node = manifest.find('uses-sdk')
    minimum = sdk_node.get(ANDROID + 'minSdkVersion')
    target = sdk_node.get(ANDROID + 'targetSdkVersion')
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise BuildError('Set ANDROID_HOME to your Android SDK. See docs/BUILD.md.')
    sdk = Path(sdk).expanduser().resolve()
    bt = sdk / 'build-tools/34.0.0'
    jar = sdk / 'platforms/android-34/android.jar'
    for file in [jar, bt / 'aapt2', bt / 'd8', bt / 'zipalign', bt / 'apksigner', bt / 'core-lambda-stubs.jar']:
        if not file.is_file():
            raise BuildError('Missing SDK file: ' + str(file) + '. Install platforms;android-34 and build-tools;34.0.0.')
    java_home = os.environ.get('JAVA_HOME')
    javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
    keytool = str(Path(java_home) / 'bin/keytool') if java_home else shutil.which('keytool')
    if not javac or not keytool:
        raise BuildError('Java 17 JDK is required. Set JAVA_HOME.')
    if not re.search(r'javac 17\.', run(javac, '-version')):
        raise BuildError('Use Java 17 for this build and set JAVA_HOME accordingly.')
    env = dict(os.environ)
    if java_home:
        env['PATH'] = str(Path(java_home) / 'bin') + os.pathsep + env.get('PATH', '')
    if bool(args.keystore) != bool(args.password_file):
        raise BuildError('Supply both --keystore and --password-file, or neither.')
    keystore = Path(args.keystore).expanduser().resolve() if args.keystore else ROOT / '.secrets/foyer.jks'
    password = Path(args.password_file).expanduser().resolve() if args.password_file else ROOT / '.secrets/keystore.pass'
    if args.keystore and (not keystore.is_file() or not password.is_file()):
        raise BuildError('The supplied signing key and password file must already exist.')
    if not args.keystore:
        if keystore.exists() != password.exists():
            raise BuildError('Signing key or password is missing. Restore the matching files; do not replace an existing signing identity.')
        if not keystore.exists():
            write_private(password, secrets.token_urlsafe(36))
            try:
                run(keytool, '-genkeypair', '-keystore', keystore, '-storepass:file', password,
                    '-keypass:file', password, '-alias', args.alias, '-keyalg', 'RSA', '-keysize', '3072',
                    '-validity', '10000', '-dname', 'CN=Foyer', env=env)
                keystore.chmod(0o600)
            except BaseException:
                if not keystore.exists():
                    password.unlink(missing_ok=True)
                raise
    out = ROOT / 'out'
    if out.is_symlink():
        raise BuildError('Refusing to replace an out/ directory that is a symlink.')
    if out.exists():
        shutil.rmtree(out)
    for folder in ['classes', 'dex', 'gen/app/foyer/launcher']:
        (out / folder).mkdir(parents=True, exist_ok=True)
    values = ',\n'.join('    ' + json.dumps(p) for p in apps)
    (out / 'gen/app/foyer/launcher/Apps.java').write_text('package ' + PACKAGE + ';\nfinal class Apps { static final String[] LIST = {\n' + values + '\n}; }\n')
    run(bt / 'aapt2', 'compile', '--dir', ROOT / 'res', '-o', out / 'resources.zip', env=env)
    run(bt / 'aapt2', 'link', '-o', out / 'base.apk', '-I', jar, '--manifest', ROOT / 'AndroidManifest.xml',
        '--min-sdk-version', minimum, '--target-sdk-version', target, out / 'resources.zip', env=env)
    sources = sorted((ROOT / 'src').rglob('*.java')) + sorted((out / 'gen').rglob('*.java'))
    run(javac, '-nowarn', '-Xlint:-options', '-source', '1.8', '-target', '1.8', '-bootclasspath',
        str(jar) + os.pathsep + str(bt / 'core-lambda-stubs.jar'), '-d', out / 'classes', *sources, env=env)
    classes = sorted((out / 'classes').rglob('*.class'))
    run(bt / 'd8', '--min-api', minimum, '--release', '--lib', jar, '--output', out / 'dex', *classes, env=env)
    shutil.copyfile(out / 'base.apk', out / 'unsigned.apk')
    with zipfile.ZipFile(out / 'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as z:
        z.write(ROOT / 'LICENSE', 'assets/LICENSE')
        for file in sorted((out / 'dex').glob('*.dex')):
            z.write(file, file.name)
    run(bt / 'zipalign', '-f', '4', out / 'unsigned.apk', out / 'aligned.apk', env=env)
    apk = out / 'foyer.apk'
    run(bt / 'apksigner', 'sign', '--ks', keystore, '--ks-key-alias', args.alias,
        '--ks-pass', 'file:' + str(password), '--out', apk, out / 'aligned.apk', env=env)
    verification = run(bt / 'apksigner', 'verify', '--verbose', '--print-certs', apk, env=env)
    cert = re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)', verification)
    if not cert:
        raise BuildError('Could not verify the signing certificate.')
    badging = run(bt / 'aapt2', 'dump', 'badging', apk, env=env)
    if "package: name='" + PACKAGE + "'" not in badging or 'uses-permission:' in badging:
        raise BuildError('Built APK package or permissions do not match the expected Foyer app.')
    metadata = {'package': PACKAGE, 'version_name': manifest.get(ANDROID + 'versionName'),
                'version_code': int(manifest.get(ANDROID + 'versionCode')), 'minimum_sdk': int(minimum),
                'target_sdk': int(target), 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
                'signer_certificate_sha256': cert.group(1)}
    apk.with_suffix('.apk.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print('Built and signature-verified: ' + str(apk))
    print('Keep the APK metadata beside it. Back up the private signing files separately from Git.')

if __name__ == '__main__':
    try:
        main()
    except (BuildError, OSError, ValueError, ET.ParseError) as error:
        print('Error: ' + str(error), file=sys.stderr)
        sys.exit(1)
