# ROADMAP — Continuum Calendar

Last reviewed: 2026-09-11 (quarterly).

## Vision

A FOSS calendar ecosystem that treats **time as a continuum**: rolling views anchored on today, schedules that show open days, and practical Google Calendar integration on desktop with an Android companion that mirrors those ideas.

## Shipped (Phase 0–1 + mobile fork)

- Bootstrap from `agent-project-bootstrap` (child of v1.4.0)
- Desktop Tauri app: rolling week, agenda empty days, Google Calendar Sign in (PKCE + loopback), Drive App Data peer sync
- Packaged Linux `.deb` (`npm run install:local` / `dpkg -i`) and Windows NSIS target
- Android Fossify fork (`org.continuumcalendar.app`): CalDAV Google calendars + optional Continuum OAuth for desktop peer sync
- GitHub Releases as the distribution channel (EXE + `.deb` + FOSS APK)
- **v1.0.0** first stable public release
- Optional Google Tasks agenda overlay (Connect Google Tasks; default Sign in stays Calendar + Drive)
- Rolling-week Android homescreen widget
- Optional Ubuntu `Desktop Deb` workflow (`workflow_dispatch`)

## Now (packaging & hardening)

- Public OAuth clients already exist (Desktop + Android debug). Release Android client uses package `org.continuumcalendar.app` + release SHA-1
- Consent screen: External / In production; privacy URL on Branding
- F-003: desktop app-config token vault (mode 0600) + Android EncryptedSharedPreferences
- ADB device smoke after Sign in

## Next

- Contacts attendee autocomplete (desktop still omits Contacts scope while KB-028 applies)
- OAuth verification if the 100-user cap on unapproved sensitive scopes becomes a problem

## Non-goals (near term)

- iOS client
- Proprietary Play Services dependency for core features
- F-Droid / Winget / Play Store (GitHub Releases only)
- Multi-account enterprise admin console

## Success metrics

- Rolling week always places today in column 1
- Empty days remain visible in agenda and widget
- Sign in with Google is one tap after a maintainer bake — end users never create a Cloud project
- Theme preference survives restart on desktop and mobile
