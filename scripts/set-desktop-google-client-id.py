#!/usr/bin/env python3
"""Write Continuum OAuth client ID for desktop + Android peer settings sync."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_PATH = ROOT / "apps" / "desktop" / ".env"
ANDROID_LOCAL = ROOT / "apps" / "mobile" / "local.properties"
REDIRECT = "http://localhost:5173/oauth/callback"


def upsert_local_properties(client_id: str, client_secret: str = "") -> None:
    """Keep Android Continuum Client ID/secret in local.properties (gitignored)."""
    updates = {"continuum.google.client.id": client_id}
    if client_secret:
        updates["continuum.google.client.secret"] = client_secret
    lines: list[str] = []
    if ANDROID_LOCAL.is_file():
        lines = ANDROID_LOCAL.read_text(encoding="utf-8").splitlines()
    out: list[str] = []
    found: set[str] = set()
    for line in lines:
        matched = False
        for key, value in updates.items():
            if line.strip().startswith(f"{key}="):
                out.append(f"{key}={value}")
                found.add(key)
                matched = True
                break
        if not matched:
            out.append(line)
    for key, value in updates.items():
        if key in found:
            continue
        if out and out[-1].strip():
            out.append("")
        if key.endswith(".id"):
            out.append("# Continuum peer settings sync (same GCP project as desktop)")
        out.append(f"{key}={value}")
    ANDROID_LOCAL.parent.mkdir(parents=True, exist_ok=True)
    ANDROID_LOCAL.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
    print(f"Updated {ANDROID_LOCAL} ({', '.join(updates)})")


def main() -> int:
    if len(sys.argv) < 2 or not sys.argv[1].strip():
        print(
            "Usage: python scripts/set-desktop-google-client-id.py "
            "<CLIENT_ID>.apps.googleusercontent.com [CLIENT_SECRET]",
            file=sys.stderr,
        )
        return 2
    client_id = sys.argv[1].strip()
    client_secret = sys.argv[2].strip() if len(sys.argv) > 2 else ""
    if "apps.googleusercontent.com" not in client_id:
        print("Client ID should look like ….apps.googleusercontent.com", file=sys.stderr)
        return 2

    # Preserve an existing secret if only the ID is being rewritten.
    if not client_secret and ENV_PATH.is_file():
        for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
            if line.startswith("VITE_GOOGLE_CLIENT_SECRET="):
                client_secret = line.split("=", 1)[1].strip()
                break

    lines = [
        "# Continuum Desktop Google OAuth (gitignored — do not commit)",
        f"VITE_GOOGLE_CLIENT_ID={client_id}",
        f"VITE_GOOGLE_REDIRECT_URI={REDIRECT}",
    ]
    if client_secret:
        lines.append(f"VITE_GOOGLE_CLIENT_SECRET={client_secret}")
    lines.append("")
    ENV_PATH.parent.mkdir(parents=True, exist_ok=True)
    ENV_PATH.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"Wrote {ENV_PATH}")

    upsert_local_properties(client_id, client_secret)

    if client_secret:
        print("Client secret included (needed for token exchange with many Google clients).")
    else:
        print(
            "No client secret set. If Sign-in fails with "
            "'client_secret is missing', re-run with the secret as the 2nd argument.",
        )
    print(
        "Peer sync: desktop and Android use the same Client ID / GCP project for Drive App Data.\n"
        "Restart tauri:dev and rebuild the Android APK so both pick up the ID.\n"
        "On Android, complete Continuum Google API sign-in (Connect) once so settings join the peer remote.",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
