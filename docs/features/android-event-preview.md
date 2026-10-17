# Feature: android-event-preview

## Acceptance criteria

- ✅ User-visible behavior: Opening an existing event on Android shows a Google Calendar–style preview (color chip + title + when line; icon rows for location, reminders, organizer when present, calendar name/email; description + OSM map when available). Pencil enters the existing edit form.
- ✅ Offline/error behavior: Empty rows are hidden; map geocode failure hides the map only; no creator row without an organizer attendee.
- ✅ Accessibility: Preview map keeps content description; toolbar edit/share/delete unchanged.
- ✅ i18n: Preview reminder/when English labels match desktop EventDetailCard tone for this slice (formatter strings in `ContinuumEventPreviewFormat`).

## Smoke scenario

1. _Given_ an event with title, time, location, and reminder
2. _When_ user opens it from day/week (view mode)
3. _Then_ preview sheet appears (not the locked editor); pencil opens the editable form without crashes

## Container map

| Layer | Path |
|-------|------|
| Logic | `continuum/ContinuumEventPreviewFormat.kt` |
| View | `res/layout/event_preview_sheet.xml`, `activity_event.xml` preview scroll |
| Tests | `ContinuumEventPreviewFormatTest.kt` |
| Wiring | `EventActivity` view-mode show/hide ≤ thin chrome switch |

## Tests

- Automated: yes — `apps/mobile/app/src/test/kotlin/.../ContinuumEventPreviewFormatTest.kt`
- Coverage: when-line, reminders, organizer

## Fallback validation

- Why tests are not feasible: N/A for formatters; full UI fall back below
- Command: `python3 scripts/agent-run.py feature-gate --stack android` (then install debug APK → open existing event → confirm preview → edit)

## Definition of Done

See `docs/FEATURE_MODULES.md`. No Room schema change.

## Notes

- No Google hero artwork (no Continuum event image field).
