# F-Droid / Fastlane images

Add store listing assets before F-Droid submission:

| File | Size (typical) |
|------|----------------|
| `icon.png` | 512×512 |
| `featureGraphic.png` | 1024×500 |
| `phoneScreenshots/*.png` | 16:9 or device frames |
Paths are referenced from `metadata/en-US/` (manual) or `fastlane/metadata/android/en-US/` (Fastlane).

This folder is exemplar scaffold only. Continuum Calendar store assets live under `apps/mobile/graphics/` and `apps/mobile/fastlane/metadata/`.

`[ADB]` capture screenshots on a physical device or emulator before release.
