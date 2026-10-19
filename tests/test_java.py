import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class JavaOrderTests(unittest.TestCase):
    def test_discovery_reconciliation_and_moves_preserve_every_installed_app(self):
        java_home = os.environ.get('JAVA_HOME')
        javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
        java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
        if not javac or not java:
            self.skipTest('Set JAVA_HOME to Java 17 to run the real Java ordering tests.')
        probe = subprocess.run([javac, '-version'], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if probe.returncode:
            self.skipTest('Set JAVA_HOME to Java 17 to run the real Java ordering tests.')
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([javac, '-d', directory,
                    str(ROOT / 'src/app/foyer/launcher/AppOrder.java'),
                    str(ROOT / 'tests/java/app/foyer/launcher/AppOrderTest.java')],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            result = subprocess.run([java, '-cp', directory, 'app.foyer.launcher.AppOrderTest'],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn('20 app-order cases passed', result.stdout)

    def test_home_redirect_ignores_other_screens_stale_events_and_duplicates(self):
        java_home = os.environ.get('JAVA_HOME')
        if not java_home:
            self.skipTest('Set JAVA_HOME to Java 17 for Home redirect tests.')
        javac = str(Path(java_home) / 'bin/javac')
        java = str(Path(java_home) / 'bin/java')
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([javac, '-d', directory,
                    str(ROOT / 'src/app/foyer/launcher/HomeRedirect.java'),
                    str(ROOT / 'tests/java/app/foyer/launcher/HomeRedirectTest.java')],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            result = subprocess.run([java, '-cp', directory, 'app.foyer.launcher.HomeRedirectTest'],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn('18 Home redirect cases passed', result.stdout)

    def test_appearance_defaults_and_all_layout_combinations_stay_in_bounds(self):
        java_home = os.environ.get('JAVA_HOME')
        if not java_home:
            self.skipTest('Set JAVA_HOME to Java 17 for appearance tests.')
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([str(Path(java_home) / 'bin/javac'), '-d', directory,
                    str(ROOT / 'src/app/foyer/launcher/Appearance.java'),
                    str(ROOT / 'tests/java/app/foyer/launcher/AppearanceTest.java')],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            result = subprocess.run([str(Path(java_home) / 'bin/java'), '-cp', directory,
                    'app.foyer.launcher.AppearanceTest'], text=True,
                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn('72 appearance layout cases passed', result.stdout)
