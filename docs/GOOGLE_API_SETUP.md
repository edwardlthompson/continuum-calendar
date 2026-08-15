# Google sign-in for Continuum (end users)

## What you experience as a user

Tap **Sign in with Google** (desktop) or **Connect Google calendars** (Android). You sign in with **your** Google account in Google’s own screen. Continuum never sees your password and does not run a “login database.”

- **Android (primary):** Uses Google calendars already on the phone (the Google account you added in Android Settings). Continuum enables CalDAV sync and lets you pick calendars — same trust model as Fossify, no Continuum OAuth required.
- **Desktop:** Uses Google’s standard “Sign in with Google” button. Calendar, Contacts (attendee autocomplete), Tasks, and private Continuum settings sync use Google APIs **on your behalf** after you approve.

## What Continuum does *not* do

- Does not ask you to create a Google Cloud project
- Does not store your Google password
- Does not sell or broker login data

## Why a Client ID exists at all (for the app, not for you)

Google requires every app that calls Calendar/Contacts/Tasks APIs to identify itself with a **public OAuth client ID** baked into the app. That ID belongs to the **Continuum Calendar project**, not to each end user. End users only click Sign in.

You do **not** need to open Google Cloud Console or create any OAuth client for your own use.

Until Continuum ships its published client ID in release binaries:

- **Android works today** via Settings → Continuum → **Connect Google calendars** (uses the Google account already on the phone).
- **Desktop Sign in** needs Continuum’s public Desktop client ID embedded at build time (`VITE_GOOGLE_CLIENT_ID`) by whoever packages Continuum — still not something end users create or manage.

## Maintainer checklist (once per Continuum release, not per user)

1. Google Cloud project for Continuum Calendar (product).
2. Enable APIs: Calendar, People, Drive (App Data), Tasks.
3. OAuth consent screen: External, scopes listed below.
4. Create OAuth clients: Desktop + Android (package + SHA-1) in the **same** Google Cloud project (Drive App Data is per project — different projects = settings never sync across devices).
5. Ship Continuum’s **Desktop client ID + client secret** in the Windows EXE (Google’s installed-app type; the “secret” is not confidential and the token endpoint requires it). Ship the **Android client ID only** in the APK (no `BuildConfig` secret). Keep `.env` / `local.properties` gitignored — never commit secrets.
6. Continuum peer remotes (desktop ↔ Android) live in Drive App Data (same GCP project Client ID on both — `scripts/set-desktop-google-client-id.py` also writes `apps/mobile/local.properties`):
   - `continuum-settings.json` — Continuum preferences (24h, first day of week, privacy, etc.)
   - `continuum-local-events.json` — Continuum-owned local calendars/events (privacy path)
7. **Privacy mode** (`useGoogleCalendar: false`) skips Google Calendar sync/display but still uses Drive App Data for Continuum settings + local event peer sync. CalDAV alone is not enough on Android for peer sync — open **Settings → Continuum → Sync with desktop** once (Drive App Data scope). Prefer an **Android** OAuth client in the same GCP project (`continuum.google.android.client.id` in `apps/mobile/local.properties`): Custom Tabs + redirect `com.googleusercontent.apps.<prefix>:/oauth2redirect`. Package `org.continuumcalendar.app.debug`, debug SHA-1 `B2:96:9C:DF:CD:01:8E:0E:6B:E7:CF:4D:D9:42:F0:AE:78:EA:76:68` (re-register this package + SHA-1 on a new Android OAuth client after the rename). On that client, open **Advanced settings** and enable **Enable custom URI scheme** (off by default; otherwise Google returns `invalid_request` / “Custom URI scheme is not enabled”). Settings also has **Paste OAuth redirect URL** recovery.

## Scopes

| Scope | Purpose |
|-------|---------|
| `https://www.googleapis.com/auth/calendar` | Event sync (skipped when privacy mode is on) |
| `https://www.googleapis.com/auth/contacts.readonly` | Attendee autocomplete |
| `https://www.googleapis.com/auth/drive.appdata` | Continuum settings + local-events peer sync (private app folder) |
| `https://www.googleapis.com/auth/tasks` | Google Tasks (optional parity) |

## Desktop env (maintainer / release packaging)

```bash
# apps/desktop/.env — Continuum’s public Desktop client ID
VITE_GOOGLE_CLIENT_ID=….apps.googleusercontent.com
# Used for browser/Vite only. Tauri Sign-in opens the system browser and
# completes via a one-shot http://127.0.0.1:<port>/ loopback (Desktop client type).
VITE_GOOGLE_REDIRECT_URI=http://localhost:5173/oauth/callback
```

### Local maintainer setup (this machine)

1. [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials)
2. Create **OAuth client ID** → Application type **Desktop app**
3. Copy the **Client ID** and **Client secret** into `apps/desktop/.env` (gitignored):

```bash
python scripts/set-desktop-google-client-id.py YOUR_CLIENT_ID.apps.googleusercontent.com YOUR_CLIENT_SECRET
```

Google’s token endpoint requires the Desktop client secret (`invalid_request — client_secret is missing` without it). Vite bakes `VITE_GOOGLE_*` into release EXEs from this gitignored `.env`. Do not strip the secret in `PROD`.

4. Restart `npm run tauri:dev` so Vite picks up the env var.
