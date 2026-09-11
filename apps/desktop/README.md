# Continuum Calendar — Desktop

Tauri 2 + React + TypeScript + Vite + Tailwind CSS + FullCalendar.

## Commands

```bash
# From repo root
npm install
npm run dev:desktop      # Vite only
npm run tauri:dev        # Tauri window + Vite

# From this folder
npm run dev
npm run tauri:dev
npm run build
npm run install:local    # release binary → ~/.local (Linux) or %LOCALAPPDATA% (Windows); Linux also writes XDG autostart + icons + claims default calendar (text/calendar, webcal)
bash scripts/claim-default-calendar.sh   # re-claim defaults without a full rebuild
npm run tauri:build      # NSIS (Windows) or AppImage (Linux)
npm run rename:appimage  # Linux: stable Continuum-Calendar-{ver}-x86_64.AppImage name

```

**Do not** copy `src-tauri/target/debug/app` (or `app.exe`) into the install folder or enable Start at login for that binary.
That build always loads `http://localhost:5173` and fails when Vite is down (KB-035).
Use `npm run install:local` (or `npm run tauri:build`) for anything you expect to run as the product app.

## Linux system packages (Ubuntu / Debian)

```bash
sudo apt install libwebkit2gtk-4.1-dev libayatana-appindicator3-dev \
  librsvg2-dev patchelf build-essential curl wget file libssl-dev \
  libgtk-3-dev libsoup-3.0-dev

```

Runtime tray indicators need `libayatana-appindicator3-1` (usually pulled in with the -dev package).

Release asset name for GitHub Releases: `Continuum-Calendar-{version}-x86_64.AppImage`
(after `npm run tauri:build` and `npm run rename:appimage`).

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

See root `.env.example` and [docs/GOOGLE_API_SETUP.md](../../docs/GOOGLE_API_SETUP.md).
