# Architecture Overview

Continuum Calendar is a FOSS monorepo for a cross-platform calendar ecosystem.

## Goals

1. **Rolling week view** — multi-day grids with **Today as column 1**.
2. **Explicit empty days** — agenda/schedule/widgets never collapse blank days.
3. **Google sync** — Calendar API CRUD + Contacts readonly for attendees.
4. **Themes** — Light, Dark, and System across desktop and mobile.
5. **Android widget** — rolling days + open gaps on the homescreen.

## Layout

```text
apps/desktop/     Shipping: Tauri + React + Vite + Tailwind + FullCalendar
apps/mobile/      Shipping: Fossify Views fork (GPL-3)
packages/shared/  Shared TS types (desktop contract; golden fixtures for KMP)
kmp/              Parallel MIT Compose Multiplatform track (ADR-0006)
docs/             Agent docs, API setup, architecture, widget specs

```

## KMP parallel track (`kmp/`)

MIT modules: `continuum-domain` (pure logic), `continuum-platform` (ports),
`continuum-data` (SQLDelight + sync), `continuum-ui` (Compose), plus
`androidApp` / `desktopApp` / `iosApp`. Does not replace shipping apps until
FR-1..3 + peer sync parity. See [ADR-0006](../adr/0006-compose-multiplatform.md).

## Desktop

- UI: React + TypeScript + Vite + Tailwind (`class` dark mode via `ThemeProvider`).
- Grid: FullCalendar `timeGrid` custom view `rollingWeek` (7 days from today).
- Google: REST Calendar v3 + People API (`apps/desktop/src/services/googleApi.ts`).
- Shell: Tauri 2 (`apps/desktop/src-tauri`).

## Mobile

See [MOBILE_FOSSIFY_FORK.md](../MOBILE_FOSSIFY_FORK.md) and [ANDROID_WIDGET_SPEC.md](../ANDROID_WIDGET_SPEC.md).

## Trust boundaries

| Boundary | Notes |
|----------|-------|
| Desktop ↔ Google APIs | OAuth bearer tokens; HTTPS only |
| Desktop OS | Tauri secure storage (planned) for refresh tokens |
| Android ↔ System Calendar/Contacts providers | Runtime permissions; no Play Services required for core FOSS path |
