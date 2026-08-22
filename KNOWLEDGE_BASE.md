# Knowledge Base

> Repository of stack-specific edge cases, resolved complex bugs, anti-patterns, and reusable project solutions.
> **Do not populate with generic framework definitions.**

## How to use

1. Add entries only after resolving a non-obvious issue specific to this project.
2. Include: symptom, root cause, fix, and prevention.
3. Link to relevant ADRs or PRs when available.

## Entries

### KB-001 — UTF-16 file corruption on Windows

| Field | Detail |
|-------|--------|
| **Symptom** | `check-json` / `npm` / `json.load` fails; git ignore rules stop working; `.gitignore` shows as untracked patterns not applied |
| **Cause** | Cursor `StrReplace` or Windows editor saves text as UTF-16 LE (NUL bytes between ASCII chars) |
| **Fix** | Rewrite affected files with Python `Path.write_text(..., encoding='utf-8')`; re-run `scripts/check-file-encoding.sh` |
| **Prevention** | Bulk edits on Windows via Python/PowerShell UTF-8 write; include root `.gitignore` in encoding scan |
### KB-002 — Invalid `trivy-action@0.28.0` ref

| Field | Detail |
|-------|--------|
| **Symptom** | Security Scan workflow fails at setup: action version not found |
| **Cause** | Bare semver `@0.28.0` is not a valid GitHub Action ref tag |
| **Fix** | Pin to full SHA: `aquasecurity/trivy-action@a9c7b0f06e461e9d4b4d1711f154ee024b8d7ab8 # v0.36.0` |
| **Prevention** | Run `validate-workflow-actions.sh` pre-push; use `check-workflow-action-ref-format.sh` locally |
### KB-003 — `gh api --silent` false CI failures

| Field | Detail |
|-------|--------|
| **Symptom** | `validate-workflow-actions.sh` fails in CI with unknown `gh` flag error |
| **Cause** | `gh api` has no `--silent` flag; stderr not suppressed correctly |
| **Fix** | Redirect to `/dev/null` instead: `gh api ... >/dev/null 2>&1` |
| **Prevention** | Test validation scripts in CI job with `GH_TOKEN`; avoid undocumented `gh` flags |
### KB-004 — Lighthouse performance flake on shared runners

| Field | Detail |
|-------|--------|
| **Symptom** | CI fails with performance 0.88 vs required 0.90 on a single Lighthouse run |
| **Cause** | GitHub-hosted runner CPU variance; single-run assertion is noisy |
| **Fix** | Set `numberOfRuns: 3` in `.lighthouserc.json`; LHCI uses median; keep `minScore: 0.9` |
| **Prevention** | Do not lower performance budget for CI flake; use multi-run median in `modules/web/MODULE.md` |
### KB-005 — Playwright webServer duplicate build

| Field | Detail |
|-------|--------|
| **Symptom** | E2E hangs or serves stale assets; double `vite build` in CI |
| **Cause** | `webServer` runs build while CI already built; wrong host binding |
| **Fix** | Use `vite preview` on `127.0.0.1`; CI runs `npm run build` once before Playwright |
| **Prevention** | Golden Path `examples/web/playwright.config.ts` documents preview-only webServer |
### KB-006 — TypeScript strict null in render handlers

| Field | Detail |
|-------|--------|
| **Symptom** | `tsc` / ESLint error: Object is possibly null inside `render()` callback |
| **Cause** | `strictNullChecks` + `document.getElementById` return type includes null |
| **Fix** | Assign narrowed ref at module scope: `const root = document.getElementById('root')!` or guard once |
| **Prevention** | Module-level `const root = app` pattern in `examples/web/src/main.ts` |
### KB-007 — npm/pip overrides policy for transitive CVEs

| Field | Detail |
|-------|--------|
| **Symptom** | Dependabot or `npm audit` / `uv pip audit` reports CVE in a transitive dependency with no direct upgrade path |
| **Cause** | Parent package pins or bundles a vulnerable sub-dependency; fix not yet published upstream |
| **Fix** | **npm:** add `overrides` in `package.json` to force patched semver (see `examples/web` `@lhci/cli` overrides). **Python:** prefer `uv`/`pip` constraint or bump direct dep; document in DECISION_LOG if override is temporary |
| **Prevention** | Prefer overrides over `--force` installs; remove overrides when upstream ships fix; weekly triage per `docs/SECURITY_TRIAGE.md`; see KB-007 before dismissing Dependabot alerts |
### KB-009 — Release Please `pr` output is JSON, not a PR number

| Field | Detail |
|-------|--------|
| **Symptom** | `release-please.yml` sync step fails: `Error reading JToken from JsonReader` or empty `gh pr checkout` |
| **Cause** | `steps.release.outputs.pr` is empty when `release_created == 'true'` (post-merge push) or stale PR metadata |
| **Fix** | Skip sync when `release_created`; resolve PR number in shell from `PR_JSON` or `gh pr list --head release-please--branches--main` |
| **Prevention** | Never use bare `fromJSON(steps.release.outputs.pr)` in workflow `env:` without a non-empty guard |
### KB-008 — `android-release` APK hash compare policy

| Field | Detail |
|-------|--------|
| **Symptom** | `Android - assembleRelease` fails: APK hashes differ between two clean `assembleRelease` runs on CI |
| **Cause** | Usually a reproducibility regression (non-hermetic timestamp, path, or dependency drift). Rare runner flakes are possible but treated as failures to catch real regressions early |
| **Fix** | Rebuild locally with `SOURCE_DATE_EPOCH=1700000000 ./gradlew clean assembleRelease` twice; compare `sha256sum` of release APK. Align `build.gradle.kts`, `gradle.properties`, and dependency lockfiles with `modules/android/MODULE.md` |
| **Prevention** | Keep `SOURCE_DATE_EPOCH` pinned in CI; use `scripts/verify-reproducible-apk.sh --strict` before release tags. Do not downgrade the job to WARN — strict compare is intentional (M17 P2) |
### KB-010 — Agent shell opens `.sh` files and steals editor focus

| Field | Detail |
|-------|--------|
| **Symptom** | While typing, a `.sh` tab opens and keystrokes land in the wrong file during Cursor Agent work |
| **Cause** | Agent runs `bash scripts/*.sh`; Cursor reveals script paths. `beforeShellExecution` hooks used to run `.sh` wrappers on every shell command |
| **Fix** | Use `python3 scripts/agent-run.py <name> [args]` in agent commands; hooks migrated to `.cursor/hooks/*.py`; workspace `.vscode/settings.json` sets `workbench.editor.autoReveal: false` |
| **Prevention** | Agents follow `.cursor/commands/` and `scripts/agent-run.py`; pin active editor tab; optional `<!-- cursor-hooks: off -->` in `BUILD_PLAN.md` disables hooks entirely |
### KB-011 — Vitest jsdom `localStorage` broken on Node 25+

| Field | Detail |
|-------|--------|
| **Symptom** | `npm test` in `examples/web`: `TypeError: Cannot read properties of undefined (reading 'clear')` or `localStorage.getItem is not a function` |
| **Cause** | Node 25+ enables a global Web Storage stub without `--localstorage-file`; jsdom skips installing real Storage and the stub shadows it |
| **Fix** | Vitest `setupFiles: ["src/test/setup-localStorage.ts"]` installs in-memory Storage when `getItem` is missing |
| **Prevention** | Keep the setup file; do not rely on Node’s experimental `localStorage` in browser-unit tests |
### KB-012 — Cursor hooks fail-open (not a hard guarantee)

| Field | Detail |
|-------|--------|
| **Symptom** | Agent runs `git push` or another denylisted command even though `destructive-ops.mdc` says it is blocked |
| **Cause** | `before_shell_guard.py` and `after_edit_encoding.py` fail-open: parse errors, empty command, missing denylist, or `<!-- cursor-hooks: off -->` in `BUILD_PLAN.md` return allow. `/push` approval of `git push` also matches `git push --force` via substring |
| **Fix** | Treat hooks as **instructed-with-best-effort**. Require `[HUMAN]` or `/push` / `/ship` for destructive-ops. Do not label fail-open hooks as hard denies |
| **Prevention** | Honesty table in `.cursor/rules/destructive-ops.mdc` and `docs/CURSOR_INTEGRATIONS.md`; keep `shell-denylist.txt` in sync with the rule |
### KB-013 — `npm ci` fails after `@puppeteer/browsers` override

| Field | Detail |
|-------|--------|
| **Symptom** | CI `npm ci` in `examples/web`: Missing `proxy-agent@8` from lock file after overriding `@puppeteer/browsers` >=3.2.0 |
| **Cause** | Browsers 3.2.0 optional peer `proxy-agent` >=8.0.1. Local `npm install` on Node 26 can omit that tree; Actions Node 22 `npm ci` requires it |
| **Fix** | Add `"proxy-agent": ">=8.0.2"` to web overrides; run `npm ci` locally before push |
| **Prevention** | After puppeteer/LHCI overrides, verify with `npm ci` (not only `npm install`) |
### KB-021 — Nested `apps/mobile/.git` becomes a gitlink on first monorepo commit

| Field | Detail |
|-------|--------|
| **Symptom** | `git add apps/mobile` warns “embedded git repository”; clones only get a gitlink, not Android sources |
| **Cause** | Fossify tree retained its own `.git` inside the monorepo |
| **Fix** | Move `apps/mobile/.git` aside (e.g. `.mobile-git-bak/`, gitignored), then `git add apps/mobile` so files are normal blobs |
| **Prevention** | On vendor/fork import, strip nested VCS metadata before the first commit; no submodules without `[HUMAN]` approval |
### KB-022 — First Continuum push: OAuth dumps + >500KB PNGs block CI

| Field | Detail |
|-------|--------|
| **Symptom** | Push protection rejects `.cursor/auth-prefs.xml`; Trivy HIGH in Fossify holiday-generator lockfile; hygiene fails on neon/screenshot PNGs >500KB |
| **Cause** | Local Android OAuth prefs accidentally staged; unused Fossify CI deps; marketing assets exceed template size budget |
| **Fix** | Gitignore auth dumps; bump holiday-generator overrides (`js-yaml`/`nanoid`); compress PNGs under 500KB; enable Actions “create PRs” for Release Please |
| **Prevention** | Never stage `.cursor/auth-*`; run `check-large-tracked-files` before first push; treat Fossify workflow deps as in-scope for Trivy |
### KB-023 — Android local delete restored by Drive peer sync

| Field | Detail |
|-------|--------|
| **Symptom** | Deleting a local event (e.g. Doctor) on the phone succeeds, then the row returns in ~2s with a new Room id |
| **Cause** | `buildLocalPayload` sent empty `deletedIds`; desktop/Drive still had the event; merge upserted it |
| **Fix** | Record tombstones before Room delete (`ContinuumLocalEventTombstones`); include them in the peer payload |
| **Prevention** | Never push a local-events snapshot with `deletedIds: []` after a user delete |
### KB-024 — Weekly event conflict badge on an all-day day

| Field | Detail |
|-------|--------|
| **Symptom** | Church on the 16th shows a conflict warning next to an all-day birthday |
| **Cause** | Badges keyed by series `id`; Fossify all-day is midnight–noon (12h), below the 20h heuristic |
| **Fix** | Key by `id` + start; treat midnight–noon ≥12h as non-busy |
| **Prevention** | Do not use repeating event ids alone for per-row UI state |
### KB-025 — Disabled Dependabot alerts fail `pre-release-gate --strict`

| Field | Detail |
|-------|--------|
| **Symptom** | `/ship` hard gate fails: cannot fetch Dependabot alerts, or High alerts appear after enabling |
| **Cause** | New public repo had alerts off; `extract-zip` has no patch; holiday-generator `nanoid` was 3.3.17 |
| **Fix** | `PUT .../vulnerability-alerts`; dismiss unused `extract-zip` (LHCI only); bump `nanoid` to 3.3.18 |
| **Prevention** | Enable Dependabot alerts during repo setup; treat Fossify holiday-generator lockfile as in-scope |
### KB-026 — Looping event titles overlap themselves

| Field | Detail |
|-------|--------|
| **Symptom** | Long agenda titles look like two copies stacked in the same slot |
| **Cause** | Dual-draw ticker cycled by `overflow + gap` (`textW - avail`); gap was smaller than the visible width |
| **Fix** | Cycle by `textWidth + gap`; Settings offers bounce / loop / reset / shrink |
| **Prevention** | Never space a second copy by overflow alone; keep widget on end-ellipsis |
### KB-027 — WindowsApps `python3` hangs Git Bash gates

| Field | Detail |
|-------|--------|
| **Symptom** | `feature-gate.sh` / `watch-agent-gates` stall with no output; `python3 -c` never returns |
| **Cause** | `C:\Users\edwar\AppData\Local\Microsoft\WindowsApps\python3.exe` Store stub is first on PATH |
| **Fix** | Source `scripts/lib/resolve-python.sh` (skips WindowsApps; prefers `py -3`); use `AppData\Local\Python\bin` for `agent-run.py` |
| **Prevention** | Do not rely on `python3` from WindowsApps in Git Bash on this machine |
### KB-028 — Google Continue → “unknown error” for non-test users

| Field | Detail |
|-------|--------|
| **Symptom** | Desktop Sign in: pick account → Continue on unverified warning → Google page “An unknown error has occurred”; no `code` hits loopback |
| **Cause** | OAuth consent is Testing; Google blocks accounts that are not listed as test users. Four sensitive scopes made Continue even more brittle |
| **Fix** | Add the exact Gmail under Audience → Test users; desktop requests Calendar only; native Rust token POST; never set empty `VITE_GOOGLE_CLIENT_SECRET=` in `.env.production` |
| **Prevention** | Confirm header version is 0.17.3+; do not treat this as an app crash until the account is a test user. Do not add `drive.appdata` or `prompt=consent` to desktop sign-in while Publishing status is Testing — Google then shows “Sorry, something went wrong there” after Continue |
### KB-029 — Room `Cannot access database on the main thread` after overlap save

| Field | Detail |
|-------|--------|
| **Symptom** | `EventActivity` crash on save: `IllegalStateException` from Room after tapping OK on overlap |
| **Cause** | `runOnUiThread { finishSaveEvent() }` then `storeEvent()` on UI when the event had no reminders |
| **Fix** | Always `ensureBackgroundThread { storeEvent(...) }` after the UI hop for notification permission |
| **Prevention** | After any `runOnUiThread` hop, do not call Room/DAO on that same stack |
### KB-030 — Pre-bumping `.template-version` makes Release Please skip that tag

| Field | Detail |
|-------|--------|
| **Symptom** | `/ship` prepared 0.18.3; Release Please published **v0.19.0** and never tagged v0.18.3 |
| **Cause** | Manifest was set to 0.18.3 in the prep commit; RP then computed the next MINOR from that baseline |
| **Fix** | Ship the published tag (`v0.19.0`). Attach current APK/EXE to that release |
| **Prevention** | Do not write a future template version into `.release-please-manifest.json` before RP has tagged it |
### KB-031 — Template `check-file-limits.sh` drops the App.tsx exemption

| Field | Detail |
|-------|--------|
| **Symptom** | After copying Canon `check-file-limits.sh` from bootstrap 0.21.0, `/ship` feature-gate fails: `App.tsx` 1668 lines |
| **Cause** | Upstream scan includes all `*.tsx`; Continuum already exempted composition roots (DECISION 2026-08-11) |
| **Fix** | Restore `! -name App.tsx` / `main.tsx` exclusions; keep the 0.21.0 `scripts/lib` 150-line check |
| **Prevention** | When cherry-picking Canon `check-file-limits.sh`, re-apply the composition-root exemption |
### KB-032 — Release Please omits `## [Unreleased]` after fold

| Field | Detail |
|-------|--------|
| **Symptom** | Post-merge `check-changelog-unreleased.sh` fails: Unreleased is not the first heading |
| **Cause** | RP writes `## [0.22.0]` and does not restore an empty Unreleased section |
| **Fix** | Add an empty `## [Unreleased]` above the new version heading immediately after merge |
| **Prevention** | After every RP merge, restore Unreleased *first* and move leftover Added/Changed bullets into the new version section before other post-release docs |
### KB-033 — `chore(release)` prep commit becomes a patch

| Field | Detail |
|-------|--------|
| **Symptom** | `/push` prepares v0.23.0 but Release Please tags **v0.22.1**; GitHub notes list only the chore line |
| **Cause** | Conventional Commits: `chore` is patch (or skipped). Features in the same prep commit do not bump minor |
| **Fix** | Accept the published patch tag; fold real feature bullets into that version in CHANGELOG + `gh release edit` |
| **Prevention** | Land `feat:` commits before the prep commit when a minor is intended; do not name a version RP has not computed |
### KB-034 — Desktop WebView freeze after HMR

| Field | Detail |
|-------|--------|
| **Symptom** | `tauri:dev` window stops accepting clicks after a Year-view or recurrence edit |
| **Cause** | Vite failed to resolve `@fullcalendar/multimonth` (workspace install not hoisted) and `.cc-splash` covered the page with `pointer-events` |
| **Fix** | Install the plugin from the repo root (`-w @continuum/desktop`); set splash `pointer-events: none` |
| **Prevention** | Add new FullCalendar plugins at the workspace root; keep the splash non-interactive |
