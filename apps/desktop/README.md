# Continuum Calendar — Desktop

Tauri 2 + React + TypeScript + Vite + Tailwind CSS + FullCalendar.

## Commands

`ash
# From repo root
npm install
npm run dev:desktop      # Vite only
npm run tauri:dev        # Tauri window + Vite

# From this folder
npm run dev
npm run tauri:dev
npm run build
npm run install:local    # tauri build --no-bundle → %LOCALAPPDATA%\Continuum Calendar\app.exe
`

**Do not** copy src-tauri\target\debug\app.exe into the install folder or Start with Windows.
That binary always loads http://localhost:5173 and shows Edge ERR_CONNECTION_REFUSED when Vite is down (KB-035).
Use
pm run install:local (or
pm run tauri:build) for anything you expect to run as the product app.

## Features (prototype)

- Rolling 7-day time grid (Today = column 1)
- Explicit empty days / open hours visible
- Theme: Light / Dark / System (ThemeContext, Tailwind class strategy)
- Google Calendar + Contacts API module (src/services/googleApi.ts)
- Copy Free Slots clipboard helper (src/utils/freeSlots.ts)

## Keyboard shortcuts

| Action | Key |
|--------|-----|
| New event | `N` |
| Today | `T` |
| Agenda / Week / Month / Year | `1` / `2` / `3` / `4` |
| Search events | `/` or `F` |
| Jump to date (prompt) | `G` |
Shortcuts are disabled while focus is in a text field. The full list also lives in **Settings → Window & startup → Keyboard shortcuts**. Toolbar buttons show their shortcut on hover.

## Env

See root .env.example and [docs/GOOGLE_API_SETUP.md](../../docs/GOOGLE_API_SETUP.md).
