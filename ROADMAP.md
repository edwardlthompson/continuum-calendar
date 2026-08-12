# ROADMAP — Continuum Calendar

## Vision

A FOSS calendar ecosystem that treats **time as a continuum**: rolling views anchored on today, schedules that show open days, and practical Google Calendar/Contacts integration on desktop with an Android widget that mirrors those ideas.

## Phase 0 — Foundation (current)

- Bootstrap from `agent-project-bootstrap`
- Monorepo: desktop Tauri app, shared types, mobile fork docs
- Prototype rolling week + theme + free-slot copy + Google API module stubs

## Phase 1 — Desktop MVP

- Complete Google OAuth (PKCE) with secure storage
- Two-way Calendar sync
- Contacts-powered attendee autocomplete
- Agenda list mode with explicit empty days
- Packaged installers via Tauri (Windows first)

## Phase 2 — Mobile fork

- Submodule FossifyOrg/Calendar into `apps/mobile`
- Settings: `show_empty_days_in_agenda`, theme Material You + L/D/System
- System Contacts quick-invite
- Rolling week homescreen widget with free gaps

## Phase 3 — Parity & polish

- Shared free-slot algorithms tested on both platforms
- Offline mock mode + conflict handling for sync
- Accessibility pass (WCAG 2.2 desktop; Android a11y)
- F-Droid / GitHub Releases distribution

## Non-goals (near term)

- iOS client
- Proprietary Play Services dependency for core features
- Multi-account enterprise admin console

## Success metrics

- Rolling week always places today in column 1
- Empty days remain visible in agenda and widget
- User can create a meeting with contact autocomplete in ≤3 interactions (desktop)
- Theme preference survives restart on desktop and mobile
