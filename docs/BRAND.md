# Continuum Calendar — Brand & public readiness

> Product visual identity and what still blocks a public release.  
> Canonical CSS: [`apps/desktop/src/styles/brand.css`](../apps/desktop/src/styles/brand.css)  
> Canonical mark (Android launcher look — bristled cyan+amber ∞): [`apps/desktop/public/continuum-mark.png`](../apps/desktop/public/continuum-mark.png)  
> Remaining HUMAN steps (paste scripts): [`docs/HUMAN_REMAINING.md`](HUMAN_REMAINING.md)

## Brand system

### Name

- **Product:** Continuum Calendar  
- **Short:** Continuum  
- **Do not ship as:** Fossify Calendar, Simple Calendar, or any FossifyOrg store identity

### Colors

| Token | Hex | Use |
|-------|-----|-----|
| Brand teal | `#0F6E8C` | Accent, today, Open days, primary CTA |
| Teal bright | `#4EB6D4` | Dark-mode accent, mark highlight |
| Teal deep | `#0A4F66` | Pressed / deep surfaces |
| Mist | `#D7EEF5` | Soft accent backgrounds (light) |
| Ink | `#0F172A` | Primary text (light) |
| Slate | `#5B6B82` | Muted text |
| Now bar | `#D32F2F` | Agenda chronological “now” rule |
| Mark cyan | `#00E5FF` | Left C + right yin-yang dot |
| Mark purple | `#E040FB` | Right C + left yin-yang dot (vaporwave) |
| Mark bg | `#0B1220` | Icon plate (dark) |

**Avoid:** purple-on-white AI defaults; cream + terracotta “AI brochure” look; generic Material calendar glyph as the product mark.

CSS variables live under `--cc-*` / `--cc-brand-*` in `brand.css`. Shared constant for Open days: `CONTINUUM_OPEN_DAY_COLOR` in `@continuum/shared`.

### Icon / mark

**Concept (canonical):** Cyan + purple vaporwave infinity — two facing “C”s with vertical tip faces, hairline center gap (C’s nearly touching), yin-yang dots (purple in cyan C, cyan in purple C), dark plate. Official flat reference: `docs/brand/logo-official.png` (same as `.cursor/logo-preview-smooth.png`). Regen app icons: `python scripts/generate-infinity-mark.py`. Do not ship bristled rings, teal “C”, or Fossify calendar bird.

**Source of truth (flat / in-app):**

```bash
python scripts/generate-infinity-mark.py
```

| Asset | Path |
|-------|------|
| Official flat reference | `docs/brand/logo-official.png` |
| Master raster | `apps/desktop/public/continuum-mark.png` |
| Favicon | `apps/desktop/public/favicon.png` (+ SVG fallback) |
| Android launcher | `apps/mobile/app/src/main/res/mipmap-*/ic_launcher.png` |
| Mobile README / store | `apps/mobile/graphics/icon.webp`, `featureGraphic.png` |
| Tauri window / installers | `apps/desktop/src-tauri/icons/*` |
| Brand tokens | `apps/desktop/src/styles/brand.css` |
| GitHub social (neon 3D) | `docs/brand/github-social-neon.png` (16:9) |
| Neon square (marketing / splash) | `docs/brand/logo-neon-3d.png` |
| Desktop splash asset | `apps/desktop/public/continuum-splash-neon.png` |
| Android splash asset | `apps/mobile/.../drawable-nodpi/continuum_splash_neon.png` |

SVG copies are simplified silhouettes for places that require vectors; **flat PNG/WebP = app icon**. Neon 3D = README, GitHub social, desktop/Android splash only.

**GitHub social image:** no public API — HUMAN step. Helper opens Settings + file:

```powershell
.\scripts\open-github-social-preview.ps1
```

Upload [`docs/brand/github-social-neon-upload.jpg`](brand/github-social-neon-upload.jpg) (1280×640, under 1 MB). Full-res master: [`docs/brand/github-social-neon.png`](brand/github-social-neon.png). Checklist: [`docs/HUMAN_REMAINING.md`](HUMAN_REMAINING.md) §4.

### Typography

- Desktop UI: Segoe UI / IBM Plex Sans / system-ui  
- Wordmark: bold Continuum with accent on Calendar optional via `.cc-brand-wordmark`

---

## Public release — what else is needed

**Channel:** GitHub Releases only (desktop + FOSS APK). F-Droid / Winget / Play are deferred.  
**Paste steps:** [`docs/HUMAN_REMAINING.md`](HUMAN_REMAINING.md).

### Done for identity

- Package id `org.continuumcalendar.app` · Continuum ∞ mark on Android + Tauri · Fastlane en-US / mobile README Continuum-branded · Commons fake-version patch

### Must-have before first GitHub Release

1. **Android Sync unblocked** — Custom URI scheme enabled; release Android client + SHA-1 for `org.continuumcalendar.app`
2. **Public Google OAuth clients** in release builds (no `client_secret` in artifacts)
3. **Consent screen** External + scopes (+ privacy URL)
4. **Privacy Policy** + **LICENSE** copyright (root MIT; mobile stays GPL-3)
5. **GitHub Release** with signed desktop + FOSS APK

### Soft for v1 (do soon after)

- F-003 secure token storage approval + implement  
- Dependabot / branch protection / medium triage  
- Commons About / Fossify suite CTA debrand (agent)

### Deferred

- F-Droid, Winget, Play  
- Non-en Fastlane polish · threat-model deep fill · feature polish rows in `BUILD_PLAN`

---

## Debrand checklist (Fossify fingerprints)

| Keep (legal / interop) | Remove / replace (branding) |
|------------------------|-----------------------------|
| GPL-3 + credit FossifyOrg/Calendar + Commons | Store title/descriptions, Fossify.org / Reddit / Telegram CTAs |
| `X-FOSSIFY-*` ICS props if needed for import | Launcher / Tauri Fossify logos |
| Internal docs noting “fork ancestry” | `org.fossify.calendar` applicationId / OAuth schemes |
| | Fastlane en-US “Fossify Calendar”; mobile README badges |
| | User-visible “Fossify” in About / thank-you |

Approved package id: **`org.continuumcalendar.app`**. Canonical mark: `python scripts/generate-infinity-mark.py` (not the old teal-C generator).

### Identity leftovers (non-blocking for GitHub v1)

1. ✅ Continuum ∞ mark → mipmaps + Tauri + public PNGs  
2. ✅ `APP_ID` + OAuth schemes + Fastlane en-US / README  
3. 🔲 Override About strings / hide Fossify thank-you (Commons) — agent follow-up  
4. ✅ Commons FakeVersionCheck patched (`6.1.6-continuum`)  
5. 🔲 Non-en Fastlane locales — deferred (no store this ship)
