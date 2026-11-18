#!/usr/bin/env bash
# Validate F-Droid/Fastlane metadata scaffold (AGENT gate; APK hashes remain ADB).
# Usage: scripts/verify-fdroid-metadata.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

META="$ROOT/examples/android/metadata/en-US"
GRADLE="$ROOT/examples/android/app/build.gradle.kts"
ERRORS=0

fail() {
  echo "FAIL: $1"
  ERRORS=$((ERRORS + 1))
}

ok() {
  echo "OK   $1"
}

if [ ! -d "$META" ]; then
  fail "missing $META"
else
  ok "metadata directory present"
fi

for f in title.txt short_description.txt full_description.txt; do
  if [ ! -s "$META/$f" ]; then
    fail "missing or empty $META/$f"
  else
    ok "$f present"
  fi
done

if [ ! -f "$GRADLE" ]; then
  fail "missing $GRADLE"
else
  VERSION_CODE="$(grep -E 'versionCode\s*=' "$GRADLE" | head -1 | sed -E 's/.*=\s*([0-9]+).*/\1/')"
  if [ -z "${VERSION_CODE:-}" ]; then
    fail "could not parse versionCode from build.gradle.kts"
  elif [ ! -s "$META/changelogs/${VERSION_CODE}.txt" ]; then
    fail "missing changelog $META/changelogs/${VERSION_CODE}.txt"
  else
    ok "changelog for versionCode ${VERSION_CODE}"
  fi
fi

if [ ! -d "$META/images" ]; then
  fail "missing $META/images/ (add README + assets before submit)"
else
  ok "images directory present"
fi

if [ -f "$ROOT/LICENSE" ]; then
  ok "root LICENSE present (MIT for template)"
else
  fail "missing root LICENSE"
fi

FASTLANE="$ROOT/examples/android/fastlane/metadata/android/en-US"
if [ ! -d "$FASTLANE" ]; then
  fail "missing $FASTLANE"
else
  ok "fastlane metadata mirror present"
  for f in title.txt short_description.txt full_description.txt; do
    if [ ! -s "$FASTLANE/$f" ]; then
      fail "missing or empty $FASTLANE/$f"
    else
      ok "fastlane $f present"
    fi
  done
  if [ ! -s "$ROOT/examples/android/fastlane/Fastfile" ]; then
    fail "missing examples/android/fastlane/Fastfile"
  else
    ok "Fastfile present"
  fi
  if [ ! -s "$ROOT/examples/android/fastlane/Appfile" ]; then
    fail "missing examples/android/fastlane/Appfile"
  else
    ok "Appfile present"
  fi
fi

RECIPE="$ROOT/examples/android/metadata/dev.foss.goldenpath.yml"
if [ ! -s "$RECIPE" ]; then
  fail "missing F-Droid build recipe $RECIPE"
elif ! grep -q "RepoType: git" "$RECIPE" || ! grep -q "subdir: examples/android" "$RECIPE"; then
  fail "F-Droid recipe must set RepoType git and subdir examples/android"
elif ! grep -q "gradle:" "$RECIPE"; then
  fail "F-Droid recipe must use a gradle build"
else
  ok "F-Droid build recipe present"
fi

AF="$ROOT/examples/android/metadata/antifeatures.yml"
if [ ! -s "$AF" ]; then
  fail "missing F-Droid AntiFeatures template $AF"
elif ! grep -q "AntiFeatures: \[\]" "$AF"; then
  fail "FOSS AntiFeatures template must default to an empty list"
else
  ok "F-Droid AntiFeatures template present"
fi

echo ""
echo "SKIP [ADB] reproducible APK hash verification — run on device/emulator per modules/android/MODULE.md"

PRODUCT_FL="$ROOT/apps/mobile/fastlane/metadata/android/en-US"
if [ -d "$ROOT/apps/mobile/app" ]; then
  if [ ! -d "$PRODUCT_FL" ]; then
    fail "missing product Fastlane $PRODUCT_FL"
  else
    ok "product Fastlane present (apps/mobile)"
    for f in title.txt short_description.txt full_description.txt; do
      if [ ! -s "$PRODUCT_FL/$f" ]; then
        fail "missing or empty $PRODUCT_FL/$f"
      else
        ok "product fastlane $f present"
      fi
    done
  fi
  MOBILE_PROPS="$ROOT/apps/mobile/gradle.properties"
  if [ -f "$MOBILE_PROPS" ]; then
    VC="$(grep -E '^VERSION_CODE=' "$MOBILE_PROPS" | head -1 | cut -d= -f2)"
    if [ -z "${VC:-}" ]; then
      fail "could not parse VERSION_CODE from apps/mobile/gradle.properties"
    elif [ ! -s "$PRODUCT_FL/changelogs/${VC}.txt" ]; then
      fail "missing product changelog $PRODUCT_FL/changelogs/${VC}.txt"
    else
      ok "product changelog for versionCode ${VC}"
    fi
  fi
  if [ ! -s "$ROOT/apps/mobile/LICENSE" ]; then
    fail "missing apps/mobile/LICENSE (GPL-3.0-only)"
  else
    ok "apps/mobile LICENSE present"
  fi
  if [ ! -s "$ROOT/LICENSING.md" ]; then
    fail "missing LICENSING.md mixed-license map"
  else
    ok "LICENSING.md present"
  fi
  GRADLE_APP="$ROOT/apps/mobile/app/build.gradle.kts"
  if ! grep -q 'maybeCreate("release")' "$GRADLE_APP"; then
    fail "apps/mobile signingConfigs must use maybeCreate(\"release\")"
  else
    ok "release signing uses maybeCreate"
  fi
  if ! grep -q 'findByName("release")' "$GRADLE_APP"; then
    fail "apps/mobile release buildType must use signingConfigs.findByName(\"release\")"
  else
    ok "release signing uses findByName"
  fi
  if ! grep -q 'jitpack.io' "$ROOT/apps/mobile/settings.gradle.kts"; then
    fail "apps/mobile needs JitPack for Fossify commons 6.1.6 (patched locally)"
  else
    ok "JitPack present for Fossify Commons"
  fi
  if ! grep -q 'register("foss") { disableGoogleOAuth() }' "$GRADLE_APP"; then
    fail "foss flavor must disable Google OAuth"
  else
    ok "foss flavor disables Google OAuth"
  fi
fi

# Dummy/placeholder screenshots fail even when metadata text is present.
if ! bash "$ROOT/scripts/check-fdroid-screenshots.sh"; then
  fail "dummy F-Droid screenshots are not allowed"
fi

if [ "$ERRORS" -gt 0 ]; then
  echo "${ERRORS} F-Droid metadata check(s) failed"
  exit 1
fi

echo "F-Droid metadata scaffold verified"
