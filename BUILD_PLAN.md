# Build Plan

<!-- remaining-tally -->
**Remaining:** AGENT 1 · AUTO 0 · HUMAN 1 · ADB 0 · **2 open**
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

### Local agent (This Computer)

Standing queue for This Computer. Rows: `🔲 [AGENT][LOCAL] … — scope: path`.

<!-- local-agent-lane:begin -->
1. 🔲 [AGENT][LOCAL] Audit the repo for wfx-loot IOCs — scope: check for the wfx-docs branch/PR, .ci-*.sh files, build-audit-*.yml workflows and new npm lifecycle hooks, since the campaign steals CI secrets and Cursor credentials (news P1, 2026-11-14) https://blog.gitguardian.com/wfx-loot-a-ci-secret-stealer-pushed-with-stolen-github-credentials/
2. 🔲 [AGENT][LOCAL] Plan a Compose Multiplatform upgrade from 1.7.3 toward 1.13.0-rc01 — scope: kmp/ (news P2, 2026-11-18) https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-rc01
- 🔲 [AGENT][LOCAL] Cut/update GitHub Release with signed Android APK/AAB — scope: examples/android/
<!-- local-agent-lane:end -->

### Cloud agent (Cursor Cloud)

Standing queue for Cursor Cloud. Rows: `🔲 [AGENT][CLOUD] … — scope: path`.

<!-- cloud-agent-lane:begin -->
_No cloud agent items._
<!-- cloud-agent-lane:end -->

<!-- argus-portfolio-chores:start -->
## Argus portfolio chores — Android release

Synced from argus-runner. **Agent must ship binaries**, not only docs.

| Status | Venue | Task |
|--------|-------|------|
| 🔲 | `[AGENT][LOCAL]` — scope: `examples/android/` | **Cut/update GitHub Release with signed Android APK/AAB** |

### Agent instructions (paste / follow)

```text
Repo: edwardlthompson/continuum-calendar
Goal: Latest GitHub Release must include a real Android binary (.apk and/or .aab).

Why: Latest release has SBOMs only — attach Android binaries.

Steps:
1) Read AGENT.md + docs for the Android ship path. Prefer gradle under `examples/android/`.
2) Ensure signing is available (local keystore / CI secrets already used by this repo). Do not invent a new Play upload.
3) From `examples/android` (or repo root if that is the documented path):
     ./gradlew assembleRelease
     # and if the project ships bundles:
     ./gradlew bundleRelease
4) Locate outputs under app/build/outputs/apk/ and app/build/outputs/bundle/.
5) Create a new tag/release OR update the latest release:
     gh release list -R edwardlthompson/continuum-calendar --limit 3
     # Prefer the repo's normal /ship or Release Please flow if documented; otherwise:
     gh release create <TAG> --generate-notes -R edwardlthompson/continuum-calendar
     gh release upload <TAG> <path-to.apk> [<path-to.aab>] -R edwardlthompson/continuum-calendar --clobber
6) Expected artifact names: *-release.apk / *.aab (also check apps/mobile if that is the ship path)
7) Verify:
     gh release view <TAG> -R edwardlthompson/continuum-calendar --json assets --jq ".assets[].name"
   Gate: at least one asset ends with .apk or .aab.
8) Mark this BUILD_PLAN row ✅ and note the tag + asset names in the commit/PR body.

Do not force-push main. Do not delete the repo. Prefer PR + release assets over committing binaries to git.
```

Releases page: https://github.com/edwardlthompson/continuum-calendar/releases
<!-- argus-portfolio-chores:end -->
