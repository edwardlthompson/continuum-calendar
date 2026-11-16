# ADR-0006 — Compose Multiplatform parallel track

- Status: Accepted
- Date: 2026-11-15
- Deciders: Continuum Calendar maintainers

## Context

Dual shipping UIs (Tauri/React desktop + Fossify GPL Android Views) duplicated Continuum domain logic across TypeScript \packages/shared\ and Kotlin. Product goal: Android, iOS, and desktop from one KMP + Compose codebase without breaking current releases.

## Decision

Adopt a **parallel MIT Gradle tree** under \kmp/\ (continuum-domain, continuum-platform, continuum-data, continuum-ui, androidApp, desktopApp, iosApp). Keep Tauri and Fossify shipping. Prefer REST-first CMP Android, SQLDelight for local store, golden fixtures vs TS contracts, and CI via \.github/workflows/kmp.yml\.

## Alternatives considered

- Rewrite Fossify in place (GPL contamination risk) — rejected
- WASM TypeScript bridge for mobile — poor native UX — rejected
- Big-bang delete of shipping apps — too risky — rejected

## Consequences

- Two Android package IDs until parity (\org.continuumcalendar.app\ vs \.cmp\)
- iOS link/sign remains \[HUMAN]\ on macOS
- Dual-maintain domain until desktop migrates off TypeScript
- Deferred P2 (hotkeys, ICS/CalDAV, holidays, Tasks, free-slots) lands in \kmp/\ first

### Critique

| Issue | Resolution |
|----|---|
| Null/empty at boundary | Settings/event JSON codecs + SQLDelight schema; tests in continuum-data/commonTest |
| Network timeout | Google REST clients use explicit HTTP failures; UI surfaces retry/sign-in |
| Race conditions | SyncCoordinator serializes peer Drive overlays; vault writes stay single-writer |
| Unhandled exceptions | DesktopCrashCapture + typed Result paths in fetch/write helpers |
