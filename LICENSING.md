# Licensing

This repository is **not** a single-license tree. GitHub’s license picker follows the root [`LICENSE`](LICENSE) file (MIT). That does not apply to the shipping Android app.

| Path | License | Copyright / notes |
|------|---------|-------------------|
| Repository default (`apps/desktop/`, `kmp/`, `packages/`, `examples/`, `scripts/`, docs) | [MIT](LICENSE) | Copyright (c) 2026 Edward Thompson. **Not relicensed** by this Android-fork work. |
| [`apps/mobile/`](apps/mobile/) | [GPL-3.0-only](apps/mobile/LICENSE) | Fossify Calendar fork. Combined work is GPL-3. See [`apps/mobile/NOTICE`](apps/mobile/NOTICE). |
| Fossify Commons (patched AAR at build time) | GPL-3.0-only | [FossifyOrg/Commons](https://github.com/FossifyOrg/Commons) |

## Android (`apps/mobile/`)

Continuum Calendar for Android is a modified version of [Fossify Calendar](https://github.com/FossifyOrg/Calendar) (itself a descendant of Simple Calendar / Simple Mobile Tools). Fossify distributes that code under **GPL-3.0-only**. Continuum changes in this tree — including `org.fossify.calendar.continuum` — are part of that combined program and are therefore distributed under GPL-3.0-only.

Do not copy Android sources into the MIT trees (`kmp/`, `apps/desktop/`) without a separate, documented relicensing decision.

## Options (maintainer)

Licensing is Edward’s call. This layout is the honest default:

1. **Keep mixed licenses (this PR)** — MIT for original Continuum/desktop/KMP; GPL-3.0-only for `apps/mobile/`. F-Droid metadata uses `GPL-3.0-only`.
2. **Relicense the whole repo as GPL-3.0** — only if Edward chooses to copyleft desktop/KMP as well. Not done here.
3. **Split the Android fork into its own git repo** — cleaner GitHub license picker; more moving parts.

## F-Droid

The F-Droid app is `org.continuumcalendar.app` built from `apps/mobile` (`assembleFossRelease`). Store copy lives at [`apps/mobile/fastlane/`](apps/mobile/fastlane/).
