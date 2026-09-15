# Feature: desktop-h1-version

Drop the installed version from the main H1. Version stays in Settings / About / update dialogs.

## Acceptance criteria

- User-visible behavior: header reads `Continuum Calendar` with no `1.0.0` (or other) suffix
- Offline/error behavior: Tauri `getVersion` failure does not affect the title
- Accessibility: a single `h1`; version not announced twice
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given the desktop app
2. When the user looks at the header
3. Then the title has no version string

## Container map

| Layer | Path |
|-------|------|
| Logic | none |
| View | `apps/desktop/src/components/AppTitle.tsx` |
| Tests | `apps/desktop/src/components/AppTitle.test.ts` (render title text) |
| Wiring | already imported from App.tsx — do not grow App.tsx |

## Tests

- Automated: yes — AppTitle unit test asserts heading text is exactly `Continuum Calendar`
- Coverage: no version substring in H1

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Settings / update dialogs still show the installed version.

## Notes

Sprint 5 deferred this on purpose. Sequential lock for Sprint 6.
