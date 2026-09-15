# Feature: android-fossify-debrand

User-visible About / thank-you / suite CTAs say Continuum. Keep GPL credit and FossifyOrg/Calendar in legal/docs.

## Acceptance criteria

- User-visible behavior: About and Commons thank-you do not offer Fossify.org / Reddit / Telegram / “More Fossify apps” as product CTAs; donate stays Continuum Venmo
- Offline/error behavior: missing donate URL hides the donate row (existing pattern)
- Accessibility: About links keep content descriptions
- i18n: English `values/strings.xml` About/thanks keys; do not rewrite FAQ #2 here

## Smoke scenario

1. Given Settings → About
2. When the user reads actions
3. Then they see Continuum privacy / GitHub / Venmo, not Fossify store identity

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/mobile/app/src/main/kotlin/org/fossify/calendar/continuum/` About helpers |
| View | About activity + `res/values/strings.xml` About/thanks keys |
| Tests | existing `ContinuumAboutLinksTest.kt` plus string assertions |
| Wiring | none in desktop |

## Tests

- Automated: yes — About link helper tests; grep/unit that English About strings omit Fossify.org CTA URLs
- Coverage: donate URL is Continuum; suite CTAs hidden or relabeled

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Keep GPL-3, Commons credit, and `SOURCE_NAMESPACE=org.fossify.calendar`. Do not touch `values-*/faq_2_text`.

## Notes

Sprint 5 left Fossify debrand out. `docs/BRAND.md` leftover #3.
