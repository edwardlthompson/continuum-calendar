# Continuum parity matrix (desktop ↔ Android)

| Capability | Desktop | Android | Settings key / sync |
|------------|---------|---------|---------------------|
| Google account connect | Sign in with Google (PKCE; Tauri uses system browser + loopback; needs `VITE_GOOGLE_CLIENT_ID`) | **Connect Google calendars** via phone Google account / CalDAV | tokens local only |
| Google Calendar sync | `calendarList` + sync **all visible** Google calendars (partial success OK); event colors from calendar | CalendarContract (device calendars); optional Continuum API path | `visibleCalendarIds` |
| Quick filter chips | N/A — sidebar checkboxes (desktop chrome) | Fossify quick filter bar | visibility local + settings |
| Default main view | **Agenda** (rolling week secondary) | `EVENTS_LIST_VIEW` / agenda list | — |
| FAB new event | Bottom-right FAB | Fossify FAB | `defaultWriteCalendarId` |
| Header overflow menu | Copy free slots / Propose / Jump / Open calendar link | Main menu actions | — |
| Google Tasks scope | In shared OAuth scopes | In ContinuumConsts | optional / **deferred UI** |
| CalDAV (other servers) | Direct client | CalendarContract | passwords local |
| Local calendars | SQLite/store | Fossify local | peer via `continuum-local-events.json` |
| Local events peer sync | Drive App Data LWW + tombstones | Room `SOURCE_SIMPLE_CALENDAR` / ICS ↔ same file | `continuum-local-events.json` |
| Privacy (no Google Calendar) | Toggle hides Google cals; keeps Drive App Data peer sync | Same Continuum toggle; hides Google CalDAV display | `useGoogleCalendar` |
| 24-hour time | Settings toggle for Agenda; **rolling week grid always 24h** for denser columns | Fossify hour format + Continuum push/apply | `use24HourFormat` |
| Sync / status messages | Fixed bottom status bar (no layout jump) | Toasts / Continuum log | — |
| First day of week | Settings + FullCalendar `firstDay` | Fossify start week on + Continuum push/apply | `firstDayOfWeek` (0=Sun) |
| ICS file open | OS `.ics` association (Tauri) + drag-and-drop + Settings import | `ACTION_VIEW` file/content + MIME | import calendar `ics-import` |
| Calendar links (webcal / HTTP ICS) | Menu/Settings paste; OS argv URL → fetch (timeout + size cap) | `BROWSABLE` webcal/http(s) + download import | — |
| ICS export | Yes | Yes (Fossify) | — |
| Holidays pack | Seed calendar (local) | Fossify holidays | **deferred** polish |
| Rolling week from today | Yes (wired to view) | `startWeekWithCurrentDay` / Continuum default on | `rollingWeekFromToday` |
| Agenda empty days | Yes — teal **Open** placeholder (`#0F6E8C`) | Yes (`AgendaEmptyDays`) | `showEmptyDaysInAgenda` |
| Agenda day headers | Shared `formatAgendaSectionTitle` — `Today · Tuesday, August 11` / `Wed · August 12` | Same via `Formatter.getAgendaSectionTitle` | — |
| Agenda density / range | Settings knobs | Continuum settings | `agendaDensity`, `agendaRangeDays` |
| Agenda open-days widget | **Deferred** (tray/mini) | Homescreen list + Open chips | widget layout local |
| Copy free slots | Header menu | Main menu + Continuum settings knobs | `workingHours`, `slotMinMinutes`, `travelBufferMinutes` |
| Propose times | Header menu | Main menu | same |
| Jump to next free block | Header menu (opens editor) | Main menu (opens EventActivity) | same |
| Travel buffer / working hours / slot min | Continuum settings panel | Continuum Settings (clock pickers) | ContinuumSettings envelope |
| Show contact birthdays | Toggle (hides Google Birthdays calendar ids/names) | Toggle (local contact-birthday + Google `addressbook#…` / “Birthdays” CalDAV) | `showContactBirthdays` |
| Local crash / error log | Settings → Download error log | Settings → View Continuum error log (local file, no telemetry) | — |
| Theme L/D/System | Yes | Continuum theme mode → AppCompat night mode | `themeMode` |
| Weekly view days / snooze / notifications | Settings knobs | Continuum Settings + Fossify snooze/weekly days | `weeklyViewDays`, `defaultSnoozeMinutes`, `notificationEnabled` |
| Redact titles (screenshots) | Toggle | Toggle | `redactTitlesInScreenshots` |
| Settings App Data sync | **Peer remote:** either device seeds/pushes/pulls Drive `continuum-settings.json` (CAS). Sign-in reconciles (seed if empty, push pending, else pull). 1s FG poll. | **Peer remote:** same file; CalDAV for calendars + Continuum Google API for settings. Poller runs `reconcilePeerRemote`. Same GCP Client ID project as desktop. | envelope revision |
| Event editor | Repeat, TZ, up to 3 reminders, busy/visibility/color, location search, attendees | Fossify `EventActivity` | `defaultReminderMinutes` |
| Location autocomplete | History + Photon via native `ureq` (User-Agent) | History + Geocoder, then Photon | — |
| Date / calendar pickers | Native `date` / `datetime-local` / `<select>` themed via `color-scheme` | Fossify themed `DatePickerDialog` | — |
| Recurrence | Daily/weekly/monthly/yearly + until (local expand; Google RRULE on create) | Repeat interval / rule / until | — |
| Time zone on event | IANA picker | Per-event TZ picker | — |
| Extra reminders | Up to 3 popup reminders | Up to 3 reminders + type | — |
| Busy / free / visibility / event color | Busy checkbox, visibility, color swatches | Availability, access, color | — |
| Open location on map | OSM search link | `geo:` / map intent | — |
| Month “today” cell | Red inset ring (`--cc-brand-now`) | Red cell surround (`continuum_brand_now`) | — |
| Reminders / notifications | OS toast | AlarmManager | reminder fields on events |
| Contacts autocomplete | People API | ContactsContract + People | — |
| Grid click → edit/create | Rolling week eventClick / select | Fossify week/day native | — |
| Default calendar for new events | Settings dropdown + sidebar ★; FAB/editor respects it | Settings → Default event type/calendar (Fossify) | `defaultWriteCalendarId` |
| Year/Month-day combo switcher | Toolbar Week / Month / Year + jump-to-date | Fossify view dialog | — |
| Event search | Header search (/) | Fossify search | — |
| Recurrence edit scope | This / following / all | Fossify one/all | — |
| Holiday pack | Settings country pack (US/CA/GB/DE) | Fossify holidays | desktop local |
| Tray remaining-today | Taskbar overlay + tray tooltip counts down | N/A | — |
Logical calendar ids: `{source}:{calendarId}` e.g. `google:primary`.
