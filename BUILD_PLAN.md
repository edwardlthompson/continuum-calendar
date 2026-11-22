# Build Plan

<!-- remaining-tally -->
**Remaining:** AGENT 5 · AUTO 0 · HUMAN 1 · ADB 0 · **6 open**
<!-- /remaining-tally -->

Finished: [COMPLETED_TASKS.md](COMPLETED_TASKS.md) · Ship: [docs/HUMAN_REMAINING.md](docs/HUMAN_REMAINING.md) · Backlog: [HUMAN_BACKLOG.md](HUMAN_BACKLOG.md)

[AGENT] code · [HUMAN] you · [ADB] device · [AUTO] CI · 🔲 open · ✅ archive · ❌ blocked · 🔶 partial (gap) · ⏸ deferred (gap)

## Do now

### Sprint KMP leftovers

<!-- parallel_exception: human-only iOS signing leftover; no AGENT parallel rows -->

1. 🔲 [HUMAN] macOS: iOS framework link / signing when ready to ship CMP iOS

### Parallel

| ID | Owner | Scope | Status |
|----|-------|-------|--------|
| — | — | none | — |
## AUTO

Monday crons + Dependabot. Deferred: glib in desktop Cargo.lock (stay off GTK4).

<!-- argus-android-release-sprint:begin -->
### Sprint 55 — Ship Android GitHub Release

Numbered rows so child `/build` (parse_board_queue) picks them up. **Not** under Archive lanes.

1. 🔲 [AGENT] Cut/update GitHub Release with signed Android APK/AAB — scope: examples/android/

#### Agent instructions (release row)

```text
Repo: edwardlthompson/continuum-calendar
Goal: Latest GitHub Release must include a real Android binary (.apk and/or .aab).

Why: Latest release has SBOMs only — attach Android binaries (check apps/mobile if that is the ship path).

Steps:
1) Read AGENT.md + Android docs. Prefer gradle under `examples/android/`.
2) Use existing signing (local keystore / repo CI secrets). Do not invent Play Store upload.
3) From `examples/android` (or documented root):
     ./gradlew assembleRelease
     ./gradlew bundleRelease
4) Outputs: app/build/outputs/apk/ and app/build/outputs/bundle/.
5) Prefer this repo's /ship or Release Please flow if documented; else:
     gh release list -R edwardlthompson/continuum-calendar --limit 3
     gh release create <TAG> --generate-notes -R edwardlthompson/continuum-calendar
     gh release upload <TAG> <apk> [<aab>] -R edwardlthompson/continuum-calendar --clobber
6) Expected names: *-release.apk / *.aab
7) Gate:
     gh release view <TAG> -R edwardlthompson/continuum-calendar --json assets --jq ".assets[].name"
   At least one asset must end with .apk or .aab.
8) Mark the BUILD_PLAN release row ✅; note tag + asset names.

No force-push to main. No committing binaries into git.
```

Releases: https://github.com/edwardlthompson/continuum-calendar/releases
<!-- argus-android-release-sprint:end -->

### Local agent (This Computer)

Standing queue for This Computer. Rows: `🔲 [AGENT][LOCAL] … — scope: path`.

<!-- local-agent-lane:begin -->
1. 🔲 [AGENT] Audit the repo for wfx-loot IOCs — scope: check for the wfx-docs branch/PR, .ci-*.sh files, build-audit-*.yml workflows and new npm lifecycle hooks, since the campaign steals CI secrets and Cursor credentials (news P1, 2026-11-14) https://blog.gitguardian.com/wfx-loot-a-ci-secret-stealer-pushed-with-stolen-github-credentials/
2. 🔲 [AGENT] Plan a Compose Multiplatform upgrade from 1.7.3 toward 1.13.0-rc01 — scope: kmp/ (news P2, 2026-11-18) https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-rc01
3. 🔲 [AGENT] Bump the Gradle wrapper 9.6.1 (apps/mobile), 9.7.1 (kmp), 9.9.0 (examples/android) → 9.9.2 and commit the regenerated wrapper jar and distributionSha256Sum (4 High security fixes: SFTP host keys, snapshot dependency verification, TestKit temp dir, daemon registry, also gets the 9.9.1 Critical version-catalog fix) — scope: apps/mobile/gradle/wrapper/,kmp/gradle/wrapper/,examples/android/gradle/wrapper/ (news P2, 2026-11-20) https://docs.gradle.org/9.9.2/release-notes.html
4. 🔲 [AGENT] Bump tauri 2.11.5 → 2.11.6+ (latest 2.12.7) and smoke the desktop app; High IPC channel-fetch ACL bypass that Dependabot can't see (GHSA-w28w-mhc8-qvjv); also add a cargo entry for apps/desktop/src-tauri to .github/dependabot.yml — scope: apps/desktop/src-tauri/,.github/dependabot.yml (news P2, 2026-11-20) https://github.com/tauri-apps/tauri/security/advisories/GHSA-w28w-mhc8-qvjv
<!-- local-agent-lane:end -->

### Cloud agent (Cursor Cloud)

Standing queue for Cursor Cloud. Rows: `🔲 [AGENT][CLOUD] … — scope: path`.

<!-- cloud-agent-lane:begin -->
_No cloud agent items._
<!-- cloud-agent-lane:end -->


## Archive

[COMPLETED_TASKS.md](COMPLETED_TASKS.md)

### Open PRs (synced)

> Auto-managed. Do not hand-edit rows inside the markers.

<!-- open-prs-sync:begin -->
_No open Dependabot or Release Please PRs._
<!-- open-prs-sync:end -->

### Template gaps (synced)

> Auto-managed Monday cron + sync-template-gaps-build-plan. Do not hand-edit inside markers.

<!-- template-gaps-sync:begin -->
_No template gaps; .template-version matches upstream (or template maintainer N/A)._
<!-- template-gaps-sync:end -->


<!-- argus-portfolio-chores:start -->
## Argus portfolio chores

Synced from argus-runner (2026-11-18). Prefer YOLO maintain on the runner; keep these until cleared.

| Status | Venue | Task |
|--------|-------|------|
| 🔲 | `[AUTO][CLOUD]` | Clear open Dependabot alerts |
| 🔲 | `[HUMAN][PC]` | Publish Android release asset (APK or AAB) |

```text
Repo: edwardlthompson/continuum-calendar
1) Squash-merge green Dependabot PRs (incl. android-dependencies when green).
2) Attach signed APK/AAB to GitHub Release.
Gate: alerts 0; .apk/.aab on release.
```
<!-- argus-portfolio-chores:end -->
