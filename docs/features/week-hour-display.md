# Feature: week-hour-display

## Acceptance criteria

- ✅ User-visible behavior: Desktop week grid always shows 00:00–24:00. Toggle between **Scroll** (tall rows, vertical scroll) and **Fit 24 hours** (compress into the pane). Control lives on the week toolbar and under Settings → Appearance → Week hours. Default is Scroll.
- ✅ Offline/error behavior: Setting persists in local Continuum settings envelope; peer sync carries `weekHourDisplay` when signed in.
- ✅ Accessibility: Toolbar segmented control uses `aria-pressed` and `role="group"`; settings row is a labeled select.
- ✅ i18n: English UI labels only for this slice (desktop settings/toolbar strings inline, matching existing Appearance pattern).

## Smoke scenario

1. _Given_ desktop app is on Week view
2. _When_ user switches Fit 24h ↔ Scroll and creates/views events before 08:00 or after 17:00
3. _Then_ those hours are visible (scroll or compressed) without console errors; working hours still affect free-slot / propose only

## Container map

| Layer | Path |
|-------|------|
| Logic | `packages/shared/src/settings.ts`, `apps/desktop/src/grid/gridMove.ts` |
| View | `RollingWeekView.tsx`, `CalendarToolbar.tsx`, `SettingsAppearance.tsx` |
| Tests | `gridMove.test.ts`, `packages/shared/src/settings.weekHour.test.ts` |
| Wiring | `App.tsx` ≤10 lines |

## Tests

- Automated: yes — `apps/desktop/src/grid/gridMove.test.ts`, `packages/shared/src/settings.weekHour.test.ts`
- Coverage: full-day window + normalize default

## Fallback validation

- Why tests are not feasible: N/A
- Command: `python3 scripts/agent-run.py feature-gate --stack web`

## Definition of Done

See `docs/FEATURE_MODULES.md`. Working hours no longer clip the week grid.

## Notes

- Android week already scrolls 24h; no Android UI for this toggle in this slice (field peer-synced for forward compatibility).
