# Continuum Calendar (Android)

<p align="center">
  <img alt="Continuum Calendar" src="graphics/icon-marketing-neon.png" width="280" />
</p>

Private FOSS calendar for Android — Continuum branding on a [Fossify Calendar](https://github.com/FossifyOrg/Calendar) fork, with optional peer sync to Continuum desktop.

**Application id:** `org.continuumcalendar.app` (debug: `org.continuumcalendar.app.debug`)

## Features

- Ad-free local calendar with CalDAV / Google calendar sync
- Month, day, week, year, and agenda (event list) views
- Widgets and reminders
- Continuum settings: Sync with desktop (Drive App Data), notifications, Open-day agenda

## Build

See the monorepo root `README.md` and `docs/GOOGLE_API_SETUP.md`. Requires Android SDK; set Continuum OAuth client ids in `apps/mobile/local.properties` for peer sync.

## License & credits

- This Android app is **GPL-3.0** (upstream Fossify Calendar / Simple Calendar lineage).
- Continuum product docs and desktop app are under the monorepo **MIT** license unless noted.
- Upstream: [FossifyOrg/Calendar](https://github.com/FossifyOrg/Calendar) and Fossify Commons.

Brand tokens and mark: [`docs/BRAND.md`](../../docs/BRAND.md).
