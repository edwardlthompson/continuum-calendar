# Feature: google-tasks-ui

## Acceptance criteria

- ✅ User-visible: Settings → Account → Connect Google Tasks (incremental OAuth; default Sign in stays Calendar + Drive)
- ✅ Offline: no Tasks scope → empty overlay, no error toast unless the Tasks API fails after connect
- ✅ Accessibility: Connect button is a labeled Settings row
- ✅ i18n: English Settings copy only (desktop)

## Smoke scenario

1. Sign in with Google (Calendar + Drive)
2. Connect Google Tasks
3. Due tasks appear as all-day agenda rows

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/services/googleTasks.ts`, `googleTasksMap.ts` |
| View | `apps/desktop/src/settings/SettingsAccount.tsx`, `App.tsx` |
| Tests | `apps/desktop/src/services/googleTasks.test.ts` |
| Wiring | `overlaySync.ts` + Settings Account |

## Tests

- Automated: yes — `apps/desktop/src/services/googleTasks.test.ts`
- Coverage: OAuth connect + due-task overlay mapping

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py feature-gate --stack node`
