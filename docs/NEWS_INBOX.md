# News Inbox

Auto-collected inbox of news, upstream updates, security fixes, and ideas relevant to this repo, appended daily by News Bot.
A Cursor planning agent should review these items and fold the useful ones into BUILD_PLAN; entries are append-only, newest at the bottom.


## 2026-11-13

**News and updates**

- Rust 1.100.0: Cargo gains `cargo::unused_dependencies` lint and dependency cooldowns (minimum publish age); Windows `CommandExt::show_window` stabilized. [source](https://blog.rust-lang.org/2026/11/12/Rust-1.100.0/)
- Kotlin 2.5.0-RC released (fixes a reified Enum<T> inference regression); Gradle 9.9.1 / 8.14.7 patch releases. [source](https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC)

**Tasks for the planning agent**

- [ ] Bump Tauri toolchain to Rust 1.100 and enable dependency cooldown — Supply-chain hardening for the desktop app. Label: `[AGENT]` · Priority: P3 · https://blog.rust-lang.org/2026/11/12/Rust-1.100.0/
- [ ] Plan Kotlin upgrade path (2.3 -> 2.5) and Gradle 9.9.1 — Android side is two minors behind the incoming 2.5. Label: `[AGENT]` · Priority: P3 · https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC

## 2026-11-14

**News and updates**

- wfx-loot campaign pushed a CI secret stealer into 17,000+ public repos using stolen GitHub credentials since Nov 10; an npm postinstall hook slipped past GitHub's workflow hold. IOCs: wfx-docs branch, .ci-xxxxxx.sh files, build-audit-*.yml workflows. [source](https://blog.gitguardian.com/wfx-loot-a-ci-secret-stealer-pushed-with-stolen-github-credentials/)

**Tasks for the planning agent**

- [x] Audit the repo for wfx-loot IOCs — Checks for the wfx-docs branch/PR, .ci-*.sh files, build-audit-*.yml workflows and new npm lifecycle hooks, since the campaign steals CI secrets and Cursor credentials. Label: `[AGENT]` · Priority: P1 · https://blog.gitguardian.com/wfx-loot-a-ci-secret-stealer-pushed-with-stolen-github-credentials/ (synced to BUILD_PLAN)

## 2026-11-18

Catch-up for Nov 17–18 (the daily brief did not run those mornings).

**News and updates**

- Compose Multiplatform 1.13.0-rc01 is out (font-cache leak, popup/tooltip crash, iOS per-frame bitmap cost). This repo's KMP track is still on 1.7.3. [source](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-rc01)

**Tasks for the planning agent**

- [x] Plan a Compose Multiplatform upgrade from 1.7.3 toward 1.13.0-rc01 — The parallel kmp/ track is several minors behind, and rc01 fixes a font-state leak and a popup disposal crash. Label: `[AGENT]` · Priority: P2 · https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.13.0-rc01 (synced to BUILD_PLAN)

## 2026-11-20

**News and updates**

- Gradle 9.9.2 and 8.14.8 (Nov 19) fix four High-severity build-tool bugs: SFTP repositories didn't verify host keys, transitive snapshot dependencies skipped dependency verification, TestKit used a predictable temp directory, and the daemon registry was briefly readable by other local users. 9.9.2 also fixes a 9.9.0 plugin-resolution regression with registries that answer 401. Wrappers older than 9.9.1/8.14.7 also miss the earlier Critical fix (version-catalog values run as code). [9.9.2 notes](https://docs.gradle.org/9.9.2/release-notes.html) · [8.14.8 notes](https://docs.gradle.org/8.14.8/release-notes.html)
- Android developer verification goes global from January 2027 and will cover all install sources on certified devices: it starts with Play and six other distributors, then expands country by country over several months. Developers who ship only outside Play are told to create an Android Developer Console account and register package names plus signing-key SHA-256 fingerprints (a new open-source guide covers re-signing repos such as F-Droid). Unregistered apps stay installable only through a one-time "advanced flow" on each device; hobbyist accounts now allow 50 devices. F-Droid publicly opposes the scheme. [Android blog](https://developer.android.com/blog/posts/android-developer-verification-2027) · [open-source registration guide](https://developer.android.com/developer-verification/guides/open-source-app-registration) · [F-Droid open letter](https://f-droid.org/2026/02/24/open-letter-opposing-developer-verification.html)
- Tauri GHSA-w28w-mhc8-qvjv (High, Sep 26): in tauri ≤2.11.5 the internal channel-fetch IPC command skips the ACL check and serves queued large channel responses by a sequential id, so any WebView that can call invoke can read (and drain) payloads meant for another WebView. Fixed in 2.11.6 (latest 2.12.7). It's a repository-only advisory missing from the Global Advisory DB, so Dependabot raises no alert. This repo's Cargo.lock resolves tauri 2.11.5; exposure is low because the app loads only its own local UI in one webview. [advisory](https://github.com/tauri-apps/tauri/security/advisories/GHSA-w28w-mhc8-qvjv) This repo's dependabot.yml also has no cargo entry for apps/desktop/src-tauri.

**Tasks for the planning agent**

- [x] Bump the Gradle wrapper 9.6.1 (apps/mobile), 9.7.1 (kmp), 9.9.0 (examples/android) to 9.9.2 and commit the regenerated wrapper jar and distributionSha256Sum — the patch fixes four High build-tool vulnerabilities (SFTP host keys, snapshot dependency verification, TestKit temp dir, daemon registry). It also picks up the 9.3 Critical version-catalog code-execution fix this wrapper lacks. Run ./gradlew wrapper --gradle-version=<ver> in each wrapper (apps/mobile/gradle/wrapper/, kmp/gradle/wrapper/, examples/android/gradle/wrapper/). Label: `[AGENT]` · Priority: P2 · https://docs.gradle.org/9.9.2/release-notes.html (synced to BUILD_PLAN)
- [ ] Decide whether to verify your identity and register this app with Google before January 2027 — if yes, create an Android Developer Console account and register the package name(s) with the SHA-256 of the GitHub Releases signing key (plus F-Droid's key if F-Droid re-signs it); if no, add an "advanced flow" install note to the README. The app is sideloaded (GitHub Releases/Obtainium/F-Droid), exactly the installs the rollout gates, and this is a personal identity/policy call. Label: `[HUMAN]` · Priority: P2 · https://developer.android.com/blog/posts/android-developer-verification-2027 (inbox only: no /build-parsed human group on this board)
- [x] Bump tauri to 2.11.6 or later (latest 2.12.7) in apps/desktop/src-tauri Cargo.toml/Cargo.lock, smoke the desktop app, and add a cargo entry for apps/desktop/src-tauri to .github/dependabot.yml — the High-severity IPC ACL bypass affects the locked 2.11.5, and Dependabot can't see it (or any other Rust update here). Label: `[AGENT]` · Priority: P2 · https://github.com/tauri-apps/tauri/security/advisories/GHSA-w28w-mhc8-qvjv (synced to BUILD_PLAN)

## 2026-11-25

**News and updates**

- Better Stack compared the same desktop chat app built in GPUI (the Rust, GPU-drawn UI framework Discord is moving its desktop app to), Tauri, and Electron, looking at memory, app size, frame rate and CPU. Useful background if this repo is choosing or revisiting a desktop shell; I did not run their comparison. [GPUI](https://gpui.rs) · [Better Stack](https://www.youtube.com/shorts/PbNB0MHO0eA)

## 2026-11-26

**News and updates**

- Kotlin 2.5.0-RC2 (Nov 25) fixes Compose issues: a Kotlin/Native crash where a lambda SAM-converted to a `@Composable` fun interface got no restart group (KT-91414), a cross-module `@JvmField` composable-lambda NoSuchFieldError (KT-91591), and a `Float.fromBits` slowdown (KT-91549). Stable is still 2.4.21 and 2.5.0 final looks close, so this is a heads-up to test on a branch, not to ship. [2.5.0-RC2](https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC2) The kmp/ track pins Kotlin 2.1.21.

- Vite 8.4.0 (Nov 26) adds an opt-in `server.fs.strict: 'extended'` for wider dev-server file-access checks, plugin names in hook errors, worker chunks in the manifest, CSS preload retries and compressed wasm in preview. This repo is on `^8.3.x`, so Dependabot will bring it in; no action needed. [v8.4.0](https://github.com/vitejs/vite/releases/tag/v8.4.0) · [support policy](https://vite.dev/releases)

- Conductor moved its agent-workspace desktop app from Tauri to Electron (0.95.0, Nov 16; numbers posted Nov 24), keeping its Rust local server: workspaces open 71% faster, startup-to-render is 4.6x faster and RAM is 35% lower, while the download grew from about 200 MB to about 500 MB. A real-world counterpoint to "Tauri is always lighter": measure cold start and RAM on the real WebView (WebKitGTK on Linux, WebView2 on Windows) rather than assume. [Conductor 0.95.0](https://www.conductor.build/changelog/0.95.0-agents-that-manage-your-local-workspaces) · [X post](https://x.com/charlieholtz/status/2125351294965805147)

**Tasks for the planning agent**

- [ ] Smoke-test kmp/ on Kotlin 2.5.0-RC2 in a branch — The same K/N Compose fixes apply to the KMP track. Label: `[AGENT]` · Priority: P3 · https://github.com/JetBrains/kotlin/releases/tag/v2.5.0-RC2

## 2026-11-28

**News and updates**

- Gradle [9.9.3](https://github.com/gradle/gradle/releases/tag/v9.9.3) (Nov 27) is a regression-only patch: 9.9.2 failed to start on Windows when GRADLE_USER_HOME sits on an ImDisk RAM disk ([gradle#39909](https://github.com/gradle/gradle/issues/39909)). No new security fixes; 9.9.2's four High fixes are still why the open wrapper-bump row is P2, and Gradle now recommends 9.9.3, so landing on it avoids a second bump. The board row is unchanged. [Release notes](https://docs.gradle.org/9.9.3/release-notes.html)

**Tasks for the planning agent**

- [ ] Use Gradle 9.9.3 rather than 9.9.2 when doing the open wrapper bump — 9.9.3 has the same security fixes plus a Windows startup regression fix, so one bump covers both. Label: `[AGENT]` · Priority: P3 · https://github.com/gradle/gradle/releases/tag/v9.9.3 (BUILD_PLAN row left unchanged)
