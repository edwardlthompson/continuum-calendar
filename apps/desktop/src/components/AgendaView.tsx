import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  CONTINUUM_OPEN_DAY_COLOR,
  buildAgendaSections,
  dayShouldShowOpen,
  formatAgendaSectionTitle,
  isEventPast,
  localDateKey,
  splitDayEventsAtNow,
  todayAgendaPhase,
  eventOccurrenceKey,
  type CalendarEvent,
  type CalendarListEntry,
  type WorkingHours,
} from '@continuum/shared'
import { formatEventTimeRange } from '../utils/timeFormat'

interface AgendaViewProps {
  events: CalendarEvent[]
  calendars: CalendarListEntry[]
  showEmptyDays: boolean
  rangeDays: number
  redactTitles: boolean
  density: 'compact' | 'comfortable'
  use24HourFormat?: boolean
  workingHours: WorkingHours
  conflictIds?: Set<string>
  onSelectEvent?: (event: CalendarEvent) => void
  /** Empty “Open” day row — schedule something in that slot. */
  onOpenDay?: (dateKey: string) => void
}

function displayTitle(title: string, redact: boolean): string {
  return redact ? '••••••••' : title
}

function addDays(dateKey: string, n: number): string {
  const d = new Date(`${dateKey}T12:00:00`)
  d.setDate(d.getDate() + n)
  return localDateKey(d.getTime())
}

export function AgendaView({
  events,
  calendars,
  showEmptyDays,
  rangeDays,
  redactTitles,
  density,
  use24HourFormat = false,
  workingHours,
  conflictIds,
  onSelectEvent,
  onOpenDay,
}: AgendaViewProps) {
  const [nowMs, setNowMs] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setNowMs(Date.now()), 30_000)
    return () => window.clearInterval(id)
  }, [])

  const start = localDateKey(nowMs)
  const end = useMemo(() => addDays(start, rangeDays), [start, rangeDays])

  const sections = useMemo(
    () => buildAgendaSections(events, start, end, showEmptyDays),
    [events, start, end, showEmptyDays],
  )

  const colorByCal = useMemo(() => {
    const m = new Map<string, string>()
    for (const c of calendars) m.set(c.id, c.color)
    return m
  }, [calendars])

  const pad = density === 'compact' ? 'py-1' : 'py-2'
  const workEnd = workingHours.end || '17:00'

  return (
    <div className="h-full overflow-auto rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-3">
      <ul className="space-y-3">
        {sections.map((section) => {
          const isToday = section.date === start
          const header = formatAgendaSectionTitle(section.date, start)
          const phase = isToday
            ? todayAgendaPhase(section.events, nowMs, workEnd, start)
            : null

          const showOpen =
            showEmptyDays &&
            !(isToday && phase === 'empty') &&
            dayShouldShowOpen(section.events)

          let body: ReactNode
          if (isToday && phase === 'empty') {
            body = null
          } else if (isToday && phase === 'open') {
            body = <OpenDayButton dateKey={section.date} pad={pad} onOpenDay={onOpenDay} />
          } else if (!isToday && section.isEmpty) {
            body = <OpenDayButton dateKey={section.date} pad={pad} onOpenDay={onOpenDay} />
          } else if (isToday && phase === 'active') {
            const { past, future } = splitDayEventsAtNow(section.events, nowMs)
            body = (
              <ul className="space-y-1">
                {past.map((ev) => (
                  <EventRow
                    key={`${ev.calendarId}-${ev.id}`}
                    ev={ev}
                    bar={colorByCal.get(ev.calendarId) ?? CONTINUUM_OPEN_DAY_COLOR}
                    pad={pad}
                    redactTitles={redactTitles}
                    use24HourFormat={use24HourFormat}
                    dimmed
                    conflict={conflictIds?.has(eventOccurrenceKey(ev))}
                    onSelectEvent={onSelectEvent}
                  />
                ))}
                <li
                  className="my-1 h-0.5 w-full rounded-full bg-red-600"
                  aria-hidden
                  role="separator"
                />
                {future.map((ev) => (
                  <EventRow
                    key={`${ev.calendarId}-${ev.id}`}
                    ev={ev}
                    bar={colorByCal.get(ev.calendarId) ?? CONTINUUM_OPEN_DAY_COLOR}
                    pad={pad}
                    redactTitles={redactTitles}
                    use24HourFormat={use24HourFormat}
                    dimmed={false}
                    conflict={conflictIds?.has(eventOccurrenceKey(ev))}
                    onSelectEvent={onSelectEvent}
                  />
                ))}
                {showOpen ? (
                  <li>
                    <OpenDayButton dateKey={section.date} pad={pad} onOpenDay={onOpenDay} />
                  </li>
                ) : null}
              </ul>
            )
          } else {
            body = (
              <ul className="space-y-1">
                {section.events.map((ev) => (
                  <EventRow
                    key={`${ev.calendarId}-${ev.id}`}
                    ev={ev}
                    bar={colorByCal.get(ev.calendarId) ?? CONTINUUM_OPEN_DAY_COLOR}
                    pad={pad}
                    redactTitles={redactTitles}
                    use24HourFormat={use24HourFormat}
                    dimmed={isEventPast(ev, nowMs)}
                    conflict={conflictIds?.has(eventOccurrenceKey(ev))}
                    onSelectEvent={onSelectEvent}
                  />
                ))}
                {showOpen ? (
                  <li>
                    <OpenDayButton dateKey={section.date} pad={pad} onOpenDay={onOpenDay} />
                  </li>
                ) : null}
              </ul>
            )
          }

          return (
            <li key={section.date}>
              <h3
                className={`mb-1 text-sm font-semibold ${
                  isToday ? 'text-[var(--cc-accent)]' : 'text-[var(--cc-text)]'
                }`}
              >
                {header}
              </h3>
              {body}
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function OpenDayButton({
  dateKey,
  pad,
  onOpenDay,
}: {
  dateKey: string
  pad: string
  onOpenDay?: (dateKey: string) => void
}) {
  return (
    <button
      type="button"
      className={`w-full rounded-md px-1 text-left text-sm font-medium hover:bg-[var(--cc-accent-soft)] ${pad}`}
      style={{ color: CONTINUUM_OPEN_DAY_COLOR }}
      onClick={() => onOpenDay?.(dateKey)}
    >
      Open
    </button>
  )
}

function EventRow({
  ev,
  bar,
  pad,
  redactTitles,
  use24HourFormat,
  dimmed,
  conflict,
  onSelectEvent,
}: {
  ev: CalendarEvent
  bar: string
  pad: string
  redactTitles: boolean
  use24HourFormat: boolean
  dimmed: boolean
  conflict?: boolean
  onSelectEvent?: (event: CalendarEvent) => void
}) {
  return (
    <li className={dimmed ? 'opacity-45' : undefined}>
      <button
        type="button"
        className={`flex w-full gap-2 rounded-md px-1 text-left hover:bg-[var(--cc-accent-soft)] ${pad} ${
          conflict ? 'ring-1 ring-amber-400/80 bg-amber-50/50 dark:bg-amber-950/20' : ''
        }`}
        onClick={() => onSelectEvent?.(ev)}
      >
        <span
          className="mt-0.5 w-1 shrink-0 self-stretch rounded-full"
          style={{ backgroundColor: conflict ? '#eab308' : bar, minHeight: '2.25rem' }}
          aria-hidden
        />
        {conflict ? (
          <span
            className="cc-conflict-badge mt-0.5 shrink-0"
            title="Scheduling conflict"
            aria-label="Scheduling conflict"
          >
            ⚠️
          </span>
        ) : null}
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-[var(--cc-text)]">
            {displayTitle(ev.title, redactTitles)}
          </span>
          <span className="block text-xs text-[var(--cc-muted)]">
            {formatEventTimeRange(ev.start, ev.end, use24HourFormat, ev.allDay)}
          </span>
        </span>
      </button>
    </li>
  )
}
