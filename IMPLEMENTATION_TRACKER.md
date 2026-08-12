# Implementation Tracker — Continuum Calendar

Status markers: 🔲 open · ✅ done · ❌ blocked

Last updated: 2026-08-10

## Shared schema
| ID | Task | Status |
|----|------|--------|
| S1 | CalendarSource, reminders, ContinuumSettingsEnvelope | ✅ |
| S2 | PARITY_MATRIX + ADR-0002 | ✅ |
| S3 | Kotlin continuum mirrors | ✅ |

## Desktop
| ID | Task | Status |
|----|------|--------|
| D1 | Google PKCE SSO + token store | ✅ |
| D2 | syncToken sync loop | ✅ |
| D3 | Agenda + empty days | ✅ |
| D4 | Event editor + contacts autocomplete | ✅ |
| D5 | OS notifications from reminders | ✅ |
| D6 | Multi-cal: local, CalDAV, ICS, holidays sidebar | ✅ |
| D7 | Settings App Data CAS + 1s poll | ✅ |
| D8 | Redact titles, propose times, free slots, jump-to-free | ✅ |

## Android
| ID | Task | Status |
|----|------|--------|
| M1 | Continuum Google PKCE auth + SyncWorker | ✅ |
| M2 | show_empty_days_in_agenda + defaults | ✅ |
| M3 | Agenda widget Open days | ✅ |
| M4 | Settings sync client + poller | ✅ |
| M5 | Redact titles toggle | ✅ |
| M6 | FreeSlots helper | ✅ |
| M7 | ProposeTimes + menu Copy/Propose/Jump | ✅ |
| M8 | Continuum Settings: travel buffer / hours / slot min | ✅ |
| M9 | agendaRangeDays=30 + SettingsPoller in MainActivity | ✅ |

## Desktop (CC2)
| ID | Task | Status |
|----|------|--------|
| D9 | rollingWeekFromToday wired to RollingWeekView | ✅ |
| D10 | Grid eventClick / select → EventEditor | ✅ |
| D11 | Working hours + slot min settings UI | ✅ |

## Human gates
| ID | Task | Status |
|----|------|--------|
| H1 | Continuum product public OAuth Client ID (maintainer packaging; not per-user) | 🔲 | Desktop PKCE + Tauri loopback ready; needs `VITE_GOOGLE_CLIENT_ID` in `apps/desktop/.env` |
| H2 | Live SSO demo with Continuum client ID | 🔲 |
