# ADR-0001 — Core monorepo architecture

- Status: Accepted
- Date: 2026-08-10

## Context

Continuum Calendar needs a desktop shell (Tauri) and an Android fork (Fossify) that share product concepts (rolling week, empty days, themes, contacts-assisted scheduling) without forcing a single runtime.

## Decision

- Use a **npm workspaces monorepo** with:
  - `apps/desktop` — Tauri 2 + React + Vite + Tailwind + FullCalendar
  - `apps/mobile` — FossifyOrg/Calendar fork (submodule after human approval)
  - `packages/shared` — TypeScript types/contracts for events, OAuth, contacts, theme
- Google integration on desktop via REST (Calendar + People) with documented OAuth scopes.
- Track work in `IMPLEMENTATION_TRACKER.md`, `ROADMAP.md`, and bootstrap `BUILD_PLAN.md`.

## Consequences

- Shared logic that must run on Android will be reimplemented in Kotlin (or later extracted to a portable format); TS shared package is the contract source of truth for desktop.
- Git submodule for mobile is deferred until `[HUMAN]` approval.
