"""Stable NSIS installer filename helper."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MOD = ROOT / "apps" / "desktop" / "scripts" / "rename-nsis.py"


def _load():
    spec = importlib.util.spec_from_file_location("rename_nsis", MOD)
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class RenameNsisTests(unittest.TestCase):
    def test_version_from_argv(self) -> None:
        mod = _load()
        self.assertEqual(mod.version(["--upload", "1.2.3"]), "1.2.3")

    def test_tag_re_accepts_release(self) -> None:
        mod = _load()
        self.assertTrue(mod.TAG_RE.fullmatch("v1.1.1"))
        self.assertIsNone(mod.TAG_RE.fullmatch("../v1.1.1"))
