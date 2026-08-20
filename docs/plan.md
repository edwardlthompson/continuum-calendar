# Implementation Plan

> First-milestone stub. Active work lives in `BUILD_PLAN.md`.
> Status: 🔲 open · ✅ done · ❌ blocked.

## Milestone — Template standards (v0.21.0)

| Task | Owner | Tests / fallback |
|------|-------|------------------|
| ✅ Sync Canon commands, help, and scripts from agent-project-bootstrap v0.21.0 | AGENT | `validate-bootstrap.sh --quick` |
| ✅ Add SDD stubs (`docs/spec.md`, this file) | AGENT | REQUIRED list in validate-bootstrap |
| 🔲 Public OAuth consent / test users | HUMAN | Manual Google Cloud console |

## Next feature

1. Copy `docs/features/_template.md` → `docs/features/{name}.md`
2. Lock the public API (Sequential)
3. Add unit tests before or with the implementation
4. Run `python3 scripts/agent-run.py watch-agent-gates --once --autofix`

If automated tests are not feasible, write the justification and fallback command in the feature spec before marking the BUILD_PLAN row ✅.
