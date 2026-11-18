# Fastlane / F-Droid metadata

F-Droid scrapes Fastlane from the **git root** or from the Gradle module when `subdir:` is set.

**Product Android app** (`org.continuumcalendar.app`):

[`apps/mobile/fastlane/metadata/android/`](../apps/mobile/fastlane/metadata/android/)

Do **not** copy [`examples/android/fastlane/`](../examples/android/fastlane/) here. That tree is the Golden Path stub (`dev.foss.goldenpath`), not Continuum Calendar.

The proposed fdroiddata recipe uses `subdir: apps/mobile/app` so F-Droid also finds `apps/mobile/fastlane`.
