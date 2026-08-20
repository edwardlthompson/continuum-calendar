# Android Homescreen Widget — Design Spec

## Purpose

Show a **rolling multi-day strip** and/or **agenda list with open days** on the Android homescreen so empty days are never omitted.

## Agenda open-days widget (priority)

Extends Fossify list widget:

- When `show_empty_days_in_agenda` is on (default), blank days render as an **Open** row (title from `R.string.continuum_open_day`).
- Built via `AgendaEmptyDays.fill` in `EventListWidgetAdapter.onDataSetChanged`.
- Configure preview uses the same fill so empty days appear as **Open**.
- TalkBack: Open rows use `accessibility_open_day`; header icons use `accessibility_widget_*`.
- Optional longest free block today via `FreeSlots.compute` (Phase E).

## Rolling week widget

| Option | Values | Notes |
|--------|--------|-------|
| `rolling_week_widget` | on/off | When on, day 1 = today (local timezone) |
| Day count | 3 / 5 / 7 | Default 7 |
| Show free gaps | on/off | Free-slot chips under each day |
| Theme | Light / Dark / System / Material You | Follows app theme when System |
## Layout (rolling)

```text
┌────────┬────────┬────────┬───── ... ─────┐
│ Today  │ +1     │ +2     │               │
│ events │ events │ Open   │               │
│ ···    │ ···    │ Free   │               │
│        │        │ 14–16  │               │
└────────┴────────┴────────┴───── ... ─────┘

```

## Data sources

1. Local Fossify calendar store / system CalendarContract.
2. Continuum Google sync overlay when SSO linked.

## Update triggers

- `ACTION_DATE_CHANGED` / midnight alarm → re-anchor rolling window.
- Calendar content observer → refresh events + free slots.
- Continuum settings revision apply → refresh open-days / redact titles.
