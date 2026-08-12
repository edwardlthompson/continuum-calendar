# Continuum Calendar

<p align="center">
  <img src="docs/brand/logo-neon-3d.png" alt="Continuum Calendar" width="420" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-2ea043?style=flat-square" alt="MIT" />
  <img src="https://img.shields.io/badge/FOSS-no_tracking-656d76?style=flat-square" alt="FOSS" />
  <img src="https://img.shields.io/badge/desktop%20%7C%20Android-open-0969da?style=flat-square" alt="Platforms" />
</p>

**Your week, your devices, your rules — without a subscription.**

Continuum is a free and open-source calendar for people who want Google when it’s useful, local privacy when it’s not, and the same agenda on phone and desktop without paying a “Pro” tax.

---

## Why Continuum?

Most polished calendars either lock serious features behind paywalls, or treat your schedule as a product. Continuum ships the opposite: **full capability by default**, **opt-in Google**, and **zero Continuum accounts, ads, or telemetry**.

| You get for free | What others often charge for — or never ship |
|------------------|-----------------------------------------------|
| **Phone ↔ desktop peer sync** for Continuum settings *and* local events | Cross-device sync gated behind Premium / cloud seats |
| **Privacy mode** — hide Google Calendar, keep peer sync of *your* local calendars | “Local-only” apps that can’t talk to your laptop |
| **Multi-calendar Google sync** (all the calendars you choose) | One calendar free, rest behind upgrade |
| **CalDAV + ICS import/export** alongside Google | Extra connectors sold as add-ons |
| **Rolling week from today** + agenda that shows **Open** empty days | Sparse “free day” UX buried in paid plans |
| **Copy free slots / propose meeting times** | Scheduling assistants as separate paid products |
| **Screenshot title redaction** for demos & sharing | Rare outside enterprise suites |
| **Light / Dark / System themes**, 24-hour time, first day of week — synced both ways | Preference sync as a subscription perk |
| **MIT FOSS** — audit it, fork it, keep it forever | Closed source you can’t leave |

You sign in with **your** Google account when you want Calendar, Contacts autocomplete, or Continuum peer remotes. Continuum never sees your password, never runs a Continuum login database, and never phones home by default.

---

## What it feels like

- **Agenda first** — upcoming days at a glance, with teal **Open** placeholders so empty days are visible, not invisible.
- **Rolling week** — start from *today* (or your preferred week start) instead of fighting a Monday-only grid.
- **One Continuum** — flip 24-hour time, birthdays, privacy, or default calendar on Android and watch desktop catch up (and the other way around).
- **Local when you want it** — create events that live in Continuum’s local calendars and peer-sync phone ↔ desktop via Google Drive’s private **App Data** folder. Turn off Google Calendar and those local events still move with you.

---

## Get started

### Desktop

```bash
npm install
npm run tauri:dev   # or: npm run dev:desktop
```

Maintainers: embed Continuum’s Google Client ID in `apps/desktop/.env` — see [`docs/GOOGLE_API_SETUP.md`](docs/GOOGLE_API_SETUP.md). End users only click **Sign in with Google**.

### Android

Build the Fossify-based Continuum app from `apps/mobile`, then:

1. **Settings → Continuum → Connect Google calendars** (uses the Google account already on the phone).
2. Complete Continuum Google API sign-in once if you want **settings + local-event peer sync** with desktop.
3. Optional: turn off **Use Google Calendar** for privacy mode.

---

## Free features, plainly

1. **Peer remote, not a Continuum cloud** — Settings and local events sync through *your* Drive App Data (`continuum-settings.json`, `continuum-local-events.json`). No Continuum server in the middle.
2. **Privacy without isolation** — `Use Google Calendar` off = no Google event sync/display; Continuum local peer sync still works.
3. **Scheduling helpers** — copy free slots, propose times, jump to the next free block.
4. **Bring your own sources** — Google, CalDAV, local calendars, ICS files and webcal links.
5. **Respectful defaults** — opt-in only telemetry (off by default), GDPR/CCPA-minded, FOSS under MIT.

---

## Repository layout

| Path | Role |
|------|------|
| `apps/desktop` | Tauri + React desktop app |
| `apps/mobile` | Android (Fossify Calendar fork) |
| `packages/shared` | Shared types & Continuum settings / local-events protocol |
| `docs/` | Setup, parity matrix, architecture |
| `BUILD_PLAN.md` | Active task board |

More for contributors and agents: [`docs/START_HERE.md`](docs/START_HERE.md) · [`docs/PARITY_MATRIX.md`](docs/PARITY_MATRIX.md) · [`docs/architecture/OVERVIEW.md`](docs/architecture/OVERVIEW.md).

---

## License

MIT — see [`LICENSE`](LICENSE). Use it, share it, improve it. Continuum stays free because the calendar should be yours.
