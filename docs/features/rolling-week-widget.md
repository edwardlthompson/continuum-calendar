# Feature: rolling-week-widget

## Acceptance criteria

- ✅ User-visible: homescreen widget “Rolling week widget from today” shows 7 day columns starting today (or week start when `rollingWeekWidget` is off)
- ✅ Offline: uses local Fossify events
- ✅ Accessibility: each day column opens that day
- ✅ i18n: `@string/rolling_week_widget`

## Smoke scenario

1. Add the rolling week widget
2. Today is column 1; tap a day to open Daily view

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/mobile/.../continuum/RollingWeek.kt` |
| View | `widget_rolling_week.xml`, `RollingWeekWidgetProvider.kt` |
| Tests | `RollingWeekTest.kt` |
| Wiring | `AndroidManifest.xml` receiver |

## Tests

- Automated: yes — `apps/mobile/app/src/test/kotlin/org/fossify/calendar/continuum/RollingWeekTest.kt`
- Coverage: today-first 7-day strip

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py feature-gate --stack android`
