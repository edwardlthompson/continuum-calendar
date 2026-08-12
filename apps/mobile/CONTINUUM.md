# Continuum Calendar — mobile source

Shallow clone of FossifyOrg/Calendar used for local sideload builds.

- Flavor built/installed: `fossDebug` (`org.continuumcalendar.app.debug`)
- Branding: Continuum Calendar name + Continuum mark adaptive icons
- Maintainer HUMAN checklist: [`docs/HUMAN_REMAINING.md`](../../docs/HUMAN_REMAINING.md)
- Continuum scheduling (parity with desktop):
  - Main menu: **Copy free slots**, **Propose times**, **Jump to next free block**
  - Settings → Continuum: Connect Google calendars (device CalDAV), empty days, redact titles, travel buffer, working hours, min slot
  - Agenda empty days fill uses `agendaRangeDays` (default 30)
  - `ContinuumSettingsPoller` runs while MainActivity is foregrounded
