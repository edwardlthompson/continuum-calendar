# Agent Memory

> Centralized index of tech stack, threat models, persistent context, and retrospectives.
> Update only at session startups, milestone boundaries, or major architectural pivots.

## Tech Stack

| Layer | Technology | Version | Notes |
|-------|-----------|---------|-------|
| Desktop | Tauri 2 + React + Vite + Tailwind + FullCalendar | 1.0.0 | `apps/desktop`; Linux install is `.deb` (`dpkg`) |
| Mobile | FossifyOrg/Calendar fork (Kotlin) | 1.0.0 / 26 | `apps/mobile` (`org.continuumcalendar.app`); release key `$HOME/keys/continuum-release.jks` |
| Shared | TypeScript types | 0.1.0 | `packages/shared` |
| Google | Calendar API + People API | REST | Scopes in `docs/GOOGLE_API_SETUP.md` |
| License | MIT | - | Pure FOSS |
| Bootstrap | agent-project-bootstrap | 0.21.0 | See `bootstrap-manifest.yaml` |
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
- Latest tag: `v1.1.2` (Release Please bump). Sprint 4–6 desktop/Android work is local/uncommitted and not on that tag.

### Key Constraints

- Shared schema/types live in `packages/shared` (Sequential lock before parallel UI work)
- Mobile git submodule requires `[HUMAN]` approval
- Max 300 lines per static data file, 150 lines per pure logic file (bootstrap rule)
- Trunk-based development with Conventional Commits
- No Google Calendar–style Day view; rolling week + agenda stay primary

## Session Retrospectives

| Date | Sprint/Task | What Worked | What to Improve |
|------|-------------|-------------|-----------------|
| 2026-09-14 | ADB widget glance | OP13 `install -r` FOSS release; today highlight + Open vs `1`; Settings rolling-week toggle on; no reboot/wipe; Launcher3 restored as HOME | `Open` wraps on 4×1; Hermes tried to become Home when using KEYCODE_HOME; Sprint 4–6 still uncommitted |
| 2026-09-14 | Sprint 6 audit leftovers | H1 without version; compact TZ; Ctrl+K palette; Open marks; FAQ locales; smoke-sprint PASS; RP #37 merged | `[ADB]` widget glance |
| 2026-09-14 | Sprint 4 UX feel | Grid drag persists; settings overlay + editor side sheet; widget Open/today; FAQ English → Connect Google; smoke-sprint PASS | `[ADB]` glance widget on a phone (no reboot/wipe); other-locale FAQ still mentions DAVx5 |
| 2026-09-13 | /push v1.1.0 | RP #35/#36 merged; installers on the tag; OP13 on signed 1.0.0; Android OAuth SHA-1 registered; keystore on Drive + GitHub | Rebuild Windows EXE via `desktop-nsis.yml` (AUTO row); Google SHA-1 can take hours to propagate |
| 2026-09-11 | Release keystore | New `$HOME/keys/continuum-release.jks`; GitHub env `release-signing`; local `assembleFossRelease` signed | Add new SHA-1 on Google Android OAuth client; uninstall old release APKs before sideload |
| 2026-09-11 | 1.0.0 stable | Left 0.x beta; GitHub tag v1.0.0; desktop source version 1.0.0; Android stays 1.10.7 | Rebuild Windows EXE / signed APK on those hosts so About matches 1.0.0 |
| 2026-09-11 | Wrap-up ship | BUILD_PLAN 0 open; Dependabot #24–#29 + release #33 merged; installers on v0.25.2; Tasks/widget/GP catch-up committed | Copy EXE/APK onto each future tag; leftover node hono 4.13.7 / vitest 4.1.11 if Dependabot reopens |
| 2026-09-11 | /cleanup + backlog | Archived OAuth/ADB; EXE+APK on v0.25.1; Tasks/widget/AppImage CI | `docs/PRIVACY.md` still needs a push to `main`; Dependabot PRs remain |
| 2026-09-11 | HUMAN OAuth/legal/token vault | Console + local bake; F-002 release Android client; F-003 vault; F-004 privacy; ROADMAP | ADB device smoke and Windows EXE/APK attach still need those hosts; `docs/PRIVACY.md` on `main` after push |
| 2026-08-22 | /ship v0.24.0 | `feat(desktop)` → RP #17 cut **v0.24.0**; editor/tray/autostart; Unreleased restored (KB-032) | Copy EXE/APK onto each tag; ADB smoke still open |
| 2026-08-21 | /ship v0.23.0 | `feat(about)` → RP #16 cut **v0.23.0**; Venmo + daily GitHub installer check; Unreleased restored (KB-032) | Copy EXE/APK onto each tag; donate nudge only after version change |
| 2026-08-20 | /ship v0.22.1 | RP #14 cut **v0.22.1** (chore prep → patch); I1 features in 300cd93; Unreleased restored after fold (KB-032/033) | Next minor needs `feat:` commits; copy product binaries onto each template tag |
| 2026-08-20 | /ship v0.22.0 | Template 0.21.0 + high-refresh Android; RP #12 cut **v0.22.0**; branch protection includes Windows upgrade-sim | HUMAN: OAuth test users; Pages 404; reinstall APK for high-refresh |
| 2026-08-20 | Template sync v0.21.0 | Canon commands/help/scripts + Windows upgrade-sim; adapters from AGENTS.md | Do not treat this as a product /ship; RP may skip a 0.21.0 tag (KB-030) |
| 2026-08-17 | /ship v0.19.0 | Template 0.18.3 parity + About Fossify CTAs removed; RP #11 cut **v0.19.0**; APK/EXE attached | HUMAN: OAuth test users; Pages 404 |
| 2026-08-16 | /ship v0.17.1 | RP #10 merged; SBOMs + 1.10.7 APK + 0.17.3 EXE on release; Calendar-only desktop OAuth + native token POST | HUMAN: add exact Gmail as OAuth test user; public consent / LICENSE / Privacy URL; Pages still 404 |
| 2026-08-15 | /ship title overflow | Feature-gate 19 stages; Settings long-title modes; Codex skipped (no key) | HUMAN: public OAuth + install 1.10.4+ on every device; Scorecard/CI wait after push |
| 2026-08-14 | Audit A2 | Release/PROD omit OAuth secret by default; v0.16.1 duplicate SBOMs removed; desktop version 0.16.2; gates green | HUMAN: OAuth/privacy/LICENSE; approve Actions on RP #9; install 1.10.4 on every device |
| 2026-08-11 | Audit A1 | Encoding skip `build/`; Android token refresh; desktop notify serialize; agenda local day keys; gates green after restoring example deps post-purge | Avoid `purge-ephemeral --apply` mid-audit without reinstalling examples/* deps; [HUMAN] public OAuth clients + secure token storage |
| 2026-08-10 | Project bootstrap | Template init + desktop scaffold + trackers | Wire live OAuth after GCP client exists |
## Template Provenance

- **Source template:** `edwardlthompson/agent-project-bootstrap`
- **Template version:** `1.2.0` (see `.template-version`; parent template catch-up is v1.4.0)
- **Last update check:** See `.template-update.json`
