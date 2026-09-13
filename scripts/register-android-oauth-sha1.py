#!/usr/bin/env python3
"""Print the Google Console URL to add the Continuum release SHA-1.

Cloud Console OAuth Android clients are not writable without a user
Google login (clientauthconfig). This script never prints client secrets.
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / "apps" / "mobile" / "local.properties"
RELEASE_KEY = "continuum.google.android.release.client.id"
SHA1 = "4A:93:68:05:E1:8F:43:28:E0:31:33:B2:CD:8B:E4:AF:71:70:E2:82"
PACKAGE = "org.continuumcalendar.app"
CREDENTIALS = "https://console.cloud.google.com/apis/credentials"


def release_client_id() -> str:
    if not LOCAL.is_file():
        raise FileNotFoundError(str(LOCAL))
    for line in LOCAL.read_text(encoding="utf-8").splitlines():
        if line.startswith(f"{RELEASE_KEY}="):
            return line.split("=", 1)[1].strip()
    raise KeyError(RELEASE_KEY)


def client_edit_url(client_id: str) -> str:
    return f"{CREDENTIALS}/oauthclient/{client_id}"


def main() -> int:
    try:
        client_id = release_client_id()
    except (FileNotFoundError, KeyError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print(f"package={PACKAGE}")
    print(f"sha1={SHA1}")
    print(f"credentials={CREDENTIALS}")
    print(f"oauthclient={client_edit_url(client_id)}")
    if os.environ.get("PRINT_CLIENT_ID", "").strip() in {"1", "true", "yes"}:
        print(f"client_id={client_id}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
