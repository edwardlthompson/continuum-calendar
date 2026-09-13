#!/usr/bin/env python3
"""Copy the Tauri NSIS EXE to Continuum-Calendar-{version}_x64-setup.exe."""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

DESKTOP = Path(__file__).resolve().parents[1]
BUNDLE = DESKTOP / "src-tauri" / "target" / "release" / "bundle" / "nsis"
TAG_RE = re.compile(r"^v?[0-9]+\.[0-9]+\.[0-9]+(?:[.-][A-Za-z0-9.-]+)?$")


def version(argv: list[str] | None = None) -> str:
    args = argv if argv is not None else sys.argv[1:]
    extra = [a for a in args if a != "--upload" and a.strip()]
    if extra:
        return extra[0].strip()
    pkg = json.loads((DESKTOP / "package.json").read_text(encoding="utf-8"))
    return str(pkg["version"])


def stable_exe(ver: str) -> Path:
    if not BUNDLE.is_dir():
        raise FileNotFoundError(f"No NSIS bundle dir at {BUNDLE}")
    exes = sorted(BUNDLE.glob("*.exe"))
    if not exes:
        raise FileNotFoundError(f"No .exe in {BUNDLE}")
    dest = BUNDLE / f"Continuum-Calendar-{ver}_x64-setup.exe"
    src = next((p for p in exes if p.name == dest.name), exes[0])
    if src.resolve() != dest.resolve():
        shutil.copy2(src, dest)
    return dest


def upload(path: Path) -> None:
    tag = (os.environ.get("ATTACH_TO_TAG") or "").strip()
    if not tag:
        raise SystemExit("ATTACH_TO_TAG is empty")
    if not TAG_RE.fullmatch(tag):
        raise SystemExit(f"unsafe ATTACH_TO_TAG: {tag!r}")
    subprocess.check_call(["gh", "release", "upload", tag, str(path), "--clobber"])


def main() -> int:
    try:
        dest = stable_exe(version())
    except FileNotFoundError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    print(f"Stable NSIS name: {dest}")
    if "--upload" in sys.argv:
        upload(dest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
