# Feature: desktop-command-palette

Keyboard command palette for existing actions (views, Today, Settings, new event). Not natural-language add.

## Acceptance criteria

- User-visible behavior: Ctrl/Cmd+K opens a filterable command list; choosing a command runs it and closes the palette
- Offline/error behavior: empty query shows the full list; unknown query shows `No matching commands`
- Accessibility: dialog with `role="dialog"`; listbox; Escape closes and returns focus to the opener
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given the desktop app
2. When the user presses Ctrl+K and types `week`
3. Then Week is focused and Enter switches view

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/chrome/commandPalette.ts` |
| View | `apps/desktop/src/chrome/CommandPalette.tsx` |
| Tests | `apps/desktop/src/chrome/commandPalette.test.ts` |
| Wiring | App.tsx ≤10 lines (hotkey + render) |

## Tests

- Automated: yes — `commandPalette.test.ts`
- Coverage: filter by label; empty query → all; no-match → []; does not parse event titles as commands

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Not NL add. Palette does not create events from free text.

## Notes

Sprint 5 left this out. Do not overlap EventEditor or AppTitle.
