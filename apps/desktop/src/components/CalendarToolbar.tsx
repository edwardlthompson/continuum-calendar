import type { Ref } from 'react'
import { hotkeyTitle } from '../hooks/desktopHotkeys'

export type MainView = 'agenda' | 'rolling' | 'month' | 'year'

export function CalendarToolbar(props: {
  view: MainView
  onView: (view: MainView) => void
  query: string
  onQuery: (q: string) => void
  jumpDate: string
  onJumpDate: (iso: string) => void
  onToday: () => void
  searchRef?: Ref<HTMLInputElement>
}) {
  const btn = (id: MainView, label: string, hint: string) => (
    <button
      type="button"
      className={`rounded px-2 py-1 text-sm ${props.view === id ? 'bg-[var(--cc-accent)] text-white' : 'border border-[var(--cc-border)]'}`}
      aria-pressed={props.view === id}
      title={hotkeyTitle(label, hint)}
      onClick={() => props.onView(id)}
    >
      {label}
    </button>
  )
  return (
    <div className="flex min-w-0 flex-wrap items-center gap-2">
      {btn('agenda', 'Agenda', '1')}
      {btn('rolling', 'Week', '2')}
      {btn('month', 'Month', '3')}
      {btn('year', 'Year', '4')}
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1 text-sm hover:bg-[var(--cc-accent-soft)]"
        aria-label="Go to today"
        title={hotkeyTitle('Today', 'T')}
        onClick={props.onToday}
      >
        Today
      </button>
      <input
        ref={props.searchRef}
        type="search"
        className="cc-native-field min-w-0 flex-1 rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
        placeholder="Search events (/)"
        value={props.query}
        onChange={(e) => props.onQuery(e.target.value)}
        aria-label="Search events"
        title={hotkeyTitle('Search events', '/ or F')}
      />
      <label
        className="flex items-center gap-1 text-sm text-[var(--cc-muted)]"
        title={hotkeyTitle('Jump to date', 'G')}
      >
        Jump
        <input
          type="date"
          className="cc-native-field rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
          value={props.jumpDate}
          onChange={(e) => props.onJumpDate(e.target.value)}
          aria-label="Jump to date"
          title={hotkeyTitle('Jump to date', 'G')}
        />
      </label>
    </div>
  )
}
