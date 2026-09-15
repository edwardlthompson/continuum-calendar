# Feature: desktop-voice-first-run

Signed-out Google relabel (never hide), first-run copy, Menu prune, idle status hidden, editor date fields readable.

## Acceptance criteria

- User-visible behavior: google calendars when signed out say “Sign in to sync”; header is “On this computer” or “Signed in”; Menu keeps scheduling plus Import calendar; Donate lives in Settings Account; idle status bar hidden
- Offline/error behavior: missing displayName uses calendar id; flash timer cleared on unmount
- Accessibility: Start/End hour and minute have distinct aria-labels; date field min 11ch
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given the desktop app signed out
2. When the user reads the sidebar and header, then opens Menu
3. Then Google is labeled Sign in to sync, the header is not “Not synced”, and Donate is absent from Menu

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/chrome/calendarLabel.ts` |
| View | `App.tsx`, `SettingsAccount.tsx`, `EventEditor` / `DateTimeLocalField` / `DateTimePopovers` |
| Tests | `apps/desktop/src/chrome/calendarLabel.test.ts` |
| Wiring | map calendars through `calendarCaption` before sidebar |

## Tests

- Automated: yes — `apps/desktop/src/chrome/calendarLabel.test.ts`
- Coverage: signed-out google suffix; signed-in keeps name; empty displayName → id

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

`GOOGLE_API_SETUP` does not appear under `apps/desktop/src`.

## Notes

Do not hide Google Primary. Relabel only.
