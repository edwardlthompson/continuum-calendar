# Feature: desktop-a11y-contrast

Dark-mode CTA contrast, control heights, focus-visible, reduced-motion splash, settings focus return, and warn tokens.

## Acceptance criteria

- User-visible behavior: Sign in, selected view chip, and FAB use `--cc-on-accent` on `--cc-accent`; chrome buttons are `h-9`; Sign in and FAB are at least 44px; body radial glow is gone
- Offline/error behavior: invalid hex in `contrastRatio` returns 0; CSS fallbacks stay on tokens
- Accessibility: WCAG 2.2 AA contrast on locked pairs; `:focus-visible` ring; splash skipped when `prefers-reduced-motion: reduce`; Escape on Settings restores focus to the Settings button
- i18n: N/A — desktop copy stays English

## Smoke scenario

1. Given the desktop app in dark theme
2. When the user tabs to Sign in and Agenda, then opens Settings and presses Escape
3. Then CTAs use ink-on-teal, a focus ring is visible, and focus returns to Settings

## Container map

| Layer | Path |
|-------|------|
| Logic | `apps/desktop/src/chrome/contrast.ts` |
| View | `brand.css`, `index.css`, `ContinuumSplash.tsx`, App chrome classes |
| Tests | `apps/desktop/src/chrome/contrast.test.ts` |
| Wiring | `App.tsx` `settingsBtnRef` + `closeSettings` |

## Tests

- Automated: yes — `apps/desktop/src/chrome/contrast.test.ts`
- Coverage: dark and light CTA pairs ≥ 4.5:1; invalid hex → 0

## Fallback validation

- Why tests are not feasible: N/A (automated tests exist)
- Command: `python3 scripts/agent-run.py watch-agent-gates --once --autofix --scope auto`

## Definition of Done

Browser: Sign in and Agenda chip use on-accent. Sequential Feature 1 of Sprint 5.

## Notes

Do not put `h-11` on every chip. Open labels use `--cc-today` via `.cc-open`.
