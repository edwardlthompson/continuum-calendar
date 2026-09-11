# Feature: desktop-appimage-ci

## Acceptance criteria

- ✅ User-visible: N/A (CI)
- ✅ Offline/error: workflow is `workflow_dispatch` only and is not a required check
- ✅ Accessibility: N/A
- ✅ i18n: N/A

## Smoke scenario

1. Actions → Desktop AppImage → Run workflow
2. Artifact `continuum-calendar-appimage` contains `Continuum-Calendar-*-x86_64.AppImage`

## Container map

| Layer | Path |
|-------|------|
| Logic | `.github/workflows/desktop-appimage.yml` |
| View | N/A (CI) |
| Tests | N/A — GitHub-hosted Tauri build |
| Wiring | `docs/CI_REQUIRED_CHECKS.md` informational |

## Tests

- Automated: no — GitHub-hosted Tauri build is not run in local feature-gate
- Coverage: N/A

## Fallback validation

- Why tests are not feasible: Ubuntu Tauri AppImage build needs GitHub-hosted Linux + Rust cache; local gates only lint the workflow YAML
- Command: `python3 scripts/agent-run.py validate-bootstrap -- --quick`
