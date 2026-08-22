import type { Ref } from 'react'

export type MainView = 'agenda' | 'rolling' | 'month' | 'year'

export function CalendarToolbar(props: {
  view: MainView
  onView: (view: MainView) => void
  query: string
  onQuery: (q: string) => void
  jumpDate: string
  onJumpDate: (iso: string) => void
  searchRef?: Ref<HTMLInputElement>
}) {
  const btn = (id: MainView, label: string) => (
    <button
      type="button"
      className={`rounded px-2 py-1 text-sm ${props.view === id ? 'bg-[var(--cc-accent)] text-white' : 'border border-[var(--cc-border)]'}`}
      aria-pressed={props.view === id}
      onClick={() => props.onView(id)}
    >
      {label}
    </button>
  )
  return (
    <div className="flex min-w-0 flex-wrap items-center gap-2">
      {btn('agenda', 'Agenda')}
      {btn('rolling', 'Week')}
      {btn('month', 'Month')}
      {btn('year', 'Year')}
      <input
        ref={props.searchRef}
        type="search"
        className="cc-native-field min-w-0 flex-1 rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
        placeholder="Search events (/)"
        value={props.query}
        onChange={(e) => props.onQuery(e.target.value)}
        aria-label="Search events"
      />
      <label className="flex items-center gap-1 text-sm text-[var(--cc-muted)]">
        Jump
        <input
          type="date"
          className="cc-native-field rounded border border-[var(--cc-border)] px-2 py-1 text-sm"
          value={props.jumpDate}
          onChange={(e) => props.onJumpDate(e.target.value)}
          aria-label="Jump to date"
        />
      </label>
    </div>
  )
}
