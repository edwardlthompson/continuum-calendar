# Feature: desktop-grid-fidelity

Week/month grid interactions match Google Calendar / Outlook: drag and resize commit, slot create uses local wall time, and the 12/24-hour setting applies to the grid.

## Acceptance criteria

- User-visible behavior: dragging or resizing a timed event persists via the existing save path; recurring events prompt This / Following / All before write; read-only calendars are not editable
- Offline/error behavior: save failure or a cancelled scope/conflict dialog reverts the FullCalendar event; empty/missing id is ignored
- Accessibility: grid remains keyboard-selectable; scope dialog is `role="dialog"` with Cancel
- i18n: N/A — desktop copy stays English (no `locales/` on this stack)

## Smoke scenario

1. Given the desktop app is showing rolling week with a timed local event
2. When the user drags it to a new hour, creates from an empty slot, and toggles 12-hour time
3. Then the event stays at the new time after reload of in-memory state, the editor shows local wall time (not UTC), and slot labels follow the 12/24 setting, without console errors

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/grid/gridMove.ts` |
| View | `apps/desktop/src/components/RollingWeekView.tsx`, `apps/desktop/src/grid/RecurrenceScopeDialog.tsx` |
| Tests | `apps/desktop/src/grid/gridMove.test.ts`, `apps/desktop/src/utils/dateTimeLocal.test.ts` |
| Wiring | `App.tsx` `onMoveEvent` / `use24HourFormat` / slot window (≤10 lines of new composition) |

## Tests

- Automated: yes — `apps/desktop/src/grid/gridMove.test.ts` plus `dateTimeLocal` local-wall-time cases
- Coverage: empty drop payload, inverted working hours fallback, series vs single, local vs UTC slot strings

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

See `docs/FEATURE_MODULES.md` per-feature checklist. Gate after this row: `watch-agent-gates --once --autofix --scope auto`. Browser: drag one timed event, create from a slot, toggle 12h.

## Notes

- Recurring drag must not silent-patch the series.
- Working hours empty or inverted → `06:00:00`–`22:00:00`.
