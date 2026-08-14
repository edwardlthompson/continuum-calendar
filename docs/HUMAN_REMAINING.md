# Continuum Calendar — HUMAN checklist (GitHub public)

> **Distribution target:** GitHub Releases only (Windows desktop + FOSS Android APK).  
> F-Droid, Winget, and Play Store are **out of scope** for this ship.  
> Run commands from repo root: `cd "C:\Users\edwar\Continuum Calendar"`  
> On Windows: `.\scripts\...` or `pwsh -File .\scripts\...` — not bare `pwsh scripts\...`.  
> Brand: [`docs/BRAND.md`](BRAND.md) · OAuth: [`docs/GOOGLE_API_SETUP.md`](GOOGLE_API_SETUP.md) · Board: [`BUILD_PLAN.md`](../BUILD_PLAN.md)

**Do not commit** `.env`, `local.properties`, keystores, or client secrets.

**Product stance (2026-08-12):** Feature work is treated as complete for a first public cut. Remaining work is packaging, legal, OAuth for *end users*, and cutting the Release — not new features. Maintainer Android Sync / Custom URI setup is **done** (removed from this checklist).

---

## Already done (do not redo)

| Item | Status |
|------|--------|
| Package id `org.continuumcalendar.app` (+ `.debug`) | ✅ |
| Continuum ∞ brand (flat app icon + neon marketing/splash) | ✅ |
| Fastlane en-US + mobile README → Continuum | ✅ |
| Commons “fake version” gate patched for Continuum package | ✅ |
| Maintainer Google OAuth / Sync with desktop (debug) | ✅ |
| GitHub repo is **public** | ✅ (`edwardlthompson/continuum-calendar`) |

---

## Priority for first public GitHub Release

| # | Item | Blocks ship? | Effort |
|---|------|--------------|--------|
| 1 | Public Desktop + Android clients in **release** builds (no secret) | **Yes** (Sign in for strangers) | ~30–60 min |
| 2 | Privacy Policy + LICENSE copyright | **Yes** (legal / consent URL) | ~20 min |
| 3 | Cut GitHub Release (desktop + FOSS APK) | **Yes** (distribution) | ~30–60 min |
| 4 | Upload GitHub **Social preview** neon image | Soft — do before sharing the repo link | ~2 min |
| 5 | Approve F-003 secure token storage (or accept risk for v1) | Soft — approve in chat | 2 min |
| 6 | Repo harden (Dependabot + branch protection) | Soft for v1 | ~10 min |
| 7 | Triage Dependabot mediums (F-009) | Soft for v1 | ~15–30 min |

Agent polish (not HUMAN, not required for GitHub-only v1): Commons About / Fossify suite CTAs still show upstream strings — ask the agent after Release if you want a clean About screen.

---

## 1) Public OAuth for end users (F-002)

Strangers must only tap **Sign in** — they must not create a Cloud project. Ship **Client IDs only** (PKCE). **Never** put `client_secret` in GitHub Release artifacts.

### 1.1 Consent screen

1. [OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent) → **External**.
2. App name: `Continuum Calendar`.
3. Scopes:

```
https://www.googleapis.com/auth/calendar
https://www.googleapis.com/auth/contacts.readonly
https://www.googleapis.com/auth/drive.appdata
https://www.googleapis.com/auth/tasks
```

4. While **Testing**: add every GitHub downloader’s Google account as a test user, **or** start verification for wide use.
5. Privacy Policy URL → set after §2 (GitHub Pages or stable repo URL).

### 1.2 Desktop client

1. Credentials → **OAuth client ID → Desktop app**.
2. Local maintainer machine only (gitignored):

```powershell
python scripts/set-desktop-google-client-id.py PASTE_DESKTOP_CLIENT_ID.apps.googleusercontent.com PASTE_DESKTOP_CLIENT_SECRET
# Creates/updates apps/desktop/.env — do not commit
npm run tauri:dev -w @continuum/desktop
```

3. For **public Release builds**: inject **Client ID** via CI secrets / build env; omit secret from the published binary. If Google still requires a Desktop secret for token exchange on your client type, document that limitation in the Release notes until a pure public+PKCE path lands — secret stays on CI/maintainer hosts only.

### 1.3 Android client in ship APK

Release `applicationId`: `org.continuumcalendar.app` + **release** keystore SHA-1. On that Android client, enable **Custom URI scheme** (Advanced settings). Bake the client id into release builds (CI secret — never commit).

v0.16.2 release keystore SHA-1 (add this fingerprint on the Android OAuth client): `72:40:1C:C3:82:8B:26:80:6A:D2:C1:F3:B9:97:52:76:13:C0:7F:4F`. Keystore files are gitignored at `apps/mobile/keystore.jks` + `keystore.properties` — **back them up off this machine** or you cannot update this APK.

```text
[HUMAN] F-002 done: Desktop + Android OAuth clients are public / PKCE; no client_secret in GitHub Release binaries. Consent screen scopes set.
```

---

## 2) Privacy Policy + LICENSE

### 2.1 LICENSE (root MIT)

Edit [`LICENSE`](../LICENSE) — replace template line:

```text
Copyright (c) 2026 agent-project-bootstrap contributors
```

with your copyright, e.g. `Copyright (c) 2026 Edward Thompson` (or your org).  
Keep `apps/mobile/` as **GPL-3** (Fossify fork); do not relicense the Android tree as MIT.

### 2.2 Privacy Policy

Rewrite [`docs/PRIVACY.md`](PRIVACY.md) from the template into Continuum product language. Minimum:

| Data | Purpose | Where |
|------|---------|--------|
| Google OAuth tokens | Calendar / Contacts / Drive App Data / Tasks | Device (harden after F-003) |
| Calendar events | Sync & display | Device + user’s Google account |
| Continuum settings JSON | Peer sync | Google Drive **App Data** (app-private) |
| Update check metadata | Optional update check | GitHub Releases API — no PII |

Publish a stable HTTPS URL (e.g. GitHub Pages `…/PRIVACY` or `docs/PRIVACY.md` on default branch) and paste it into the OAuth consent screen.

```text
[HUMAN] Privacy + LICENSE updated for Continuum; consent screen privacy URL set to <URL>.
```

---

## 3) Cut the first GitHub Release

**Channel:** GitHub Releases only — attach:

- Windows desktop installer / bundle (Tauri)
- FOSS Android APK (`org.continuumcalendar.app`, signed with your release keystore)
- Optional: SBOM if CI already produces one

### 3.1 Optional but recommended — rename the repo

Current remote: `edwardlthompson/agent-project-bootstrap`. For a public Continuum product, rename to something like `Continuum-Calendar` (GitHub → Settings → General → Repository name) and update clone URLs / About ([`docs/GITHUB_ABOUT.md`](GITHUB_ABOUT.md)).

### 3.2 Preflight

```powershell
python scripts/agent-run.py check-repo-hygiene
# Local gates you normally trust before ship — e.g.:
# python scripts/agent-run.py validate-bootstrap -- --quick
```

Confirm Release artifacts were built **with** public Client IDs and **without** secrets (§1).

### 3.3 Publish

Use your usual Release Please / `gh release create` / Actions release workflow. Tag e.g. `v1.0.0` (or `v0.9.0` if you want a soft launch). Release notes should mention:

- GitHub-only distribution
- Google Sign-in requires the Continuum OAuth consent (Testing vs verified)
- Android is GPL-3 (Fossify lineage); desktop/docs MIT
- How to install APK (sideload) and desktop

```text
[HUMAN] First GitHub Release published: v0.16.2 with desktop + FOSS APK.
https://github.com/edwardlthompson/continuum-calendar/releases/tag/v0.16.2
```

---

## 4) GitHub Social preview (neon marketing image)

GitHub **does not expose an API** to set the repo Social preview image — it must be uploaded in the UI. Image is already in the repo; use the helper to open Settings + the file:

```powershell
cd "C:\Users\edwar\Continuum Calendar"
.\scripts\open-github-social-preview.ps1
# optional: .\scripts\open-github-social-preview.ps1 -Repo YOUR_USER/Continuum-Calendar
```

Manual path if you skip the script:

1. Open `https://github.com/<owner>/<repo>/settings` → scroll to **Social preview** → **Edit** → **Upload an image…**
2. Upload [`docs/brand/github-social-neon-upload.jpg`](brand/github-social-neon-upload.jpg) (1280×640, under 1 MB — GitHub’s limit).  
   Full-res master remains [`docs/brand/github-social-neon.png`](brand/github-social-neon.png).

```text
[HUMAN] GitHub social preview set to docs/brand/github-social-neon-upload.jpg
```

---

## 5) Secure token storage (F-003) — approve or defer

Today tokens may live in less-hardened stores. For a cautious public v1, approve so the agent can implement next:

| Platform | Approach |
|----------|----------|
| Desktop (Tauri) | OS keychain / encrypted plugin store |
| Android | `EncryptedSharedPreferences` (or equivalent) for Continuum OAuth tokens |

```text
[HUMAN] F-003 approved: encrypted OS-backed storage on desktop + EncryptedSharedPreferences on Android. Agent may implement after Release if deferred.
```

Or explicitly defer:

```text
[HUMAN] F-003 deferred for v1 GitHub ship; tracked as follow-up. Accepting higher token-storage risk until implemented.
```

---

## 6) GitHub security (soft for v1)

```powershell
gh auth status
.\scripts\setup-github-repo.ps1
# optional: .\scripts\setup-github-repo.ps1 -Repo YOUR_USER/Continuum-Calendar
```

Enable Dependabot alerts, private vulnerability reporting, and `main` branch protection (required checks as the script / UI lists). About blurb: [`docs/GITHUB_ABOUT.md`](GITHUB_ABOUT.md).

```text
[HUMAN] Dependabot alerts + private vulnerability reporting + branch protection enabled on main.
```

---

## 7) Dependabot triage (F-009) — soft for v1

Playbook: [`docs/SECURITY_TRIAGE.md`](SECURITY_TRIAGE.md).

```powershell
gh browse --settings
# or open …/security/dependabot on the Continuum repo
```

```text
[HUMAN] F-009: Dependabot mediums triaged (fixed or deferred with DECISION_LOG rationale).
```

---

## Explicitly deferred (not part of this ship)

| Item | Why deferred |
|------|----------------|
| F-Droid submission | GitHub-only distribution |
| Winget / other package indexes | GitHub-only distribution |
| Google Play | FOSS / sideload path |
| Commons About full debrand | Agent follow-up; does not block install from Releases |
| Non-en Fastlane locale polish | No store listing this ship |
| Weekly / quarterly maintenance | After v1 is live |

---

## Quick reference

| What | URL / path |
|------|------------|
| Credentials | https://console.cloud.google.com/apis/credentials |
| Consent screen | https://console.cloud.google.com/apis/credentials/consent |
| Set desktop client | `python scripts/set-desktop-google-client-id.py <id> [secret]` |
| Set Android client | `python scripts/set-android-google-client-id.py <id>` |
| Release keystore SHA-1 | `keytool -list -v -keystore <release.jks>` |
| GitHub harden | `.\scripts\setup-github-repo.ps1` |
| GitHub social preview helper | `.\scripts\open-github-social-preview.ps1` |
| Social upload (≤1 MB) | [`docs/brand/github-social-neon-upload.jpg`](brand/github-social-neon-upload.jpg) |
| Privacy draft | [`docs/PRIVACY.md`](PRIVACY.md) |
| Security triage | [`docs/SECURITY_TRIAGE.md`](SECURITY_TRIAGE.md) |

---

## After you finish a block

Paste the matching `[HUMAN] …` line in chat (or mark ✅ in `BUILD_PLAN.md`). The agent can then bake release Client IDs, implement F-003, finish About debrand, and help draft the Release — without re-asking for Cloud Console clicks you already did.
