# Feature: multi-google-calendar-sync

## Acceptance criteria

- ✅ User-visible: hidden secondary Google calendars stay off the grid; primary still syncs
- ✅ Offline/error: per-calendar failures stay partial (`errors[]`)
- ✅ Accessibility: N/A (sync)
- ✅ i18n: N/A

## Smoke scenario

1. Uncheck a secondary calendar in the sidebar
2. Sync — that calendar is not pulled; primary still updates

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/services/googleCalendarSyncIds.ts` |
| View | N/A (sync) |
| Tests | `googleCalendarSyncIds.test.ts` |
| Wiring | `syncService.ts` |

## Tests

- Automated: yes — `apps/desktop/src/services/googleCalendarSyncIds.test.ts`
- Coverage: primary + visible secondaries; skip hidden

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py feature-gate --stack node`
