#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path

path = Path(os.environ['FAKE_ADB_STATE'])
s = json.loads(path.read_text())
a = sys.argv[1:]
if a[:1] == ['-s']:
    a = a[2:]
s.setdefault('commands', []).append(a)

def finish(output='', code=0):
    path.write_text(json.dumps(s))
    print(output)
    sys.exit(code)

def home():
    if s.get('force_stock') and s['original'].split('/')[0] not in s['disabled']:
        return s['original']
    return s['home']

if a == ['get-state']:
    finish(s.get('connection', 'device'))
if a[:1] == ['install']:
    if s.get('fail_install'):
        finish('INSTALL_FAILED_UPDATE_INCOMPATIBLE', 1)
    target = a[a.index('--user') + 1] if '--user' in a else 'all'
    if target == 'all' and 'secondary_packages' in s and 'app.foyer.launcher' not in s['secondary_packages']:
        s['secondary_packages'].append('app.foyer.launcher')
    if 'app.foyer.launcher' not in s['packages']:
        s['packages'].append('app.foyer.launcher')
    finish('Success')
if a[:1] != ['shell']:
    finish('Unexpected command: ' + repr(a), 19)
a = a[1:]
if a == ['am', 'get-current-user']:
    finish(str(s.get('user', 0)))
if a == ['getprop', 'ro.serialno']:
    finish(s.get('identity', 'FAKE-DEVICE-A'))
if a == ['settings', 'get', 'secure', 'enabled_accessibility_services']:
    finish(s.get('accessibility_services', 'null'))
if a == ['settings', 'get', 'secure', 'accessibility_enabled']:
    finish(s.get('accessibility_enabled', '0'))
if a[:4] == ['settings', 'put', 'secure', 'enabled_accessibility_services']:
    s['accessibility_services'] = a[4]
    finish()
if a == ['settings', 'delete', 'secure', 'enabled_accessibility_services']:
    s['accessibility_services'] = 'null'
    finish()
if a[:4] == ['settings', 'put', 'secure', 'accessibility_enabled']:
    s['accessibility_enabled'] = a[4]
    finish()
if a == ['settings', 'get', 'secure', 'android_id']:
    finish('fake-android-id')
if a == ['getprop', 'ro.build.version.sdk']:
    finish('30')
if a[:3] == ['cmd', 'package', 'resolve-activity']:
    finish(home())
if a[:3] == ['cmd', 'package', 'query-activities']:
    values = [s['original']]
    if 'app.foyer.launcher' in s['packages']:
        values.append('app.foyer.launcher/.MainActivity')
    finish('\n'.join(v for v in values if v.split('/')[0] not in s['disabled']))
if a[:3] == ['cmd', 'package', 'set-home-activity']:
    if s.get('fail_activate') and a[-1].startswith('app.foyer.launcher/'):
        finish('set-home failed', 1)
    s['home'] = a[-1]
    finish('Success')
if a[:2] == ['cmd', 'role']:
    finish()
if a[:3] == ['pm', 'list', 'packages']:
    values = s['disabled'] if '-d' in a else s['packages']
    finish('\n'.join('package:' + p for p in values))
if a[:2] == ['pm', 'path']:
    finish('package:/data/app/base.apk' if a[-1] in s['packages'] else '')
if a[:2] == ['dumpsys', 'package']:
    value = 3 if a[-1] in s['disabled'] else s.get('enabled_state', 0)
    finish('User 0: installed=true hidden=false enabled=' + str(value))
if a[:2] == ['pm', 'disable-user']:
    if a[-1] not in s['disabled']:
        s['disabled'].append(a[-1])
    if s.get('fail_disable_after_change'):
        finish('device interrupted after disabling', 1)
    finish('new state: disabled-user')
if a[:2] in (['pm', 'default-state'], ['pm', 'enable']):
    if s.get('fail_restore'):
        finish('restoration rejected', 1)
    s['disabled'] = [p for p in s['disabled'] if p != a[-1]]
    s['enabled_state'] = 0 if a[1] == 'default-state' else 1
    finish('restored')
if a[:2] == ['am', 'start']:
    if s.get('fail_preview'):
        finish('Error: Activity not started', 1)
    s['focus'] = a[a.index('-n') + 1].split('/')[0]
    finish('Status: ok')
if a == ['input', 'keyevent', 'KEYCODE_HOME']:
    s['focus'] = home().split('/')[0]
    finish()
if a == ['dumpsys', 'window']:
    finish('mCurrentFocus=Window{test u0 ' + s.get('focus', home().split('/')[0]) + '/.MainActivity}')
finish('Unexpected command: ' + repr(a), 19)
