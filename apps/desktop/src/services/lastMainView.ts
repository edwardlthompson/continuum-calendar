import type { MainView } from '../components/CalendarToolbar'

/** Device-local only (not Drive-synced) so PC and phone can remember different views. */
export const LAST_MAIN_VIEW_KEY = 'continuum.lastMainView'

const VIEWS: readonly MainView[] = ['agenda', 'rolling', 'month', 'year']

export function parseMainView(raw: unknown): MainView | null {
  return typeof raw === 'string' && (VIEWS as readonly string[]).includes(raw)
    ? (raw as MainView)
    : null
}

/** First launch defaults to week — Continuum’s primary desktop grid. */
export function loadLastMainView(fallback: MainView = 'rolling'): MainView {
  try {
    return parseMainView(localStorage.getItem(LAST_MAIN_VIEW_KEY)) ?? fallback
  } catch {
    return fallback
  }
}

export function saveLastMainView(view: MainView): void {
  if (!parseMainView(view)) return
  try {
    localStorage.setItem(LAST_MAIN_VIEW_KEY, view)
  } catch {
    /* ignore quota / private mode */
  }
}
