#!/usr/bin/env python3
"""Write Continuum Android OAuth client ID into apps/mobile/local.properties (gitignored)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_LOCAL = ROOT / "apps" / "mobile" / "local.properties"
KEY = "continuum.google.android.client.id"


def main() -> int:
    if len(sys.argv) < 2 or not sys.argv[1].strip():
        print(
            "Usage: python scripts/set-android-google-client-id.py "
            "<ANDROID_CLIENT_ID>.apps.googleusercontent.com",
            file=sys.stderr,
        )
        return 2
    client_id = sys.argv[1].strip()
    if "apps.googleusercontent.com" not in client_id:
        print("Client ID should look like ….apps.googleusercontent.com", file=sys.stderr)
        return 2

    lines: list[str] = []
    if ANDROID_LOCAL.is_file():
        lines = ANDROID_LOCAL.read_text(encoding="utf-8").splitlines()
    out: list[str] = []
    found = False
    for line in lines:
        if line.strip().startswith(f"{KEY}="):
            out.append(f"{KEY}={client_id}")
            found = True
        else:
            out.append(line)
    if not found:
        if out and out[-1].strip():
            out.append("")
        out.append("# Continuum Android OAuth (Custom Tabs) — same GCP project as desktop")
        out.append(f"{KEY}={client_id}")
    ANDROID_LOCAL.parent.mkdir(parents=True, exist_ok=True)
    ANDROID_LOCAL.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
    print(f"Updated {ANDROID_LOCAL} ({KEY})")
    print("Rebuild/reinstall the APK, then Settings → Continuum → Sync with desktop.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
