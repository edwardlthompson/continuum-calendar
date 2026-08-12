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
```

## Features (prototype)

- Rolling 7-day time grid (`Today` = column 1)
- Explicit empty days / open hours visible
- Theme: Light / Dark / System (`ThemeContext`, Tailwind `class` strategy)
- Google Calendar + Contacts API module (`src/services/googleApi.ts`)
- Copy Free Slots clipboard helper (`src/utils/freeSlots.ts`)

## Env

See root `.env.example` and [`docs/GOOGLE_API_SETUP.md`](../../docs/GOOGLE_API_SETUP.md).
