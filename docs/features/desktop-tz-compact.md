# Feature: desktop-tz-compact

Replace the editor’s full IANA dump with a short common-zone list plus filter. Store real IANA ids.

## Acceptance criteria

- User-visible behavior: Time zone control shows a compact common list (device zone first) and a filter field; picking a zone writes the IANA id
- Offline/error behavior: unknown existing `timeZone` stays selectable as a one-off option; empty filter shows the common list
- Accessibility: combobox/list has a name `Time zone`; filter is a labeled search
- i18n: N/A — zone ids stay IANA English

## Smoke scenario

1. Given a timed event in the editor
2. When the user opens Time zone
3. Then they are not facing an 80-option unlabeled dump

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/chrome/timeZones.ts` |
| View | `apps/desktop/src/components/EventEditor.tsx` (TZ field only) |
| Tests | `apps/desktop/src/chrome/timeZones.test.ts` |
| Wiring | none in App.tsx |

## Tests

- Automated: yes — `timeZones.test.ts`
- Coverage: device zone first; filter by substring; unknown id preserved; common list length ≤ 24

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Do not add a Day view or NL add. Do not grow EventEditor past the TZ control.

## Notes

Sprint 5 left the mega-list out. Logic file under 150 lines.
