#!/usr/bin/env python3
"""Install, preview, activate, or recover Foyer without deleting apps or data."""
import argparse
import hashlib
import json
import os
import re
import shlex
import subprocess
import sys
import time
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'app.foyer.launcher'
COMPONENT = PACKAGE + '/.MainActivity'
PKG = re.compile(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+')
COMP = re.compile(r'([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+)/([A-Za-z.][A-Za-z0-9_.]*)')

class WorkflowError(Exception):
    pass

def component(value):
    if not isinstance(value, str) or not COMP.fullmatch(value):
        raise WorkflowError('Invalid or unsupported Home component: ' + str(value))
    return value

def canonical(value):
    package, activity = component(value).split('/')
    return package + '/' + (package + activity if activity.startswith('.') else activity)

def save(path, state):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    temp = path.with_suffix('.tmp')
    fd = os.open(str(temp), os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as f:
        json.dump(state, f, indent=2)
        f.write('\n')
    os.replace(str(temp), str(path))

class Device:
    def __init__(self, serial):
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.:-]*', serial):
            raise WorkflowError('Use one explicit ADB serial or IP:port; spaces and shell characters are not allowed.')
        self.serial = serial
        self.executable = os.environ.get('ADB', 'adb')

    def run(self, *args, timeout=30):
        try:
            p = subprocess.run([self.executable, '-s', self.serial, *args], text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        except (OSError, subprocess.TimeoutExpired) as e:
            raise WorkflowError('ADB failed: ' + str(e)) from e
        out = p.stdout.replace('\r', '').strip()
        if p.returncode or re.search(r'(?m)^(Error:|Exception occurred|Failure \[)', out):
            raise WorkflowError(out or 'ADB command failed')
        return out

    def shell(self, *args):
        return self.run('shell', *args)

    def check(self):
        if ':' in self.serial:
            subprocess.run([self.executable, 'connect', self.serial], check=False, timeout=20,
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if self.run('get-state') != 'device':
            raise WorkflowError('Device is not connected and authorized. Approve debugging on its screen.')
        if self.shell('am', 'get-current-user') != '0':
            raise WorkflowError('Only Android user 0 is supported; no changes were made.')
        if int(self.shell('getprop', 'ro.build.version.sdk')) < 23:
            raise WorkflowError('Foyer requires Android 6 / API 23 or newer.')

    def identity(self):
        values = [self.shell('getprop', 'ro.serialno'), self.shell('settings', 'get', 'secure', 'android_id')]
        if not any(v and v not in ('null', 'unknown') for v in values):
            raise WorkflowError('Cannot identify this device reliably enough to save recovery state.')
        return hashlib.sha256(json.dumps(values).encode()).hexdigest()

    def home(self):
        out = self.shell('cmd', 'package', 'resolve-activity', '--brief', '-a',
                         'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
        for line in reversed(out.splitlines()):
            if COMP.fullmatch(line.strip()):
                return component(line.strip())
        raise WorkflowError('No working default Home app was found. Select one on the TV first.')

    def homes(self):
        out = self.shell('cmd', 'package', 'query-activities', '--brief', '-a',
                         'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
        return {canonical(s.strip()) for s in out.splitlines() if COMP.fullmatch(s.strip())}

    def enabled(self, package):
        out = self.shell('dumpsys', 'package', package)
        match = re.search(r'User 0:([^\n]+)', out)
        value = re.search(r'\benabled=(\d+)\b', match.group(1)) if match else None
        if not value or int(value.group(1)) not in (0, 1):
            raise WorkflowError('The previous Home app is not in a supported enabled state.')
        return int(value.group(1))

    def select(self, target):
        target = component(target)
        sdk = int(self.shell('getprop', 'ro.build.version.sdk'))
        if sdk >= 29:
            self.shell('cmd', 'role', 'add-role-holder', '--user', '0', 'android.app.role.HOME', target.split('/')[0])
        self.shell('cmd', 'package', 'set-home-activity', '--user', '0', target)

    def verify_home(self, target):
        if canonical(self.home()) != canonical(target):
            raise WorkflowError('Home still resolves to another launcher. No launcher will be disabled automatically.')
        self.shell('input', 'keyevent', 'KEYCODE_HOME')
        package = target.split('/')[0]
        for _ in range(10):
            lines = self.shell('dumpsys', 'window').splitlines()
            if any('mCurrentFocus=' in s and re.search(r'\s' + re.escape(package) + r'/', s) for s in lines):
                return
            time.sleep(0.5)
        raise WorkflowError('The selected Home app did not appear in front.')

def validate_state(state):
    if state.get('format') != 1 or state.get('package') != PACKAGE or state.get('user') != 0:
        raise WorkflowError('Unsupported recovery state.')
    previous = component(state.get('original_home'))
    if previous.split('/')[0] in (PACKAGE, 'android', 'com.android.tv.settings'):
        raise WorkflowError('Recovery needs an earlier state with a real previous launcher.')
    if state.get('original_enabled') not in (0, 1):
        raise WorkflowError('Invalid previous launcher state.')
    if state.get('disabled_launcher') not in (None, previous.split('/')[0]):
        raise WorkflowError('Recovery state refers to an unrelated package.')
    if not re.fullmatch(r'[a-f0-9]{64}', state.get('device_id', '')):
        raise WorkflowError('Invalid recovery device identity.')

def restore(device, state, path):
    validate_state(state)
    if state['device_id'] != device.identity():
        raise WorkflowError('Recovery state belongs to a different device; no recovery changes were made.')
    previous = state['original_home']
    if state.get('disable_attempted'):
        action = 'default-state' if state['original_enabled'] == 0 else 'enable'
        device.shell('pm', action, '--user', '0', previous.split('/')[0])
    if canonical(previous) not in device.homes():
        raise WorkflowError('The previous Home activity is unavailable. Foyer has been left installed.')
    device.select(previous)
    device.verify_home(previous)
    state['status'] = 'recovered'
    save(path, state)
    print('Previous home screen restored. Foyer remains installed.')

def install(device, args):
    if args.disable_launcher and not args.activate:
        raise WorkflowError('--disable-launcher requires --activate.')
    if args.apps and args.apps != 'auto' and any(not PKG.fullmatch(p) for p in args.apps.split(',')):
        raise WorkflowError('--apps must be auto or comma-separated Android package names.')
    apk = Path(args.apk).resolve()
    try:
        meta = json.loads(apk.with_suffix('.apk.json').read_text())
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    except (OSError, ValueError) as e:
        raise WorkflowError('Build the APK and its metadata first: ' + str(e)) from e
    if meta.get('package') != PACKAGE or meta.get('sha256') != digest:
        raise WorkflowError('APK checksum or package metadata does not match this Foyer build.')
    device.check()
    previous = device.home()
    previous_package = previous.split('/')[0]
    if previous_package in ('android', 'com.android.tv.settings'):
        raise WorkflowError('The current Home app is a system fallback, not a recoverable launcher.')
    if args.activate and previous_package == PACKAGE:
        raise WorkflowError('Foyer is already Home. Update without --activate and keep the original activation state file.')
    if args.disable_launcher and args.disable_launcher != previous_package:
        raise WorkflowError('Only the currently selected previous Home package can be explicitly disabled.')
    state = {'format': 1, 'package': PACKAGE, 'user': 0, 'serial': device.serial,
             'device_id': device.identity(), 'original_home': previous,
             'original_enabled': device.enabled(previous_package),
             'disabled_launcher': args.disable_launcher, 'disable_attempted': False,
             'activation_attempted': False, 'status': 'saved'}
    path = Path(args.state_dir).resolve() / (time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8] + '.json')
    save(path, state)
    print('Recovery state: ' + str(path), flush=True)
    out = device.run('install', '--user', '0', '--no-incremental', '-r', str(apk), timeout=120)
    if not re.search(r'(?m)^Success$', out):
        raise WorkflowError('Install did not report Success: ' + out)
    state['status'] = 'installed'
    save(path, state)
    command = ['am', 'start', '-W', '-n', COMPONENT]
    if args.apps:
        command += ['--es', 'apps', args.apps]
    device.shell(*command)
    if not args.activate:
        print('Foyer opened for preview. The previous Home selection and package states were not changed.')
        return
    try:
        state['activation_attempted'] = True
        save(path, state)
        if args.disable_launcher:
            state['disable_attempted'] = True
            save(path, state)
            device.shell('pm', 'disable-user', '--user', '0', args.disable_launcher)
        device.select(COMPONENT)
        device.verify_home(COMPONENT)
        state['status'] = 'active'
        save(path, state)
        print('Verified: Home opens Foyer. Keep the recovery state file.')
    except BaseException as cause:
        print('Activation failed; restoring the previous Home app.', file=sys.stderr)
        try:
            restore(device, state, path)
        except Exception as recovery:
            state['status'] = 'recovery_failed'
            save(path, state)
            raise WorkflowError('Recovery could not complete: ' + str(recovery) + '\nKeep Foyer installed and retry recover with: ' + str(path)) from cause
        raise

def disable_home_redirect(device):
    device.check()
    value = device.shell('settings', 'get', 'secure', 'enabled_accessibility_services')
    services = [] if value in ('null', '') else value.split(':')
    own = {PACKAGE + '/.HomeButtonService', PACKAGE + '/' + PACKAGE + '.HomeButtonService'}
    remaining = [service for service in services if service not in own]
    if remaining == services:
        print('Foyer Home redirect is already off; no settings changed.')
        return
    if remaining:
        device.shell('settings', 'put', 'secure', 'enabled_accessibility_services', shlex.quote(':'.join(remaining)))
    else:
        device.shell('settings', 'delete', 'secure', 'enabled_accessibility_services')
        device.shell('settings', 'put', 'secure', 'accessibility_enabled', '0')
    restored = device.shell('settings', 'get', 'secure', 'enabled_accessibility_services')
    if restored not in (':'.join(remaining), 'null' if not remaining else ':'.join(remaining)):
        raise WorkflowError('Could not verify the accessibility service setting.')
    print('Foyer Home redirect disabled. Other accessibility services and apps were preserved.')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    setup = sub.add_parser('install', help='Install/update and preview; changing Home is opt-in.')
    setup.add_argument('--serial', required=True)
    setup.add_argument('--apk', default=str(ROOT / 'out/foyer.apk'))
    setup.add_argument('--state-dir', default=str(ROOT / '.state'))
    setup.add_argument('--apps')
    setup.add_argument('--activate', action='store_true')
    setup.add_argument('--disable-launcher', metavar='EXACT_PREVIOUS_HOME_PACKAGE')
    recovery = sub.add_parser('recover', help='Restore the previous launcher using its saved device-specific state.')
    recovery.add_argument('--serial', required=True)
    recovery.add_argument('--state', required=True)
    redirect = sub.add_parser('disable-home-redirect', help='Turn off only Foyer Home button accessibility redirect.')
    redirect.add_argument('--serial', required=True)
    args = parser.parse_args()
    device = Device(args.serial)
    if args.command == 'disable-home-redirect':
        disable_home_redirect(device)
    elif args.command == 'install':
        install(device, args)
    else:
        path = Path(args.state).resolve()
        state = json.loads(path.read_text())
        validate_state(state)
        device.check()
        restore(device, state, path)

if __name__ == '__main__':
    try:
        main()
    except (WorkflowError, OSError, ValueError, KeyboardInterrupt) as error:
        print('Error: ' + (str(error) or 'Interrupted'), file=sys.stderr)
        sys.exit(1)
