# Feature: desktop-open-illustrations

Small Continuum-owned empty-state marks for Open / splash rest states — not stock calendar clipart.

## Acceptance criteria

- User-visible behavior: Open empty invitation and splash rest state use a local SVG (or CSS mark) from `apps/desktop/src/illustrations/`
- Offline/error behavior: missing asset falls back to the existing Open text/button (no broken image)
- Accessibility: decorative marks `aria-hidden`; Open button label unchanged
- i18n: N/A

## Smoke scenario

1. Given Agenda with an Open day
2. When the user looks at the Open row
3. Then they see the Continuum mark, not a generic stock icon

## Container map

| Layer | Path |
|-------|------|
| Logic | none |
| View | `apps/desktop/src/illustrations/` + `ContinuumSplash.tsx` rest frame |
| Tests | `apps/desktop/src/illustrations/marks.test.ts` (export exists; SVG has no external URL) |
| Wiring | Open row may import one mark; do not rewrite Agenda fold logic |

## Tests

- Automated: yes — marks export + no `http` in SVG source
- Coverage: at least one Open mark and one splash mark

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Do not replace the ∞ brand or add a Day view. Keep files small.

## Notes

Sprint 5 left custom illustrations out. Isolate under `illustrations/` so AgendaView fold code stays owned by Sprint 5.
