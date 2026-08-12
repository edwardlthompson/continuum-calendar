# Mobile fork plan — FossifyOrg/Calendar

Target path: `apps/mobile` (Kotlin Android app based on [FossifyOrg/Calendar](https://github.com/FossifyOrg/Calendar)).

> Status: documentation + submodule placeholder. Do **not** add the git submodule until `[HUMAN]` approves (`docs/REPO_HYGIENE.md`).

## Planned product settings

| Key | Type | Default | Behavior |
|-----|------|---------|----------|
| `show_empty_days_in_agenda` | Boolean | `true` | Agenda/Schedule lists render blank days instead of collapsing them |
| `rolling_week_widget` | Boolean (widget config) | `true` | Homescreen widget columns start at **today** and roll forward |

## Themes

- Keep Fossify Light / Dark / System (follow device).
- Add **Material You** dynamic color when Android 12+ (`DynamicColors` / Material 3), alongside the standard trio.
- Persist preference with existing theme settings storage.

## Contacts / attendees

- Hook Android **ContactsContract** (system Contacts provider) for quick-invite autocomplete when creating events.
- Optional later: Google Contacts sync path aligned with desktop scopes — FOSS builds must remain usable without proprietary Google Play Services for local contacts.

## Implementation sketch

1. Add submodule (after approval):

   ```bash
   git submodule add https://github.com/FossifyOrg/Calendar.git apps/mobile
   ```

2. Settings UI: toggle for `show_empty_days_in_agenda` under Agenda / Schedule.
3. Agenda adapters: when building day sections, include days with zero events.
4. Widget config Activity: checkbox `rolling_week_widget`; update `AppWidgetProvider` to use `LocalDate.now()` as first column.
5. Theme: apply Material You overlay when enabled; fall back to Light/Dark/System.
6. Event editor: Contacts picker / autocomplete via `ContactsContract.CommonDataKinds.Email`.

## FOSS compliance

- No Firebase, Play In-App Update, or closed analytics.
- Prefer UnifiedPush / system providers per bootstrap Module A.
- CI FOSS greps scan Gradle manifests only.

## Verification

- `[ADB]` Emulator: agenda with empty mid-week days visible.
- `[ADB]` Widget: first column = today after reboot/date change.
- `[ADB]` Theme: Light, Dark, System, Material You (API 31+).
