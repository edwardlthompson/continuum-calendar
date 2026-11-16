<!-- Continuum product README — keep badge contract in scripts/lib/readme_badges.py -->

<p align="center">
  <img src="branding/assets/logo-mark.svg" alt="Continuum Calendar logo" width="160" height="160" />
</p>

<h1 align="center">Continuum Calendar</h1>

<p align="center"><strong>Your week, your devices, your rules — without a subscription.</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-2ea043?style=flat-square" alt="MIT" />
  <img src="https://img.shields.io/badge/template-2.0.1-0969da?style=flat-square" alt="Template" />
  <img src="https://img.shields.io/badge/FOSS-no_tracking-656d76?style=flat-square" alt="FOSS" />
  <a href="https://github.com/edwardlthompson/continuum-calendar/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/edwardlthompson/continuum-calendar/ci.yml?style=flat-square&label=CI" alt="CI" /></a>
  <img src="https://img.shields.io/badge/AGENT-2ea043?style=flat-square" alt="AGENT" />
  <img src="https://img.shields.io/badge/HUMAN-0969da?style=flat-square" alt="HUMAN" />
  <img src="https://img.shields.io/badge/ADB-bf8700?style=flat-square" alt="ADB" />
  <img src="https://img.shields.io/badge/AUTO-656d76?style=flat-square" alt="AUTO" />
  <img src="https://img.shields.io/badge/web-stack-646cff?style=flat-square" alt="web" />
  <img src="https://img.shields.io/badge/python-stack-3776AB?style=flat-square" alt="python" />
  <img src="https://img.shields.io/badge/android-stack-3DDC84?style=flat-square" alt="android" />
</p>

## Pitch

Continuum is a free and open-source calendar for people who want Google when it's useful, local privacy when it's not, and the same agenda on phone and desktop without paying a Pro tax.

## Features

- Rolling week from today with explicit Open empty days
- Google Calendar sync with peer settings and local-event overlays
- CalDAV + ICS import/export alongside Google
- Compose Multiplatform tree under `kmp/` (hotkeys, Tasks, holidays, free slots)
- Desktop + Android FOSS apps under MIT
- Cyan + purple infinity brand mark — owned Continuum identity

## Quick start

1. Clone the repo and open it in Cursor.
2. Follow `docs/START_HERE.md` for the startup sequence.
3. Desktop: `apps/desktop` (Tauri) or `kmp/desktopApp` (CMP).
4. Android: `apps/mobile` FOSS flavor.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Agent workflow: `/bootstrap`, `/build`, `/verify`, `/ship` per [`docs/help/BATCH_COMMANDS.md`](docs/help/BATCH_COMMANDS.md).

## Security

See [`SECURITY.md`](SECURITY.md). Never commit secrets or `.env` files.

## License

MIT — see [`LICENSE`](LICENSE).

GitHub Pages demo: https://edwardlthompson.github.io/continuum-calendar/
