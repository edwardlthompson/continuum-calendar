from pathlib import Path
import re
import subprocess

pkg = "org.continuumcalendar.app.debug"
serial = "8bf09993"


def pull(name: str) -> str:
    r = subprocess.run(
        ["adb", "-s", serial, "shell", "run-as", pkg, "cat", f"shared_prefs/{name}"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return r.stdout if r.returncode == 0 else r.stderr


auth = pull("continuum_google_auth.xml")
settings = pull("continuum_settings.xml")
local_events = pull("continuum_local_events.xml")

Path(".cursor/auth-prefs.xml").write_text(auth, encoding="utf-8")
Path(".cursor/settings-prefs.xml").write_text(settings, encoding="utf-8")

print("=== continuum_google_auth ===")
if "Permission denied" in auth or not auth.strip().startswith("<?xml"):
    print("UNREADABLE:", auth[:200])
else:
    # string values
    for m in re.finditer(r'<string name="([^"]+)">([^<]*)</string>', auth):
        k, v = m.group(1), m.group(2)
        sensitive = any(x in k.lower() for x in ("token", "access", "refresh", "verifier", "code"))
        if sensitive:
            print(f"  {k}: PRESENT len={len(v)}")
        else:
            print(f"  {k}: {v[:100]}")
    for m in re.finditer(r'<(?:long|int|boolean) name="([^"]+)" value="([^"]+)"', auth):
        print(f"  {m.group(1)}: {m.group(2)}")

print("=== continuum_settings ===")
if settings.strip().startswith("<?xml"):
    for m in re.finditer(r'name="([^"]+)"', settings):
        print("  key:", m.group(1))
    rev = re.search(r'name="revision" value="(\d+)"', settings)
    print("  revision:", rev.group(1) if rev else None)
    print("  settings_json present:", "settings_json" in settings)
else:
    print(settings[:200])

print("=== continuum_local_events ===")
if local_events.strip().startswith("<?xml"):
    for m in re.finditer(r'name="([^"]+)"[^/]*value="([^"]+)"', local_events):
        print(f"  {m.group(1)}: {m.group(2)}")
else:
    print("(missing or empty)", local_events[:120])

print("=== recent ContinuumDiag ===")
r = subprocess.run(
    ["adb", "-s", serial, "logcat", "-d", "-t", "300", "ContinuumDiag:V", "*:S"],
    capture_output=True,
    text=True,
    encoding="utf-8",
    errors="replace",
)
lines = [ln for ln in r.stdout.splitlines() if ln.strip()]
for ln in lines[-30:]:
    # redact bearer-like strings
    safe = re.sub(r"ya29\.[A-Za-z0-9._-]+", "ya29.[REDACTED]", ln)
    safe = re.sub(r"1//[A-Za-z0-9._-]+", "1//[REDACTED]", safe)
    print(safe)
