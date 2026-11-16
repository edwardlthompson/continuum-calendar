# CMP ↔ shipping desktop parity gaps

Behavioral parity for `kmp/` Compose Multiplatform vs shipping Tauri desktop (`apps/desktop`). Not pixel-identical — same in practice.

Last audit: 2026-11-15 (updated after event round-trip + week drag).

## Status legend

| Tag | Meaning |
|-----|---------|
| ✅ | Practical parity in CMP |
| 🔶 | Partial |
| ❌ | Missing |
| ⏸ | Deferred (P2 / desktop-only polish) |

## P0 — daily path

| ID | Area | Old behavior | CMP | Status |
|----|------|--------------|-----|--------|
| P0-SET | Settings | Full categories: Account, Appearance, Calendar & views, Reminders, Scheduling, Window, Import/export + search | Peer categories + category search + Window prefs; Files under P2-ICS | ✅ |
| P0-CAL | Calendars | Visibility, ★ default write, New/Reminder prefs | Settings calendar list + visibility filter | ✅ |
| P0-CRUD | Events | FAB / Open day / editor create·edit·delete | Editor + Google write preserves guests/reminders/recurrence; all-day convert; date pickers polish later | ✅ |
| P0-AGENDA-OPEN | Agenda | Open day → new event at work-hours start | Wired to editor at work-hours start | ✅ |
| P0-AUTH | Auth | Sign in/out, reconnect banner, Drive for peer | In-app PKCE loopback OAuth + Sign out + reconnect banner; Drive peer = P1-SYNC | ✅ |

## P1 — core product

| ID | Area | Old | CMP | Status |
|----|------|-----|-----|--------|
| P1-WEEK | Week | Time-grid, drag/resize, scroll/fit24, day shift | Scroll / Fit 24h / Agenda + gutter/now/slots; vertical drag-move + resize | ✅ |
| P1-TOOLBAR | Chrome | Search, go-to date, focus Today | Search + Go date + Today resets focus/shift | ✅ |
| P1-SYNC | Sync | Peer settings/local events + overlays | Drive App Data settings + local-events overlay on refresh (needs drive.appdata scope) | ✅ |
| P1-DETAIL | Detail | Edit, maps, guests, reminders | Side sheet: guests/reminders/repeat; clickable location + OSM preview + map chooser; Contacts/suggest = P2 | ✅ |
| P1-CONFLICT | Conflicts | Banner + save dialog | Overlap detect, banner, yellow highlight | ✅ |
| P1-MONTH-YEAR | Views | Month & year grids | Month grid + year overview (not FullCalendar) | ✅ |
| P1-REMIND | Reminders | OS notify + defaults | Tray notify + per-event reminders; snooze matches shipping (settings-only) | ✅ |
| P1-SCHED | Scheduling | Working hours / buffer / slot min | Settings + agenda Open free-slot hint (buffer / slot min / working hours) | ✅ |

## P2 — polish / power

| ID | Area | Status |
|----|------|--------|
| P2-HOTKEYS | N/T/1–4/F/G/Ctrl+K | ✅ |
| P2-ICS-CALDAV | Import/export/subscribe + CalDAV | ✅ ICS + settings backup + CalDAV account store |
| P2-HOLIDAY | Holiday packs | ✅ |
| P2-TASKS | Google Tasks overlay | ✅ |
| P2-BIRTHDAY | Automated birthday calendars | ✅ (toggle + eventType/cal filter) |
| P2-WINDOW | Tray / minimize / login | ✅ Open at startup + minimize/close-to tray |
| P2-MENU | Free slots / propose | ✅ |
| P2-MISC | Splash, donate, update, crash feedback, maps, Contacts | ✅ Splash + About update/feedback/Venmo + maps; Contacts suggest deferred (birthday toggle ships) |

## ContinuumSettings field checklist

| Field | Old UI | CMP UI |
|-------|--------|--------|
| themeMode | ✅ | ✅ |
| use24HourFormat | ✅ | ✅ |
| showEmptyDaysInAgenda | ✅ | ✅ |
| weekHourDisplay | ✅ | ✅ (Scroll / Fit 24h / Agenda) |
| redactTitlesInScreenshots | ✅ | ✅ |
| useGoogleCalendar | ✅ | ✅ |
| showContactBirthdays | ✅ | ✅ |
| firstDayOfWeek | ✅ | ✅ |
| weeklyViewDays | ✅ | ✅ |
| rollingWeekFromToday | ✅ | ✅ |
| visibleCalendarIds / notify prefs / ★ | ✅ | ✅ |
| defaultWriteCalendarId | ✅ | ✅ |
| agendaRangeDays | ✅ | ✅ |
| agendaDensity | ✅ | ✅ |
| notificationEnabled / reminder / snooze | ✅ | ✅ (UI + tray notify) |
| workingHours / travelBuffer / slotMin | ✅ | ✅ |
| secondaryTimeZone | model only | model only |

Desktop-local (not in peer settings): holiday pack ❌; window/tray 🔶 (open at startup + prefs); ICS/CalDAV ❌; settings JSON ❌; crash/feedback ❌; detail maps ✅.

## Execution order

1. **P0-SET + P0-CAL** — Settings panel for all peer `ContinuumSettings` + calendar list
2. **Persist settings** + filter events by visibility
3. **P0-CRUD + P0-AGENDA-OPEN** — editor sheet; FAB / Open day; Google insert/patch/delete when vault signed in
4. **P0-AUTH** — Sign out + reconnect messaging (vault already loads)
5. **P1** — toolbar search/jump, richer week, month/year, conflicts, reminders UI
6. **P2** as needed

## Source map

- Old settings: `apps/desktop/src/settings/*`, model `packages/shared/src/settings.ts`
- CMP model: `kmp/continuum-domain/.../SettingsModels.kt`
- CMP shell: `kmp/continuum-ui/.../ContinuumShell.kt`
