import hashlib
import json
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / 'out/foyer.apk'

@unittest.skipUnless(APK.is_file(), 'Build Foyer first to check the actual APK artifact.')
class BuildArtifactTests(unittest.TestCase):
    def test_distributed_apk_preserves_the_complete_mit_licence(self):
        with zipfile.ZipFile(APK) as z:
            self.assertIn('assets/LICENSE', z.namelist())
            self.assertEqual(z.read('assets/LICENSE'), (ROOT / 'LICENSE').read_bytes())

    def test_install_metadata_matches_the_actual_signed_apk(self):
        metadata = json.loads(APK.with_suffix('.apk.json').read_text())
        self.assertEqual(metadata['sha256'], hashlib.sha256(APK.read_bytes()).hexdigest())
