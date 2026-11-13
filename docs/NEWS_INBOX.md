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
