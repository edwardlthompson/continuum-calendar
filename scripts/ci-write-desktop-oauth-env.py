#!/usr/bin/env python3
"""Write apps/desktop/.env from CI secrets for Desktop release packaging.

Fail-closed: both VITE_GOOGLE_CLIENT_ID and VITE_GOOGLE_CLIENT_SECRET must be
non-empty. Never prints the secret. Used by desktop-nsis / desktop-deb workflows.
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_PATH = ROOT / "apps" / "desktop" / ".env"
REDIRECT = "http://localhost:5173/oauth/callback"


def main() -> int:
    client_id = os.environ.get("VITE_GOOGLE_CLIENT_ID", "").strip()
    client_secret = os.environ.get("VITE_GOOGLE_CLIENT_SECRET", "").strip()

    missing: list[str] = []
    if not client_id:
        missing.append("VITE_GOOGLE_CLIENT_ID")
    if not client_secret:
        missing.append("VITE_GOOGLE_CLIENT_SECRET")
    if missing:
        print(
            "Missing required GitHub Actions secrets for Desktop OAuth bake: "
            + ", ".join(missing)
            + ". Add them under Settings → Secrets and variables → Actions, "
            "then re-run this workflow.",
            file=sys.stderr,
        )
        return 1

    if "apps.googleusercontent.com" not in client_id:
        print(
            "VITE_GOOGLE_CLIENT_ID should look like ….apps.googleusercontent.com",
            file=sys.stderr,
        )
        return 1

    lines = [
        "# Continuum Desktop Google OAuth (CI-generated — do not commit)",
        f"VITE_GOOGLE_CLIENT_ID={client_id}",
        f"VITE_GOOGLE_REDIRECT_URI={REDIRECT}",
        f"VITE_GOOGLE_CLIENT_SECRET={client_secret}",
        "",
    ]
    ENV_PATH.parent.mkdir(parents=True, exist_ok=True)
    ENV_PATH.write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"Wrote {ENV_PATH} (client id + secret baked for release; secret not logged)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
