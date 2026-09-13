#!/usr/bin/env python3
"""Verify Continuum release key files exist and GitHub already holds them."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

KEYS = Path.home() / "keys"
NEEDED = (
    "continuum-release.jks",
    "continuum-release.env",
    "continuum-release.sha.txt",
)
EXPECTED_SECRETS = {
    "ANDROID_KEYSTORE_BASE64",
    "SIGNING_KEY_ALIAS",
    "SIGNING_KEY_PASSWORD",
    "SIGNING_STORE_PASSWORD",
}


def main() -> int:
    missing = [name for name in NEEDED if not (KEYS / name).is_file()]
    if missing:
        print("FAIL: missing key files:", ", ".join(missing), file=sys.stderr)
        return 1
    for name in NEEDED:
        path = KEYS / name
        mode = path.stat().st_mode & 0o777
        if mode & 0o077:
            print(f"WARN: {path} mode {mode:o} is group/world readable", file=sys.stderr)
    env_name = os.environ.get("GITHUB_RELEASE_SIGNING_ENV", "release-signing")
    try:
        raw = subprocess.check_output(
            ["gh", "secret", "list", "--env", env_name],
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as exc:
        print(f"FAIL: gh secret list --env {env_name}: {exc}", file=sys.stderr)
        return 1
    present = {line.split("\t", 1)[0].strip() for line in raw.splitlines() if line.strip()}
    missing_secrets = sorted(EXPECTED_SECRETS - present)
    if missing_secrets:
        print("FAIL: GitHub env secrets missing:", ", ".join(missing_secrets), file=sys.stderr)
        return 1
    print(f"OK: local key files in {KEYS} and GitHub environment {env_name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
