# Feature: desktop-nsis-ci

## Acceptance criteria

- ✅ User-visible: N/A (CI)
- ✅ Offline/error: workflow is `workflow_dispatch` only and is not a required check
- ✅ Accessibility: N/A
- ✅ i18n: N/A

## Smoke scenario

1. Actions → Desktop NSIS → Run workflow (optional tag `v1.1.1`)
2. Artifact `continuum-calendar-nsis` contains `Continuum-Calendar-*_x64-setup.exe`

## Container map

| Layer | Path |
|-------|------|
| Logic | `.github/workflows/desktop-nsis.yml` |
| View | N/A (CI) |
| Tests | `apps/desktop/scripts/rename-nsis.py` (local rename helper) |
| Wiring | `docs/CI_REQUIRED_CHECKS.md` informational |

## Tests

- Automated: no — GitHub-hosted Tauri NSIS build is not run in local feature-gate
- Coverage: N/A

## Fallback validation

- Why tests are not feasible: Windows Tauri NSIS needs GitHub-hosted Windows + Rust cache; local gates only lint the workflow YAML
- Command: `python3 scripts/agent-run.py validate-bootstrap -- --quick`

## Definition of Done

See `docs/FEATURE_MODULES.md`.
