import re
import subprocess
import time

serial = "8bf09993"
pkg = "org.continuumcalendar.app.debug"


def adb(*args: str) -> str:
    r = subprocess.run(
        ["adb", "-s", serial, *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return r.stdout


def pref(name: str) -> str:
    return adb("shell", "run-as", pkg, "cat", f"shared_prefs/{name}")


time.sleep(20)
auth = pref("continuum_google_auth.xml")
settings = pref("continuum_settings.xml")
local = pref("continuum_local_events.xml")

has_access = 'name="access"' in auth
has_refresh = 'name="refresh"' in auth
expires = re.search(r'name="expires" value="(\d+)"', auth)
rev = re.search(r'name="revision" value="(\d+)"', settings)
pending = re.search(r'name="pending_peer_push" value="([^"]+)"', settings)
local_rev = re.search(r'name="revision" value="(\d+)"', local)

print("access:", has_access)
print("refresh:", has_refresh)
print("expires:", expires.group(1) if expires else None)
print("settings_revision:", rev.group(1) if rev else None)
print("pending_peer_push:", pending.group(1) if pending else None)
print("local_events_revision:", local_rev.group(1) if local_rev else None)

log = adb("logcat", "-d", "-t", "250", "ContinuumDiag:V", "*:S")
refresh_fail = 0
json_229 = 0
useful = []
for ln in log.splitlines():
    if "at org." in ln or "at java." in ln:
        continue
    if "Token refresh" in ln and "failed" in ln:
        refresh_fail += 1
    if "Value 229" in ln:
        json_229 += 1
    if any(
        k in ln
        for k in (
            "Token",
            "OAuth",
            "Drive",
            "peer",
            "HTTPS",
            "JSONException",
            "reconcile",
            "push",
            "pull",
        )
    ):
        useful.append(ln[:220])

print("refresh_fail_lines:", refresh_fail)
print("json_229_lines:", json_229)
print("--- diag ---")
for ln in useful[-30:]:
    print(ln)

ok = has_access and has_refresh and (rev is not None) and json_229 == 0
print("VERDICT:", "PASS" if ok else "NEEDS_ATTENTION")
