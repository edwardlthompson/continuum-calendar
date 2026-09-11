# Privacy Policy — Continuum Calendar

Last updated: 2026-09-11

Continuum Calendar is FOSS calendar software (desktop + Android). This policy describes what the apps store on your device and what they send to Google or GitHub when you opt in. We do not run a Continuum login database and we do not sell personal data.

Stable URL for Google OAuth consent: [docs/PRIVACY.md on GitHub](https://github.com/edwardlthompson/continuum-calendar/blob/main/docs/PRIVACY.md).

## Data We Collect

| Data | Purpose | Lawful Basis | Retention |
|------|---------|--------------|-----------|
| Google OAuth tokens | Sign in and Calendar / Drive App Data APIs | Contract — you tapped Sign in | Until you sign out on that device |
| Calendar events | Display and sync | Contract | Device + your Google Calendar |
| Continuum settings JSON | Peer sync between desktop and phone | Contract | Google Drive App Data until you disconnect |
| Local Continuum events | Privacy-path calendars | Contract | Device + Drive App Data when signed in |
| Optional crash text | Debug after “Save last crash” | Consent | Device only until you delete or send a GitHub issue |
| Update check metadata | See if a newer installer exists | Legitimate interest | Local: `last_checked`, `installed_artifact_format`, `check_interval` — no PII sent |
## App update checks

- Release endpoint: GitHub Releases API or configured manifest URL
- Stored locally: `last_checked`, `installed_artifact_format`, `check_interval`
- No PII transmitted

## Data We Do Not Collect

- No Continuum cloud account
- No advertising ID or third-party analytics
- No tracking without explicit opt-in
- No sale of personal data
- No PII in logs without user consent
- No crash upload unless you choose GitHub feedback
- Contacts and Tasks APIs are not requested on desktop Sign in in the current production configuration (Calendar + Drive App Data)

## Google APIs

Desktop Sign in uses a Continuum **Desktop** OAuth client. Android can use the Google account already on the phone (CalDAV) and, for desktop peer sync, an **Android** OAuth client in the same Google Cloud project. Tokens never leave your device except to Google’s token and API endpoints.

## User Rights (GDPR / CCPA)

- **Access:** Users can request a copy of their data
- **Deletion:** Sign out in Settings clears stored tokens on that device; delete Google Calendar data in the Google account
- **Opt-out:** Telemetry and analytics are opt-in only; turn off update checks in Settings
- **Portability:** Export settings / ICS where technically feasible

Drive App Data files (`continuum-settings.json`, `continuum-local-events.json`) are removed when you disconnect that Google account from Continuum or delete the files from Google’s App Data.

## Data Minimization

- Collect only what each feature requires
- Use local-first storage where possible
- Anonymize or aggregate analytics data

## DPIA Checklist (`[HUMAN]`)

If processing EU personal data:

- ✅ Document processing purpose and legal basis (this file)
- ✅ Assess necessity and proportionality (OAuth + calendar sync only after Sign in)
- ✅ Identify risks and mitigations (app-private token vault; no Continuum cloud)
- ✅ Record in `DECISION_LOG.md` or ADR (2026-09-11 F-003 / F-004)

## Children

Continuum is not directed at children under 13.

## Contact

Privacy inquiries: see maintainers in `.github/CODEOWNERS` or `SECURITY.md`.
