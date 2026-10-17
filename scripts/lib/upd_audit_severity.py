"""Filter `upd audit --format json` to HIGH/CRITICAL only (ship local-gate policy)."""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path


def high_critical(payload: object) -> list[dict]:
    if not isinstance(payload, dict):
        return []
    out: list[dict] = []
    for item in payload.get("vulnerabilities") or []:
        if not isinstance(item, dict):
            continue
        sev = str(item.get("severity") or "").strip().lower()
        if sev in ("high", "critical"):
            out.append(item)
    return out


def run_upd_audit_json(argv: list[str], timeout: int, cwd: Path | None, env: dict) -> int:
    resolved = shutil.which(argv[0], path=env.get("PATH"))
    cmd = [resolved, *argv[1:]] if resolved else argv
    try:
        proc = subprocess.run(
            cmd,
            cwd=cwd,
            timeout=timeout,
            check=False,
            env=env,
            capture_output=True,
            text=True,
        )
    except subprocess.TimeoutExpired:
        print(f"FAIL: timeout after {timeout}s: {' '.join(argv)}", file=sys.stderr)
        return 1
    except FileNotFoundError:
        print(f"WARN: command not found: {argv[0]}", file=sys.stderr)
        return 127
    raw = (proc.stdout or "").strip() or (proc.stderr or "").strip()
    if raw:
        print(raw, flush=True)
    if proc.returncode not in (0, 1, 6):
        print(f"FAIL audit: upd exited {proc.returncode}", file=sys.stderr)
        return 1
    try:
        payload = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        print("FAIL audit: upd did not emit JSON", file=sys.stderr)
        return 1
    bad = high_critical(payload)
    if bad:
        print(f"FAIL audit: {len(bad)} HIGH/CRITICAL finding(s)", file=sys.stderr)
        return 1
    print("OK audit: no HIGH/CRITICAL (Medium/Low/Unknown ignored for local gate)", flush=True)
    return 0
