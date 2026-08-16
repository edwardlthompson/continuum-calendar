# Agent Memory

> Centralized index of tech stack, threat models, persistent context, and retrospectives.
> Update only at session startups, milestone boundaries, or major architectural pivots.

## Tech Stack

| Layer | Technology | Version | Notes |
|-------|-----------|---------|-------|
| Desktop | Tauri 2 + React + Vite + Tailwind + FullCalendar | 0.1.0 | `apps/desktop` |
| Mobile | FossifyOrg/Calendar fork (Kotlin) | active | `apps/mobile` (`org.continuumcalendar.app`) |
| Shared | TypeScript types | 0.1.0 | `packages/shared` |
| Google | Calendar API + People API | REST | Scopes in `docs/GOOGLE_API_SETUP.md` |
| License | MIT | - | Pure FOSS |
| Bootstrap | agent-project-bootstrap | 0.16.1 | See `bootstrap-manifest.yaml` |

## Active Modules

- ✅ Web / desktop UI (`modules/web/MODULE.md` patterns; app in `apps/desktop`)
- ✅ Android / F-Droid (`modules/android/MODULE.md`; fork docs in `apps/mobile`)
- ⬜ Python / Node Golden Path examples retained from template (not primary product paths)

## Threat Model Checklist

- ✅ `docs/THREAT_MODEL.md` present from template (refine for OAuth/calendar data)
- ✅ No proprietary closed-source SDKs required for core FOSS path
- ✅ Secrets excluded from VCS; `.env.example` documents Google client id only
- ✅ Dependabot alerts + security updates enabled on `continuum-calendar` (2026-08-14 `/ship`)

## Persistent Context

### Project Purpose

Continuum Calendar — FOSS calendar with rolling week (Today = column 1), explicit empty days, Google Calendar/Contacts sync, themes, and Android homescreen widget.

### Public home

- GitHub: https://github.com/edwardlthompson/continuum-calendar
- Latest template-aligned tag: `v0.17.0` (SBOMs on GitHub Release; upload Windows EXE + signed FOSS APK separately)

### Key Constraints

- Shared schema/types live in `packages/shared` (Sequential lock before parallel UI work)
- Mobile git submodule requires `[HUMAN]` approval
- Max 300 lines per static data file, 150 lines per pure logic file (bootstrap rule)
- Trunk-based development with Conventional Commits

## Session Retrospectives

| Date | Milestone | What worked | What to improve |
|------|-----------|-------------|-----------------|
| 2026-08-15 | /ship title overflow | Feature-gate 19 stages; Settings long-title modes; Codex skipped (no key) | HUMAN: public OAuth + install 1.10.4+ on every device; Scorecard/CI wait after push |
| 2026-08-14 | Audit A2 | Release/PROD omit OAuth secret by default; v0.16.1 duplicate SBOMs removed; desktop version 0.16.2; gates green | HUMAN: OAuth/privacy/LICENSE; approve Actions on RP #9; install 1.10.4 on every device |
| 2026-08-11 | Audit A1 | Encoding skip `build/`; Android token refresh; desktop notify serialize; agenda local day keys; gates green after restoring example deps post-purge | Avoid `purge-ephemeral --apply` mid-audit without reinstalling examples/* deps; [HUMAN] public OAuth clients + secure token storage |
| 2026-08-10 | Project bootstrap | Template init + desktop scaffold + trackers | Wire live OAuth after GCP client exists |

## Template Provenance

- **Source template:** `edwardlthompson/agent-project-bootstrap`
- **Template version:** `0.17.1` (see `.template-version`)
- **Last update check:** See `.template-update.json`
