# Decision Log

> Chronological register of major technical trade-offs, accepted architectures, and rejected alternatives.
> **Treat past entries as immutable history; append only.**

## Format

```markdown
### YYYY-MM-DD — [Title]
- **Status:** Accepted | Rejected | Superseded
- **Context:** ...
- **Decision:** ...
- **Alternatives considered:** ...
- **Consequences:** ...
```

## Entries

_Seed template ADR: `docs/adr/0000-template-baseline.md`. Child repos use `docs/adr/0001-core-architecture.md`._

### 2026-08-12 — Public GitHub home continuum-calendar
- **Status:** Accepted
- **Context:** Local tree had no commits and only a `template` remote; `/ship` could not push. Product checklist still pointed at the bootstrap template repo.
- **Decision:** Create public `edwardlthompson/continuum-calendar`, initial-commit the monorepo, set `origin`, push `main`. Keep `template` remote for upstream bootstrap pulls.
- **Alternatives considered:** Rename/repurpose `agent-project-bootstrap` (rejected — template stays distinct); private repo (rejected — FOSS public home)
- **Consequences:** CI/Dependabot/Release Please attach to the new origin; OAuth redirect/package docs should cite this repo URL

### 2026-08-11 — Package id org.continuumcalendar.app
- **Status:** Accepted
- **Context:** Public listing and OAuth require a Continuum applicationId; full Kotlin package move of the Fossify fork is high-risk
- **Decision:** `APP_ID=org.continuumcalendar.app` with debug suffix `.debug`; keep `SOURCE_NAMESPACE=org.fossify.calendar` for AGP/R/imports; Continuum mark replaces Fossify launcher/Tauri icons
- **Alternatives considered:** Rename entire `org.fossify.calendar` source tree (deferred — large blast radius); keep Fossify applicationId (rejected for store identity)
- **Consequences:** Debug install id becomes `org.continuumcalendar.app.debug`; [HUMAN] must create a matching Android OAuth client + SHA-1 before peer sync on new installs

### 2026-08-11 — Audit A1 gate and auth durability fixes
- **Status:** Accepted
- **Context:** `/audit` found encoding gate scanning Android `app/build` intermediates; expired Google access tokens broke peer sync; desktop reminder keys survived reloads without timers
- **Decision:** Skip `build`/`.gradle`/`.cxx`/`__pycache__` in `check-file-encoding.py`; implement Android `ensureFreshTokens`; rebuild desktop reminder timers with a serialize chain; use local agenda day keys; exempt composition-root `App.tsx` from 300-line static-data scan
- **Alternatives considered:** Delete build trees only (insufficient); leave refresh as HUMAN-only (rejected — workers already had refresh tokens)
- **Consequences:** Local gates green after restoring example deps post-purge; [HUMAN] still owns public OAuth client / no client secret in bundles and secure token storage

### 2026-08-10 — Continuum Calendar monorepo bootstrap
- **Status:** Accepted
- **Context:** Need desktop (Tauri) + Android (Fossify fork) sharing product contracts without a single runtime
- **Decision:** npm workspaces monorepo with `apps/desktop`, `apps/mobile` (docs-first), `packages/shared`; Google REST Calendar + Contacts scopes; FullCalendar rolling week prototype
- **Alternatives considered:** Electron (heavier); pure PWA-only (no native widget path); immediate Fossify submodule (rejected pending HUMAN approval)
- **Consequences:** Kotlin reimplements free-slot/widget logic; desktop is contract source of truth in TypeScript

### 2026-08-10 — Ship v0.16.0 (/ship)
- **Status:** Accepted
- **Context:** Need third-party review + broader autofix before release; `/ship` should stay one command
- **Decision:** Codex read-only reviewer (opt-in CI + `/codex-review`) feeds `CODE_REVIEW.md` → Cursor `/fix`; expand `/prerelease` with multi-stack autofix; merge Release Please #51 to **v0.16.0**
- **Alternatives considered:** Codex writes patches in CI (rejected: destructive-ops / FOSS spend control); chain Codex into every `/maintain` (rejected: API cost)
- **Consequences:** `/ship` runs autofix + optional Codex + hard gate; enable Codex CI by copying workflow example + `OPENAI_API_KEY`

### 2026-08-01 — Ship v0.15.2 (/ship)
- **Status:** Accepted
- **Context:** Plan Mode left risks as open questions; Dependabot High blocked pre-release (js-yaml, then postcss)
- **Decision:** Require Issue→Resolution Critique in always-applied rules + `/plan`; override patched npm transitive CVEs; merge Release Please #50 to **v0.15.2**
- **Alternatives considered:** Soft "list risks" Critique (rejected: humans still had to chase resolutions); defer brace-expansion/postcss (rejected: pre-release gate requires zero Critical/High)
- **Consequences:** Agents must bake mitigations into plan todos; template at 0.15.2 with SBOM release assets

### 2026-07-22 — Ship v0.15.0 (/ship)
- **Status:** Accepted
- **Context:** `/ship` after M33 + local-first compute; first CI failed on duplicate `## [Unreleased]`; web tests failed on Node 25+ localStorage stub
- **Decision:** Polyfill Storage in vitest setup (KB-011); collapse stale Unreleased; merge Release Please #37 to **v0.15.0**
- **Alternatives considered:** `--no-webstorage` only (rejected: may break older Node CI); leave duplicate Unreleased (rejected: gate hard-fail)
- **Consequences:** Template at 0.15.0 with Cursor worktrees/permissions/skills/plugin pack and local-first parallelism

### 2026-07-21 — Local-first compute on This Computer
- **Status:** Accepted
- **Context:** Agents defaulted toward serial work or Cloud handoff even when the desktop has many cores
- **Decision:** Ship `local-compute.mdc` + sessionStart CPU reminder; parallelize independent `validate-bootstrap` checks via `run_checks_parallel.py` (`BOOTSTRAP_CHECK_JOBS`); pytest-xdist `-n auto`; Gradle `--parallel`; document `/scope` + worktrees/`/best-of-n` as the local default over Cloud Agents
- **Alternatives considered:** Always Cloud Agents for parallelism (rejected: wastes local hardware and costs credits); unbounded bash `&` in validate-bootstrap (rejected: harder error aggregation on Windows)
- **Consequences:** Quick bootstrap checks use all cores (e.g. jobs=CPU count); agents are steered to concurrent Task/worktrees when local

### 2026-07-21 — Cursor 3.9–3.11 FOSS integration (M33)
- **Status:** Accepted
- **Context:** Cursor added native worktrees setup, Auto-review `permissions.json`, Skills direction, CLI/GHA, side chats, Design Mode, cloud conversation hooks, Automations, and plugin packaging; registry lagged at 2026-06-30
- **Decision:** Ship FOSS live `worktrees.json` + fail-soft OS setup, committed `permissions.json` (dual layer with hooks), four new skills + checker atomic update, CLI workflow under `.github/workflow-examples/` (never auto-run), plugin via pack-to-`dist/cursor-plugin` (no repo-root symlink); keep commercial as examples (cloud hooks, Automations recipes, Bugbot Autofix map)
- **Alternatives considered:** Custom plugin paths into `.cursor/` (rejected: discovery risk); whole-repo plugin symlink (rejected: double-load); `.example.yml` under `workflows/` (rejected: GHA may load it); weaken shell hook for Auto-review (rejected: hooks stay hard FOSS enforcement)
- **Consequences:** `check-cursor-integrations` requires seven skills + worktrees/permissions; `/best-of-n` documented beside parallel-lock worktrees; Cloud Agents still ignore Run Modes

### 2026-07-12 — Pre-release gate Dependabot counter + FOSS MCP check
- **Status:** Accepted
- **Context:** `/push` pre-release `--strict` failed: Dependabot alerts API used unsupported `page=` form; FOSS integrations check failed whenever gitignored `.cursor/mcp.json` existed locally
- **Decision:** Count alerts via `gh api --paginate` query string; treat live `mcp.json` as OK unless `git ls-files` shows it tracked; multi-stack `--strict` skips missing optional toolchains
- **Alternatives considered:** Require `security_events` refresh always (rejected: false failures blocked release); ban local MCP (rejected: contradicts CURSOR_INTEGRATIONS activation)
- **Consequences:** Maintainer gates pass with local MCP enabled; Release Please #36 published v0.14.1

### 2026-07-12 — Dependabot automerge CI gap (M32)
- **Status:** Accepted
- **Context:** Merges via `GITHUB_TOKEN` (`app/github-actions`) do not start `push` workflows; `main` tip after Dependabot merges had zero CI runs; weekly health failed waiting for missing runs
- **Decision:** Prefer optional `AUTOMERGE_TOKEN` PAT for Dependabot/Release Please merge; add `workflow_dispatch` to CodeQL + Security Scan; `check-github-ci.sh --dispatch-if-missing` (weekly health uses it with `actions: write`); prefer Git Bash in `agent-run.py` on Windows
- **Alternatives considered:** Require PAT only (rejected: blocks FOSS template without secrets); SHA-pin all actions for Scorecard (deferred: conflicts with documented `@vX.Y.Z` policy)
- **Consequences:** Weekly health can self-heal missing runs; post-merge CI still needs HUMAN required-status-checks + optional PAT for true push triggers

### 2026-07-02 — Quiet agent shell (hooks Python + agent-run)
- **Status:** Accepted
- **Context:** Cursor Agent shell execution opened `.sh` hook and script tabs, stealing editor focus while users typed
- **Decision:** Migrate hooks to Python; add `scripts/agent-run.py` for agent gate invocations; ship `.vscode/settings.json` anti-reveal defaults; document KB-010
- **Alternatives considered:** Disable hooks globally (rejected: loses destructive-op guard); rewrite all scripts to PowerShell (rejected: scope); `pythonw.exe` for hooks (rejected: breaks stdout JSON)
- **Consequences:** Agent-facing commands no longer contain `.sh` paths; underlying bash scripts unchanged for CI/humans

### 2026-07-01 — Cursor hook smoke isolation (M31)
- **Status:** Accepted
- **Context:** M31 audit found `check-cursor-hooks.sh --smoke` false-pass when `.cursor-session-state.json` already listed `git push` in `destructive_ops_approved`
- **Decision:** Smoke test clears session approvals before deny assertion; validate hook scripts require shebang on line 1
- **Alternatives considered:** Ignore local session state in smoke (rejected: hides real deny-path bugs); require empty session file (rejected: breaks dev workflow)
- **Consequences:** `--smoke` is deterministic in CI and locally; invalid hook scripts fail validate-bootstrap early

### 2026-06-30 — Cursor hooks as enforcement layer (M30)
- **Status:** Accepted
- **Context:** M27 rejected `beforeSubmitPrompt` hooks; rules alone cannot block destructive shell commands at runtime
- **Decision:** Ship FOSS-safe project hooks (`beforeShellExecution`, `afterFileEdit`, `subagentStart`, `sessionStart`, `beforeMCPExecution`); fail-open guards; session `destructive_ops_approved` for `/push`/`/ship`; opt-out via `<!-- cursor-hooks: off -->`
- **Alternatives considered:** Prompt-rewrite hooks (rejected per M27); broad shell blocklists (rejected: blocks legitimate agent work)
- **Consequences:** `check-cursor-hooks.sh --smoke` in validate-bootstrap; complements `destructive-ops.mdc` without token bloat

### 2026-06-20 — Repo-wide checklist status markers
- **Status:** Accepted
- **Context:** BUILD_PLAN and scattered checklists used mixed ⬜ / `- [ ]` / ✅ formats; inconsistent in Markdown Preview vs source
- **Decision:** Standardize on 🔲 open · ✅ done · ❌ blocked emoji markers repo-wide; document in `BUILD_PLAN.md` legend and agent read order
- **Alternatives considered:** GitHub `- [ ]` task lists (rejected: poor Preview readability and agent parsing); keep ⬜ white square (rejected: visually similar to ✅ in some fonts)
- **Consequences:** All new checklist rows use emoji; `agent-progress.sh` accepts legacy ⬜ for child repos during transition

### 2026-06-18 — Release automation hardening (M29)
- **Status:** Accepted
- **Context:** v0.11.0 release lacked SBOM assets (GITHUB_TOKEN cannot chain `release` → `release.yml`); Release Please skipped `extra-files`; `health-check.yml` registered as path name caused 0-job push failures
- **Decision:** `release-please.yml` runs `sync-template-version.sh` on release PR branches and dispatches `release.yml` on `release_created`; rename workflow to `weekly-health-check.yml`; fix sync script for Windows Git Bash
- **Alternatives considered:** PAT with workflow scope for release chaining (rejected: secrets management); manual SBOM backfill only (rejected: repeated human step each release)
- **Consequences:** Release Please needs `actions: write`; future releases should ship SBOM assets without manual dispatch

### 2026-06-17 — Batch instruction templates (M27)
- **Status:** Accepted
- **Context:** Agents and child-repo owners needed repeatable shortcuts for bootstrap, verify, build, ship, and maintenance workflows without re-pasting long prompts
- **Decision:** Ship 25 slash commands in `.cursor/commands/` (20 atomic + 5 super), bare-word expansion via `batch-commands.mdc`, human cheat sheet at `docs/help/BATCH_COMMANDS.md`, registry at `docs/BATCH_COMMANDS.md`; `/push` and `/ship` grant explicit push approval
- **Alternatives considered:** `beforeSubmitPrompt` hook for bare words (rejected: Cursor API cannot rewrite prompts); single mega-doc for humans and agents (rejected: overwhelms first-time users)
- **Consequences:** `alwaysApply` rule adds ~25 lines per session; `check-batch-commands.sh` prevents registry drift; child repos cherry-pick via `UPGRADING_FROM_TEMPLATE.md`

### 2026-06-30 — Autonomous /build with grouped human section
- **Status:** Accepted
- **Context:** `/build` halted on HUMAN/ADB rows; humans needed a single review block after automation; child repos need scripted attempts before manual follow-up
- **Decision:** Add `build-sprint-status.sh`, `attempt-build-plan-row.sh`, and `HUMAN_BACKLOG.md` (failure-only); restructure BUILD_PLAN with `#### Human & device (after automation)`; AGENT/AUTO runs first, then automation attempts on grouped human rows
- **Alternatives considered:** Skip human rows entirely during /build (rejected: loses automation catalog value); keep human rows interleaved in Sequential (rejected: hard to review after automation)
- **Consequences:** Child repos must place HUMAN/ADB rows in the grouped section; `<!-- no-auto-approve -->` disables autonomous ADR ack

### 2026-06-13 — @lhci/cli npm overrides for transitive CVEs
- **Status:** Accepted
- **Context:** Lighthouse CI (`@lhci/cli`) bundles transitive dependencies (`tmp`, `uuid`) with known CVEs; no patched `@lhci/cli` release available at triage time
- **Decision:** Add npm `overrides` in `examples/web/package.json` forcing `tmp >= 0.2.6` and `uuid >= 11.1.1`; document in KB-007
- **Alternatives considered:** Dismiss Dependabot alert (rejected: hides real risk); remove Lighthouse CI job (rejected: loses performance gate)
- **Consequences:** Lockfile must be regenerated after override changes; overrides should be removed when `@lhci/cli` ships fixed dependencies

### 2026-06-13 — Ship all optional ecosystem modules (M3)
- **Status:** Accepted
- **Context:** Sprint M3 asked whether to ship Lightroom, Rust, and Go optional modules in the template maintainer repo
- **Decision:** Ship all three with Golden Path stubs, MODULE.md guides, and path-gated CI jobs (`lightroom`, `rust`, `go`) that skip when child repos remove the directories
- **Alternatives considered:** Lightroom-only (rejected: Rust/Go stubs are low-cost and popular); defer all optional modules (rejected: COMPLETED_TASKS M3 work already landed)
- **Consequences:** Template CI runs more jobs on `main`; child repos can delete unused `examples/` folders to skip jobs via `hashFiles` guards

### 2026-08-14 — Android local-delete tombstones
- **Status:** Accepted
- **Context:** Deleting local peer events on Android (e.g. Doctor / `mock-3`) vanished for ~2s then Drive merge restored them because `buildLocalPayload` sent empty `deletedIds`.
- **Decision:** Persist tombstones in `continuum_local_events` prefs, record them before Room delete, include them in the peer payload, and merge them on pull.
- **Alternatives considered:** Delete-only on Android without Drive tombstones (rejected — desktop copy always wins); wait for desktop-only delete (rejected — phone is the source of truth for that smoke).
- **Consequences:** Desktop Refresh / next peer poll drops the event; pre-upgrade deletes may need one more delete after install.

### 2026-08-14 — Conflict warnings keyed by occurrence
- **Status:** Accepted
- **Context:** Weekly Church inherited a conflict badge on Aug 16 next to an all-day birthday because badges used series `id`, and Fossify all-day is midnight–noon (12h) which missed the 20h heuristic.
- **Decision:** Key warnings by `id` + start; treat midnight–noon ≥12h blocks as non-busy on Android and in `@continuum/shared`.
- **Alternatives considered:** Keep id-only badges (rejected — false positives every Sunday); raise the duration cutoff to 12h without midnight check (rejected — would hide real half-day meetings).
- **Consequences:** Same-day timed overlaps still warn; repeating events only warn on the occurrence that overlaps.

### 2026-08-14 — Conflict day-blocks are fleet-wide (shared + every Android surface)
- **Status:** Accepted
- **Context:** The Aug 16 occurrence-key fix was on `main` and in the v0.16.2 APK, but week view still used series-id collisions, day view had no Continuum badges, and VERSION_CODE stayed 21 so a 1.10.3 device could not tell it was stale.
- **Decision:** One `isTimedBusyEvent` rule (midnight → local noon or later, or ≥20h) in `@continuum/shared` and `ContinuumConflict`; week/day/agenda/widget all use occurrence times; bump Android to 1.10.4 / 22.
- **Alternatives considered:** Keep 12h-duration-only heuristic (rejected — DST and Fossify noon); leave VERSION_CODE 21 (rejected — other phones keep the old APK).
- **Consequences:** Install 1.10.4 on every device; same-day timed overlaps still warn.

### 2026-08-14 — Dependabot High: extract-zip and nanoid
- **Status:** Accepted
- **Context:** Enabling Dependabot alerts on `continuum-calendar` surfaced High `extract-zip` (CVE-2026-56876, no patch) and `nanoid` < 3.3.18.
- **Decision:** Dismiss `extract-zip` as `not_used` (LHCI / `@puppeteer/browsers` in `examples/web` only). Bump holiday-generator `nanoid` override to 3.3.18.
- **Alternatives considered:** Remove `@lhci/cli` (rejected — loses Lighthouse gate); wait for an extract-zip release (none expected).
- **Consequences:** Revisit extract-zip if LHCI drops the transitive; nanoid alert closes after the lockfile lands on `main`.

