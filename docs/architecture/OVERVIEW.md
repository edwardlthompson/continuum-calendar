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
apps/desktop/     Tauri + React + Vite + Tailwind + FullCalendar
apps/mobile/      FossifyOrg/Calendar fork notes / submodule target
packages/shared/  Shared TS types (events, OAuth, contacts, theme)
docs/             Agent docs, API setup, architecture, widget specs
```

## Desktop

- UI: React + TypeScript + Vite + Tailwind (`class` dark mode via `ThemeProvider`).
- Grid: FullCalendar `timeGrid` custom view `rollingWeek` (7 days from today).
- Google: REST Calendar v3 + People API (`apps/desktop/src/services/googleApi.ts`).
- Shell: Tauri 2 (`apps/desktop/src-tauri`).

## Mobile

See [MOBILE_FOSSIFY_FORK.md](./MOBILE_FOSSIFY_FORK.md) and [ANDROID_WIDGET_SPEC.md](./ANDROID_WIDGET_SPEC.md).

## Trust boundaries

| Boundary | Notes |
|----------|-------|
| Desktop ↔ Google APIs | OAuth bearer tokens; HTTPS only |
| Desktop OS | Tauri secure storage (planned) for refresh tokens |
| Android ↔ System Calendar/Contacts providers | Runtime permissions; no Play Services required for core FOSS path |
