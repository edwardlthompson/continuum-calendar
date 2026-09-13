# Build Plan

<!-- remaining-tally -->
**Remaining:** AGENT 0 · AUTO 0 · HUMAN 0 · ADB 0 · **0 open**
<!-- /remaining-tally -->

> Active board. Finished work: [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md) · Roadmap: [`ROADMAP.md`](ROADMAP.md) · Ship checklist: [`docs/HUMAN_REMAINING.md`](docs/HUMAN_REMAINING.md)

| Label | Owner |
|-------|--------|
| `[AGENT]` | Cursor Agent (code / docs / CI) |
| `[HUMAN]` | You (OAuth, legal, releases, product calls) |
| `[ADB]` | You + Android device/emulator |
| `[AUTO]` | GitHub Actions / Dependabot (Monday crons cover most of this) |
Status: 🔲 open · ✅ done (archive) · ❌ blocked

---

## HUMAN / ADB (do these)

Paste-ready steps: [`docs/HUMAN_REMAINING.md`](docs/HUMAN_REMAINING.md)

Ship leftovers from v1.1.1 are archived. Active board is empty.

## AUTO (GitHub — do not duplicate on this board)

Monday crons already cover health, security, Scorecard, CodeQL, stale, and template-upgrade simulation:

- `.github/workflows/weekly-health-check.yml` (`0 7 * * 1`)
- `.github/workflows/security.yml` / `codeql.yml` / `scorecard.yml` / `stale.yml`

Dependabot + auto-merge: `.github/dependabot.yml`, `dependabot-automerge.yml`.
Known deferred: medium `glib` in desktop `Cargo.lock` (do not bump to GTK4).

---

## Ongoing Maintenance

Not a checklist. GitHub Monday cron already runs CI wait, security triage, template-gap sync, radar, `update-deps` dry-run, Dependabot leftover list, open-PR BUILD_PLAN sync, and latest-release SBOM.

---

## Archive

Finished work: [`COMPLETED_TASKS.md`](COMPLETED_TASKS.md).

### Open PRs (synced)

> Auto-managed. Do not hand-edit rows inside the markers.

<!-- open-prs-sync:begin -->
_No open Dependabot or Release Please PRs._
<!-- open-prs-sync:end -->

### Template gaps (synced)

> Auto-managed Monday cron + `sync-template-gaps-build-plan`. Do not hand-edit inside markers.

<!-- template-gaps-sync:begin -->
_No template gaps; .template-version matches upstream (or template maintainer N/A)._
<!-- template-gaps-sync:end -->
