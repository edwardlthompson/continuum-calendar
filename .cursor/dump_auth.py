import re
import subprocess
from pathlib import Path

r = subprocess.run(
    [
        "adb",
        "-s",
        "8bf09993",
        "shell",
        "run-as",
        "org.continuumcalendar.app.debug",
        "cat",
        "shared_prefs/continuum_google_auth.xml",
    ],
    capture_output=True,
    text=True,
    encoding="utf-8",
    errors="replace",
)
xml = r.stdout
Path(".cursor/auth-prefs.xml").write_text(xml, encoding="utf-8")
print("return", r.returncode)
print("keys:", re.findall(r'name="([^"]+)"', xml))
for m in re.finditer(r'<string name="([^"]+)">([^<]*)</string>', xml):
    k, v = m.group(1), m.group(2)
    print(f"string {k}: len={len(v)}")
for m in re.finditer(r'<(long|int|boolean) name="([^"]+)" value="([^"]+)"', xml):
    print(f"{m.group(1)} {m.group(2)}: {m.group(3)}")
