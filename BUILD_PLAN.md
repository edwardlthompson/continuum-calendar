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
<!-- local-agent-lane:end -->

### Cloud agent (Cursor Cloud)

Standing queue for Cursor Cloud. Rows: `🔲 [AGENT][CLOUD] … — scope: path`.

<!-- cloud-agent-lane:begin -->
_No cloud agent items._
<!-- cloud-agent-lane:end -->

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
