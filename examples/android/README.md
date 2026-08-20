# Golden Path Android (exemplar)

FOSS-only Gradle/Kotlin Compose skeleton. No Google Play Services or Firebase.

**This is not the Continuum Calendar Android app.** The product lives in [`apps/mobile/`](../../apps/mobile/) (Fossify Calendar fork). Module contract: [`modules/android/MODULE.md`](../../modules/android/MODULE.md).

## Repository layout

```text
examples/android/
  app/src/main/
    res/values/strings.xml       # user-visible strings (English default)
    res/values-{lang}/           # add when shipping translations
    java/.../ui/
      theme/                     # GoldenPathTheme, generated Color.kt / Type.kt / Dimens.kt
      components/                # ThemeToggle, etc. — labels via stringResource()
      screens/                   # GoldenPathScreen, etc.

```

**Styles and strings are separate:** theme colors and spacing live in `ui/theme/` (from `design-tokens/`). All copy lives in `strings.xml`, consumed via `stringResource(R.string.*)` in Compose — never `Text("literal")`.

See [`docs/DESIGN_GUIDE.md`](../../docs/DESIGN_GUIDE.md) for tokens and i18n. Product strings and themes live in `apps/mobile/`.

## Structure validation (CI)

CI validates Gradle file structure and FOSS compliance markers only. Full APK builds require local Android SDK.

## Local build (ADB / HUMAN tasks)

```bash
export SOURCE_DATE_EPOCH=1700000000
cd examples/android
./gradlew assembleDebug

```

Product builds: `cd apps/mobile && ./gradlew assembleDebug`.

## Emulator checklist

Before running instrumented tests or manual QA:

- 🔲 Android SDK Platform 34+ installed (`sdkmanager "platforms;android-34"`)
- 🔲 Build-tools 34.x installed
- 🔲 System image with Google APIs **not** required (use AOSP image for FOSS parity)
- 🔲 `adb devices` lists emulator or hardware as `device`
- 🔲 Set `SOURCE_DATE_EPOCH` for reproducible release builds (exemplar default: `1700000000`)
- 🔲 Accept licenses: `sdkmanager --licenses`

## FOSS compliance

- No `com.google.android.gms` dependencies
- No Firebase dependencies
- `SOURCE_DATE_EPOCH` for reproducible builds
- Pinned Gradle wrapper SHA-256 in `gradle/wrapper/gradle-wrapper.properties`

## F-Droid notes

This tree’s `metadata/` and `fastlane/` files are **exemplar scaffold** for `scripts/verify-fdroid-metadata.sh`. Continuum Calendar store copy is `apps/mobile/fastlane/metadata/`. Record dependency hashes and reproducible-build notes in `AGENT_MEMORY.md` when activating Module A.
