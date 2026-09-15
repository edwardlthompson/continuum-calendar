# Feature: android-rolling-widget-faq

Rolling-week homescreen widget today/empty states, Widgets settings toggle, Continuum Connect FAQ, privacy About URL, and `strings.xml` for Open/empty copy.

## Acceptance criteria

- User-visible behavior: widget highlights today, distinguishes empty vs busy days, **Open** stays one line on a 4×1 strip, and uses dedicated preview art; Settings → Widgets exposes `rollingWeekWidget`
- Offline/error behavior: empty day list keeps the count-only fallback; missing RemoteViews does not crash the provider
- Accessibility: widget day cells remain clickable content descriptions via existing launch intents
- i18n: hardcoded `"Open"` / empty strings move to `strings.xml`; FAQ #2 points at Continuum Connect Google (not official Google Calendar / DAVx5)

## Smoke scenario

1. Given a device or emulator with the rolling-week widget
2. When today has no events and another day in the strip has events
3. Then today is highlighted, empty vs busy are distinct, and FAQ/privacy About use Continuum URLs

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/mobile/.../continuum/RollingWeekWidgetProvider.kt`, `RollingWeek.kt` |
| View | `widget_rolling_week.xml`, Settings Widgets, FAQ/About strings |
| Tests | `RollingWeekTest.kt` plus string-resource checks |
| Wiring | Fossify widget provider already registered |

## Tests

- Automated: yes — widget day-count / empty fallback unit tests; string resources present
- Coverage: provider with empty day list; Open count-row size stays one line; FAQ copy does not recommend DAVx5 as the Google path

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py feature-gate --stack android`

## Definition of Done

ADB glance passed 2026-09-14 on OP13 (`8bf09993`): today column highlighted, empty days **Open**, Tuesday showed **1** after a local event. Do not reboot or wipe phones.

## Notes

- Privacy About row: `docs/PRIVACY.md` URL, not repo root.
