#!/usr/bin/env python3
"""
Patch Fossify commons so FakeVersionCheck accepts Continuum's applicationId
and About does not show hello@fossify.org.

Upstream checks packageName.startsWith("org.fossify."). Continuum uses
org.continuumcalendar.app — rewrite the 12-byte UTF-8 constant to "org.continuu"
(same length; prefix of org.continuumcalendar.*).

Installs a local Maven artifact:
  apps/mobile/libs/m2/org/fossify/commons/6.1.6-continuum/
Point settings.gradle.kts at libs/m2 and set commons version to 6.1.6-continuum.
"""
from __future__ import annotations

import io
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = "6.1.6-continuum"
UPSTREAM_VERSION = "6.1.6"
M2 = ROOT / "apps" / "mobile" / "libs" / "m2" / "org" / "fossify" / "commons" / VERSION
OUT_AAR = M2 / f"commons-{VERSION}.aar"
OUT_POM = M2 / f"commons-{VERSION}.pom"
OLD = b"org.fossify."
NEW = b"org.continuu"
# Both sites gate on packageName.startsWith("org.fossify.")
TARGET_CLASSES = (
    "org/fossify/commons/compose/extensions/ActivityExtensionsKt.class",
    "org/fossify/commons/activities/BaseSimpleActivity.class",
)
# Commons AboutScreen always shows R.string.my_email (hello@fossify.org).
MY_EMAIL_FOSSIFY = b'<string name="my_email">hello@fossify.org</string>'
MY_EMAIL_BLANK = b'<string name="my_email"></string>'


def find_upstream() -> tuple[Path, Path]:
    gradle = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1" / "org.fossify" / "commons"
    if not gradle.is_dir():
        raise SystemExit(f"Commons not in Gradle cache: {gradle} (build app once first)")
    aars = sorted(gradle.rglob(f"*{UPSTREAM_VERSION}*.aar"), key=lambda p: p.stat().st_mtime, reverse=True)
    poms = sorted(gradle.rglob(f"*{UPSTREAM_VERSION}*.pom"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not aars or not poms:
        raise SystemExit(f"No commons {UPSTREAM_VERSION} AAR/POM in Gradle cache")
    return aars[0], poms[0]


def patch_class(data: bytes) -> bytes:
    if NEW in data and OLD not in data:
        return data
    if OLD not in data:
        raise SystemExit(f"expected {OLD!r} in FakeVersionCheck class")
    return data.replace(OLD, NEW)


def patch_my_email(data: bytes) -> tuple[bytes, int]:
    count = data.count(MY_EMAIL_FOSSIFY)
    if count:
        data = data.replace(MY_EMAIL_FOSSIFY, MY_EMAIL_BLANK)
    return data, count


def main() -> int:
    src_aar, src_pom = find_upstream()
    print(f"Source AAR: {src_aar}")
    print(f"Source POM: {src_pom}")
    M2.mkdir(parents=True, exist_ok=True)

    buf = io.BytesIO()
    patched: set[str] = set()
    email_hits = 0
    with zipfile.ZipFile(src_aar, "r") as zin, zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            raw = zin.read(info.filename)
            if info.filename == "classes.jar":
                jbuf = io.BytesIO()
                with zipfile.ZipFile(io.BytesIO(raw), "r") as jin, zipfile.ZipFile(
                    jbuf, "w", zipfile.ZIP_DEFLATED
                ) as jout:
                    for jinfo in jin.infolist():
                        jraw = jin.read(jinfo.filename)
                        if jinfo.filename in TARGET_CLASSES:
                            jraw = patch_class(jraw)
                            patched.add(jinfo.filename)
                            print(f"Patched {jinfo.filename}: {OLD!r} -> {NEW!r}")
                        jout.writestr(jinfo, jraw)
                raw = jbuf.getvalue()
            elif info.filename.endswith(".xml"):
                raw, count = patch_my_email(raw)
                email_hits += count
            zout.writestr(info, raw)
    missing = set(TARGET_CLASSES) - patched
    if missing:
        raise SystemExit(f"Missing classes in commons AAR: {sorted(missing)}")
    if email_hits == 0:
        raise SystemExit("commons AAR had no my_email Fossify string to blank")
    print(f"Blanked my_email in {email_hits} XML string(s)")
    OUT_AAR.write_bytes(buf.getvalue())

    pom = src_pom.read_text(encoding="utf-8")
    pom = pom.replace(f"<version>{UPSTREAM_VERSION}</version>", f"<version>{VERSION}</version>", 1)
    # Keep dependency versions as upstream; only the commons artifact version changes.
    OUT_POM.write_text(pom, encoding="utf-8", newline="\n")
    print(f"Wrote {OUT_AAR} ({OUT_AAR.stat().st_size} bytes)")
    print(f"Wrote {OUT_POM}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
