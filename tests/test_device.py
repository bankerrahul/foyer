import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class DeviceWorkflowTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.device_file = self.base / 'device.json'
        self.initial = {'original': 'com.vendor.home/.Launcher', 'home': 'com.vendor.home/.Launcher',
                        'packages': ['com.vendor.home', 'org.example.player'], 'disabled': [],
                        'app_data': {'com.vendor.home': 'retain', 'org.example.player': 'retain'}}
        self.save_device(self.initial)
        self.apk = self.base / 'foyer.apk'
        self.apk.write_bytes(b'test APK fixture')
        self.metadata = self.apk.with_suffix('.apk.json')
        self.metadata.write_text(json.dumps({'package': 'app.foyer.launcher', 'sha256': hashlib.sha256(self.apk.read_bytes()).hexdigest()}))
        self.env = dict(os.environ, ADB=str(ROOT / 'tests/fake_adb.py'), FAKE_ADB_STATE=str(self.device_file))
        (ROOT / 'tests/fake_adb.py').chmod(0o755)
        self.states = self.base / 'states'

    def device(self):
        return json.loads(self.device_file.read_text())

    def save_device(self, value):
        self.device_file.write_text(json.dumps(value))

    def install(self, *options):
        return self.run_cli('install', '--apk', str(self.apk), '--state-dir', str(self.states), *options)

    def run_cli(self, command, *options):
        return subprocess.run([sys.executable, str(ROOT / 'scripts/device.py'), command,
                               '--serial', 'fake-device-1', *options], env=self.env,
                              text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)

    def state_file(self):
        return next(self.states.glob('*.json'))

    def assert_original_available(self):
        d = self.device()
        self.assertEqual(d['home'], 'com.vendor.home/.Launcher')
        self.assertEqual(d['disabled'], [])
        self.assertEqual(d['app_data'], self.initial['app_data'])
        self.assertIn('org.example.player', d['packages'])

    def test_preview_installs_without_changing_home_or_disabling_packages(self):
        p = self.install()
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertIn('app.foyer.launcher', self.device()['packages'])
        self.assertEqual(self.device()['focus'], 'app.foyer.launcher')
        self.assertEqual(json.loads(self.state_file().read_text())['original_home'], 'com.vendor.home/.Launcher')

    def test_install_does_not_add_foyer_to_an_inactive_secondary_profile(self):
        secondary = ['org.example.private']
        self.save_device(dict(self.initial, secondary_packages=secondary))
        p = self.install()
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertIn('app.foyer.launcher', self.device()['packages'])
        self.assertEqual(self.device()['secondary_packages'], secondary)

    def test_disabling_home_redirect_preserves_other_accessibility_services(self):
        self.save_device(dict(self.initial, accessibility_services='org.example.reader/.Service:app.foyer.launcher/.HomeButtonService:org.example.remote/.Service', accessibility_enabled='1'))
        p = self.run_cli('disable-home-redirect')
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertEqual(self.device()['accessibility_services'], 'org.example.reader/.Service:org.example.remote/.Service')
        self.assertEqual(self.device()['accessibility_enabled'], '1')
        self.assert_original_available()

    def test_disabling_only_home_redirect_returns_accessibility_to_off(self):
        self.save_device(dict(self.initial, accessibility_services='app.foyer.launcher/app.foyer.launcher.HomeButtonService', accessibility_enabled='1'))
        p = self.run_cli('disable-home-redirect')
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertEqual(self.device()['accessibility_services'], 'null')
        self.assertEqual(self.device()['accessibility_enabled'], '0')
        self.assert_original_available()

    def test_auto_order_reset_is_accepted_without_changing_home(self):
        p = self.install('--apps', 'auto')
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertIn('app.foyer.launcher', self.device()['packages'])

    def test_activation_preserves_all_packages_and_needs_no_disable_by_default(self):
        p = self.install('--activate')
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertEqual(self.device()['home'], 'app.foyer.launcher/.MainActivity')
        self.assertEqual(self.device()['disabled'], [])
        self.assertEqual(self.device()['app_data'], self.initial['app_data'])

    def test_oem_override_rolls_back_without_automatic_disabling(self):
        self.save_device(dict(self.initial, force_stock=True))
        p = self.install('--activate')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertIn('app.foyer.launcher', self.device()['packages'])

    def test_explicit_launcher_switch_and_recovery_restore_original_enabled_state(self):
        self.save_device(dict(self.initial, force_stock=True, enabled_state=1))
        p = self.install('--activate', '--disable-launcher', 'com.vendor.home')
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertEqual(self.device()['disabled'], ['com.vendor.home'])
        p = self.run_cli('recover', '--state', str(self.state_file()))
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertEqual(self.device()['enabled_state'], 1)
        self.assertIn('app.foyer.launcher', self.device()['packages'])

    def test_partial_disable_failure_is_recovered(self):
        self.save_device(dict(self.initial, fail_disable_after_change=True))
        p = self.install('--activate', '--disable-launcher', 'com.vendor.home')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()

    def test_activation_failure_restores_home_and_retains_new_launcher(self):
        self.save_device(dict(self.initial, fail_activate=True))
        p = self.install('--activate', '--disable-launcher', 'com.vendor.home')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertIn('app.foyer.launcher', self.device()['packages'])

    def test_failed_recovery_retains_state_and_never_uninstalls_fallback(self):
        self.save_device(dict(self.initial, fail_activate=True, fail_restore=True))
        p = self.install('--activate', '--disable-launcher', 'com.vendor.home')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertIn('app.foyer.launcher', self.device()['packages'])
        self.assertEqual(json.loads(self.state_file().read_text())['status'], 'recovery_failed')

    def test_recovery_refuses_a_different_device_without_changing_it(self):
        p = self.install('--activate')
        self.assertEqual(p.returncode, 0, p.stdout)
        d = self.device()
        d['identity'] = 'FAKE-DEVICE-B'
        self.save_device(d)
        p = self.run_cli('recover', '--state', str(self.state_file()))
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertIn('different device', p.stdout.lower())
        self.assertEqual(self.device()['home'], 'app.foyer.launcher/.MainActivity')

    def test_unrelated_package_cannot_be_disabled(self):
        p = self.install('--activate', '--disable-launcher', 'com.android.vending')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertNotIn('app.foyer.launcher', self.device()['packages'])

    def test_bad_apk_checksum_aborts_before_install(self):
        self.apk.write_bytes(b'changed after metadata was written')
        p = self.install('--activate')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertNotIn('app.foyer.launcher', self.device()['packages'])

    def test_signature_rejection_does_not_uninstall_or_clear_existing_app(self):
        d = dict(self.initial, fail_install=True)
        d['packages'] = self.initial['packages'] + ['app.foyer.launcher']
        d['app_data'] = dict(self.initial['app_data'], **{'app.foyer.launcher': 'saved preferences'})
        self.save_device(d)
        p = self.install()
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertIn('UPDATE_INCOMPATIBLE', p.stdout)
        self.assertEqual(self.device()['app_data']['app.foyer.launcher'], 'saved preferences')
        self.assertIn('app.foyer.launcher', self.device()['packages'])

    def test_apps_input_cannot_inject_remote_shell_commands(self):
        p = self.install('--apps', 'org.example.player;reboot')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertNotIn('app.foyer.launcher', self.device()['packages'])

    def test_secondary_android_user_is_rejected(self):
        self.save_device(dict(self.initial, user=10))
        p = self.install('--activate')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertNotIn('app.foyer.launcher', self.device()['packages'])

    def test_unwritable_recovery_location_aborts_before_install(self):
        p = self.install('--activate', '--state-dir', str(self.apk))
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assert_original_available()
        self.assertNotIn('app.foyer.launcher', self.device()['packages'])

    def test_update_keeps_an_active_foyer_home_and_its_preferences(self):
        d = dict(self.initial, home='app.foyer.launcher/.MainActivity')
        d['packages'] = self.initial['packages'] + ['app.foyer.launcher']
        d['app_data'] = dict(self.initial['app_data'], **{'app.foyer.launcher': 'saved favourites'})
        self.save_device(d)
        p = self.install()
        self.assertEqual(p.returncode, 0, p.stdout)
        self.assertEqual(self.device()['home'], 'app.foyer.launcher/.MainActivity')
        self.assertEqual(self.device()['app_data']['app.foyer.launcher'], 'saved favourites')
        self.assertEqual(self.device()['disabled'], [])

    def test_existing_foyer_home_is_not_accepted_as_its_own_activation_fallback(self):
        d = dict(self.initial, home='app.foyer.launcher/.MainActivity')
        d['packages'] = self.initial['packages'] + ['app.foyer.launcher']
        self.save_device(d)
        p = self.install('--activate')
        self.assertNotEqual(p.returncode, 0, p.stdout)
        self.assertIn('already Home', p.stdout)
        self.assertEqual(self.device()['home'], 'app.foyer.launcher/.MainActivity')

if __name__ == '__main__':
    unittest.main()
