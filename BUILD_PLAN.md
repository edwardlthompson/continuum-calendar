# Build Plan

> Prioritized task board with owner labels. **Completed sprints:** `COMPLETED_TASKS.md`.
> Product tracker: `IMPLEMENTATION_TRACKER.md` · Roadmap: `ROADMAP.md` · Manifest: `bootstrap-manifest.yaml`

## Owner Label Legend

| Label   | Owner           | When to use                                                |
| ------- | --------------- | ---------------------------------------------------------- |
| `AGENT` | Cursor Agent    | Code, docs, scaffolding, tests, CI config                  |
| `HUMAN` | Human developer | Approvals, credentials, GitHub settings, product decisions |
| `ADB`   | Human (Android) | Android SDK, emulator/device testing, F-Droid submission   |
| `AUTO`  | CI/scripts/bots | GitHub Actions, Dependabot, pre-commit, update checker     |

## Status markers

| Marker | State   | Agent action                                                          |
| ------ | ------- | --------------------------------------------------------------------- |
| 🔲     | Open    | Default for new tasks; work or leave queued                           |
| ✅      | Done    | Replace 🔲 when complete; archive sprint rows to `COMPLETED_TASKS.md` |
| ❌      | Blocked | Replace 🔲 when blocked; add brief reason after the description       |

**Task format:** `🔲 [OWNER] Description` · done: `✅ [OWNER] Description` · blocked: `❌ [OWNER] Description — reason`

```bash
grep '\[AGENT\]' BUILD_PLAN.md
grep '\[HUMAN\]' BUILD_PLAN.md
grep '\[ADB\]' BUILD_PLAN.md
grep '\[AUTO\]' BUILD_PLAN.md
```

**Agent rule:** Execute all `[AGENT]` **Sequential** items first, then dispatch **Parallel** agents with isolated file scopes. Shared schema/types are Sequential-only.

---

## Continuum Calendar — Active Board

### Sprint A2 — Audit 2026-08-14 (HUMAN backlog)

> **A2 AGENT** archived in COMPLETED_TASKS.md @ `fdcd285`. See ephemeral `CODE_REVIEW.md`.

#### Human & device (after automation)

- 🔲 [HUMAN] F-002: Public OAuth clients + consent + release keystore SHA-1 on Android client
- 🔲 [HUMAN] F-003: Approve or defer secure token storage
- 🔲 [HUMAN] F-004: LICENSE copyright + Continuum Privacy Policy URL
- 🔲 [HUMAN] F-007: Approve CodeQL/Actions on Release Please PR #9
- 🔲 [ADB] Install 1.10.4 APK; About shows 1.10.4; all-day + timed event has no false conflict

### Sprint A1 — Audit 2026-08-11 (HUMAN backlog)

> **A1 AGENT** work archived in COMPLETED_TASKS.md. See ephemeral `CODE_REVIEW.md`.  
> **Paste-ready HUMAN steps (GitHub-only public ship):** [`docs/HUMAN_REMAINING.md`](docs/HUMAN_REMAINING.md)

#### Human & device (after automation)

- 🔲 [HUMAN] F-002: Confirm desktop/Android Google OAuth clients are **public** (PKCE, no client secret in apps) — Cloud Console
- 🔲 [HUMAN] F-003: Approve secure token storage approach (Tauri store / EncryptedSharedPreferences)
- 🔲 [HUMAN] F-009: Triage Dependabot medium `glib` (postcss/hono cleared; see A2 F-005)
- 🔲 [ADB] Smoke agenda now-bar + Continuum notifications toggle after A1 APK install

### Sprint B1 — Brand & public identity

> Spec: `docs/BRAND.md` · tokens: `apps/desktop/src/styles/brand.css` · mark: `apps/desktop/public/continuum-mark.png` (Android ∞ launcher)

#### Sequential

1. ✅ [AGENT] Continuum brand stylesheet + SVG mark + Android brand color resources + BRAND.md (public/debrand checklist)
2. ✅ [HUMAN] Approve package id `org.continuumcalendar.app` (listing name: Continuum Calendar)
3. ✅ [AGENT] Rasterize Continuum mark into mobile mipmaps + `apps/desktop/src-tauri/icons/*`
4. ✅ [AGENT] Rewrite Fastlane en-US + mobile README; set `APP_ID=org.continuumcalendar.app` (`SOURCE_NAMESPACE` keeps fork Kotlin path)
5. ✅ [HUMAN] New Android OAuth client for `org.continuumcalendar.app` / `.debug` + SHA-1 in same GCP project (through HUMAN_REMAINING §1.3; sideload ready)

#### Human & device (after automation)

- 🔲 [HUMAN] Privacy Policy + LICENSE copyright for Continuum
- ✅ [AGENT] Commons About / thank-you debrand (hide Fossify suite CTAs)
- 🔲 [ADB] Visual smoke: launcher icon, package id, About screen, no Fossify store wording

### Sprint CC0 — Bootstrap & Desktop Prototype

<!-- agent_count_target: 3 | sequential_lock_step: 2 -->

#### Sequential (must complete in order)

1. ✅ [AGENT] Initialize from agent-project-bootstrap; set project metadata Continuum Calendar
2. ✅ [AGENT] Lock shared types in `packages/shared` (events, OAuth, contacts, theme)
3. 🔲 [HUMAN] Optional for desktop API: Continuum product ships one public OAuth Client ID (not per-user; end users only click Sign in). Android syncs Google via the account already on the phone — no Cloud Console step for you. See `docs/GOOGLE_API_SETUP.md`
4. ✅ [HUMAN] Fossify Calendar fork in `apps/mobile` (Continuum branding + CalDAV Google path)
5. ✅ [AGENT] Google PKCE + token storage wired; Android primary path = Connect Google calendars (device CalDAV)

#### Parallel (safe after Sequential step 2)

| Task | Owner | Isolated scope |
|------|-------|----------------|
| Desktop rolling week + theme + free slots (prototype) | AGENT | `apps/desktop/**` |
| Google API setup + architecture docs | AGENT | `docs/GOOGLE_API_SETUP.md`, `docs/architecture/**`, `docs/adr/**` |
| Mobile fork + widget specs | AGENT | `docs/MOBILE_FOSSIFY_FORK.md`, `docs/ANDROID_WIDGET_SPEC.md`, `apps/mobile/**` |

#### Human & device (after automation)

- 🔲 [HUMAN] Enable Dependabot alerts + private vulnerability reporting
- 🔲 [HUMAN] `scripts/setup-github-repo.ps1` branch protection
- 🔲 [ADB] Emulator QA after mobile fork lands

### Sprint CC1 — Google Sync MVP (blocked on OAuth credentials)

<!-- agent_count_target: 2 | sequential_lock_step: 1 -->

#### Sequential

1. ❌ [AGENT] Exchange auth code + refresh tokens — blocked on HUMAN OAuth client
2. 🔲 [AGENT] Persist tokens via Tauri secure store

#### Parallel (safe after Sequential step 2)

| Task | Owner | Isolated scope |
|------|-------|----------------|
| Calendar CRUD UI + sync | AGENT | `apps/desktop/src/services/**`, `apps/desktop/src/components/**` |
| Contacts autocomplete UI | AGENT | `apps/desktop/src/components/attendees/**` |

### Sprint CC2 — Scheduling parity (desktop ↔ Android)

<!-- agent_count_target: 2 | sequential_lock_step: 1 -->

#### Sequential

1. ✅ [AGENT] Kotlin ProposeTimes + FreeSlots format + agendaRangeDays=30
2. ✅ [AGENT] Android MainActivity Continuum scheduling actions + Settings knobs
3. ✅ [AGENT] ContinuumSettingsSync apply breadth + SettingsPoller wiring
4. ✅ [AGENT] Desktop rolling-week setting + click-to-edit + working hours/slot settings
5. ✅ [AGENT] PARITY_MATRIX / tracker / CONTINUUM.md update; rebuild APK

#### Deferred (not this sprint)

- 🔲 [AGENT] Google Tasks UI
- 🔲 [AGENT] Multi-Google-calendar API sync
- 🔲 [AGENT] Rolling-week homescreen widget
- ✅ [HUMAN] Package id `org.continuumcalendar.app` (see Sprint B1; OAuth client still open)

---

## Child Repo Playbook (template)

### Sprint 0 — Template Customization

#### Sequential

1. ✅ [AGENT] Run `scripts/init-project.ps1` (`-Stack multi`, Continuum Calendar)
2. 🔲 [AGENT] Run `scripts/setup-github-repo.ps1` (requires `gh` auth with admin)
3. 🔲 [AUTO] Sprint 0 sign-off (all green on `main`):
  - `validate-bootstrap.sh --quick`
  - `feature-gate.sh --stack multi`
  - `check-github-ci.sh --wait 300` after first push
  - `check-license-compliance.sh`

#### Parallel (safe after Sequential step 1)

| Task | Owner | Isolated scope |
|------|-------|----------------|
| Keep web Golden Path examples healthy | AGENT | `examples/web/**` |
| Keep android module docs aligned | AGENT | `modules/android/**`, `examples/android/**` |

---

## Ongoing Maintenance

- 🔲 [HUMAN] Weekly Dependabot / CVE triage (`docs/SECURITY_TRIAGE.md`)
- 🔲 [HUMAN] Enable GitHub Pages if a public demo is wanted (404 at `/ship` 2026-08-14)
- ✅ [HUMAN] Attach desktop installer / Android APK to GitHub Release (v0.19.0: EXE 0.17.3 + APK 1.10.7)
- 🔲 [AUTO] Triage Dependabot medium `glib` in `apps/desktop/src-tauri/Cargo.lock` (do not bump to 0.20 — GTK4)
- ✅ [AUTO] Confirm v0.19.0 GitHub Release has SBOMs + product binaries (2026-08-17 `/ship`)
- 🔲 [AUTO] Template update check (`scripts/check-template-updates.ps1`)
- 🔲 [HUMAN] Quarterly ROADMAP review
