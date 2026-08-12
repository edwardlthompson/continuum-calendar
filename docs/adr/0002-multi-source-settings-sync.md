# ADR-0002 — Multi-source calendars and settings sync

- Status: Accepted
- Date: 2026-08-10

## Context

Continuum needs Google SSO parity across desktop and Android, Fossify-style multi-calendar support on desktop, and cross-device settings without a Continuum backend.

## Decision

1. **Google Calendar API** is authoritative for Google-backed calendars when Continuum SSO is signed in (disable/read-only system Google CalDAV on Android to avoid duplicates).
2. **CalDAV / local / ICS / holidays** remain first-class sources via `CalendarSource`.
3. **Settings** sync through Drive App Data as `ContinuumSettingsEnvelope` with monotonic `revision` + Drive etag CAS; foreground ~1s metadata poll for live apply.
4. OAuth scopes: `calendar`, `contacts.readonly`, `drive.appdata`.

## Consequences

- Desktop implements CalDAV directly; Android keeps CalendarContract for non-Google CalDAV.
- Settings passwords/tokens never leave the device via App Data.
- “Realtime” settings means ~1–2s while apps are foregrounded.
