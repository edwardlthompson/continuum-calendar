#!/usr/bin/env python3
"""Write Continuum Android OAuth client ID into apps/mobile/local.properties (gitignored)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_LOCAL = ROOT / "apps" / "mobile" / "local.properties"
DEBUG_KEY = "continuum.google.android.client.id"
RELEASE_KEY = "continuum.google.android.release.client.id"


def main() -> int:
    args = [a for a in sys.argv[1:] if a.strip()]
    release = False
    if args and args[0] in ("--release", "-r"):
        release = True
        args = args[1:]
    if not args:
        print(
            "Usage: python scripts/set-android-google-client-id.py "
            "[--release] <ANDROID_CLIENT_ID>.apps.googleusercontent.com",
            file=sys.stderr,
        )
        return 2
    client_id = args[0].strip()
    key = RELEASE_KEY if release else DEBUG_KEY
    if "apps.googleusercontent.com" not in client_id:
        print("Client ID should look like ….apps.googleusercontent.com", file=sys.stderr)
        return 2

    lines: list[str] = []
    if ANDROID_LOCAL.is_file():
        lines = ANDROID_LOCAL.read_text(encoding="utf-8").splitlines()
    out: list[str] = []
    found = False
    for line in lines:
        if line.strip().startswith(f"{key}="):
            out.append(f"{key}={client_id}")
            found = True
        else:
            out.append(line)
    if not found:
        if out and out[-1].strip():
            out.append("")
        comment = (
            "# Continuum Android OAuth release client — same GCP project as desktop"
            if release
            else "# Continuum Android OAuth (Custom Tabs) — same GCP project as desktop"
        )
        out.append(comment)
        out.append(f"{key}={client_id}")
    ANDROID_LOCAL.parent.mkdir(parents=True, exist_ok=True)
    ANDROID_LOCAL.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
    print(f"Updated {ANDROID_LOCAL} ({key})")
    print("Rebuild/reinstall the APK, then Settings → Continuum → Sync with desktop.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
