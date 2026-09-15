# Feature: desktop-chrome-feel

Desktop chrome matches GCal/Outlook: settings overlay, dismissible menus, in-app dialogs instead of `window.prompt`, editor side sheet, Open row on empty today.

## Acceptance criteria

- User-visible behavior: Settings covers the calendar as a dialog/drawer; header Menu closes on outside click and Escape; Jump / ICS / CalDAV use in-app dialogs; event editor is a side sheet with the week/agenda still visible; today’s empty agenda shows an Open row
- Offline/error behavior: dialogs cancel without mutating calendars; reset-defaults requires confirm
- Accessibility: settings overlay `role="dialog"` + Escape; focus returns to the opener
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given the desktop app is on rolling week
2. When the user opens Settings, presses Escape, opens Menu and clicks outside, then edits an event
3. Then Settings and Menu close, the calendar stays visible beside the editor, and no `window.prompt` appears

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/settings/` overlay + dialog helpers |
| View | `SettingsPanel.tsx`, `EventEditor.tsx`, `AgendaView.tsx`, `CalendarToolbar` / App chrome |
| Tests | co-located unit tests for menu dismiss and jump dialog |
| Wiring | `App.tsx` chrome only |

## Tests

- Automated: yes — menu dismiss, prompt-less jump dialog, settings overlay presence
- Coverage: Escape closes overlay; empty-today Open row

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Sequential Feature 1 must be ✅ first. Browser: open settings, Esc, edit event with calendar still visible.

## Notes

- Soften maintainer OAuth packaging copy in `SettingsAccount.tsx` for packaged builds.
- Conflict chrome: brand tokens; keep ⚠️ as secondary.
