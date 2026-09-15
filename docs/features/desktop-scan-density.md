# Feature: desktop-scan-density

Fold extra Open days, drop FullCalendar prev/next/today, compact search, quiet sidebar notify prefs, view subtitle in the toolbar.

## Acceptance criteria

- User-visible behavior: at most 7 empty Open days plus one “Show N Open days” row; Week/Month show a date title without a second Today; search is max 16rem; New/Reminder sit behind a per-calendar control; holidays omit New
- Offline/error behavior: empty section list stays empty; `maxEmpty: 0` means 7; overflow omitted when count is 0
- Accessibility: fold control is a button; notify prefs `aria-expanded`; Open hover uses accent-soft
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given agenda with empty days on and a 30-day range
2. When the user opens Agenda
3. Then today and busy days stay visible, extra Opens fold, and Week has one Today control

## Container map

| Layer | Path |
|-------|------|
| Logic | `packages/shared/src/agendaFold.ts` |
| View | `AgendaView.tsx`, `RollingWeekView.tsx`, `CalendarToolbar.tsx`, `CalendarSidebar.tsx` |
| Tests | `packages/shared/src/agendaFold.test.ts` |
| Wiring | none in App.tsx |

## Tests

- Automated: yes — `packages/shared/src/agendaFold.test.ts`
- Coverage: empty input; cap 7; today always kept; all-day Open days count; past-today Open when nowMs is set; folded count

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

`buildAgendaSections` unchanged. Android does not import the fold helper.

## Notes

Expand is session state only.
