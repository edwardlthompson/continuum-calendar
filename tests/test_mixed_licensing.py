"""Root MIT vs apps/mobile GPL-3.0-only must stay honest for F-Droid."""
from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class MixedLicensingTests(unittest.TestCase):
    def test_root_license_is_mit(self) -> None:
        text = (ROOT / "LICENSE").read_text(encoding="utf-8")
        self.assertIn("MIT License", text)
        self.assertIn("Edward Thompson", text)

    def test_mobile_license_is_gpl3(self) -> None:
        text = (ROOT / "apps" / "mobile" / "LICENSE").read_text(encoding="utf-8")
        self.assertIn("GNU GENERAL PUBLIC LICENSE", text)
        self.assertIn("Version 3", text)

    def test_mobile_notice_credits_fossify(self) -> None:
        text = (ROOT / "apps" / "mobile" / "NOTICE").read_text(encoding="utf-8")
        self.assertIn("Fossify Calendar", text)
        self.assertIn("Simple Mobile Tools", text)
        self.assertIn("Edward Thompson", text)
        self.assertIn("GPL-3.0-only", text)

    def test_licensing_map_does_not_relicense_mit_trees(self) -> None:
        text = (ROOT / "LICENSING.md").read_text(encoding="utf-8")
        self.assertIn("`apps/desktop/`", text)
        self.assertIn("`kmp/`", text)
        self.assertIn("GPL-3.0-only", text)
        self.assertIn("Not relicensed", text)

    def test_fastlane_changelog_matches_version_code(self) -> None:
        props = (ROOT / "apps" / "mobile" / "gradle.properties").read_text(encoding="utf-8")
        self.assertIn("VERSION_NAME=1.0.0", props)
        self.assertIn("VERSION_CODE=26", props)
        log = ROOT / "apps" / "mobile" / "fastlane" / "metadata" / "android" / "en-US" / "changelogs" / "26.txt"
        self.assertTrue(log.is_file(), "missing Fastlane changelog for versionCode 26")
        self.assertIn("1.0.0", log.read_text(encoding="utf-8"))

    def test_fastlane_en_us_copy_is_present_and_short(self) -> None:
        base = ROOT / "apps" / "mobile" / "fastlane" / "metadata" / "android" / "en-US"
        title = (base / "title.txt").read_text(encoding="utf-8").strip()
        short = (base / "short_description.txt").read_text(encoding="utf-8").strip()
        full = (base / "full_description.txt").read_text(encoding="utf-8")
        self.assertEqual(title, "Continuum Calendar")
        self.assertLessEqual(len(short), 80)
        self.assertIn("GPL-3.0-only", full)
        self.assertNotIn("Golden Path FOSS Android stub", short)

    def test_foss_flavor_disables_google_oauth(self) -> None:
        gradle = (ROOT / "apps" / "mobile" / "app" / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('register("foss") { disableGoogleOAuth() }', gradle)
        self.assertIn('maybeCreate("release")', gradle)
        self.assertIn('signingConfigs.findByName("release")', gradle)
        settings = (ROOT / "apps" / "mobile" / "settings.gradle.kts").read_text(encoding="utf-8")
        self.assertIn("jitpack.io", settings.lower())

    def test_readme_states_mixed_license(self) -> None:
        text = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("per-directory licenses", text)
        self.assertIn("GPL-3.0-only", text)
        self.assertIn("not relicensed", text.lower())
        self.assertIn("badge/license-MIT", text)


if __name__ == "__main__":
    unittest.main()
