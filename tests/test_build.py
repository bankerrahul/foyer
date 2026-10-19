import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class BuildInputTests(unittest.TestCase):
    def invalid_apps(self, contents, expected):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'apps.txt'
            path.write_text(contents)
            p = subprocess.run([sys.executable, str(ROOT / 'scripts/build.py'), '--apps', str(path)],
                               text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self.assertNotEqual(p.returncode, 0)
            self.assertIn(expected, p.stdout)

    def test_java_source_injection_in_apps_file_is_rejected_before_build(self):
        self.invalid_apps('org.example.player"; System.exit(1); //\n', 'Invalid package')

    def test_initial_order_can_contain_more_than_six_apps(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location('foyer_build', ROOT / 'scripts/build.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'apps.txt'
            path.write_text('org.example.a\norg.example.b\norg.example.c\norg.example.d\norg.example.e\norg.example.f\norg.example.g\n')
            self.assertEqual(module.read_apps(path), ['org.example.a', 'org.example.b', 'org.example.c', 'org.example.d', 'org.example.e', 'org.example.f', 'org.example.g'])

    def test_empty_favourites_still_allows_the_all_apps_screen(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location('foyer_build', ROOT / 'scripts/build.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'apps.txt'
            path.write_text('# No pinned apps\n')
            self.assertEqual(module.read_apps(path), [])

    def test_duplicate_favourites_are_deduplicated_without_reordering(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location('foyer_build', ROOT / 'scripts/build.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'apps.txt'
            path.write_text('org.example.plex\norg.example.tv\norg.example.plex\n')
            self.assertEqual(module.read_apps(path), ['org.example.plex', 'org.example.tv'])
