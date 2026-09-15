# Feature: android-faq-locales

Non-English FAQ #2 must not send Google users to the official Calendar app or DAVx5. Match English Connect Google copy.

## Acceptance criteria

- User-visible behavior: every `faq_2_text` in `res/values-*/strings.xml` points at Continuum Connect Google, not DAVx5 / official Google Calendar
- Offline/error behavior: missing locale falls back to English `values/strings.xml` (already Continuum)
- Accessibility: N/A — string resources
- i18n: this feature is the i18n pass

## Smoke scenario

1. Given the app in a non-en locale that still had Fossify FAQ #2
2. When the user opens FAQ question 2
3. Then the text does not mention DAVx5

## Container map

| Layer | Path |
|-------|------|
| Logic | none |
| View | `apps/mobile/app/src/main/res/values-*/strings.xml` (`faq_2_text` only) |
| Tests | `apps/mobile/app/src/test/kotlin/.../FaqLocaleCopyTest.kt` (no DAVx5 in faq_2_text) |
| Wiring | none |

## Tests

- Automated: yes — scan locale `faq_2_text` for `DAVx5` / `davx5`
- Coverage: all `values-*` FAQ #2 strings

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Do not edit English `values/strings.xml` (Sprint 4 already rewrote it). Do not debrand About in this row.

## Notes

Sprint 4 leftover; Sprint 5 left other-locale FAQ out because English default is safe.
