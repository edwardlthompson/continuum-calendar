import type { CalendarEvent, CalendarListEntry } from '@continuum/shared'
import { freqFromRrule } from '@continuum/shared'
import { LinkifiedText } from './LinkifiedText'
import { LocationMapPreview } from './LocationMapPreview'
import { useMapOpenChooser } from './useMapOpenChooser'

const FREQ_LABEL: Record<string, string> = {
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
  yearly: 'Yearly',
}

function formatWhen(
  event: Partial<CalendarEvent>,
  use24HourFormat: boolean,
): string {
  const start = event.start
  const end = event.end
  if (!start) return 'No time set'
  const timeOpts: Intl.DateTimeFormatOptions = {
    hour: use24HourFormat ? '2-digit' : 'numeric',
    minute: '2-digit',
    hour12: !use24HourFormat,
  }
  if (event.allDay) {
    const s = new Date(`${start.slice(0, 10)}T12:00:00`)
    const e = end ? new Date(`${end.slice(0, 10)}T12:00:00`) : s
    const sameDay = start.slice(0, 10) === (end ?? start).slice(0, 10)
    if (sameDay) {
      return `${s.toLocaleDateString(undefined, {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric',
      })} · All day`
    }
    return `${s.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    })} – ${e.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })} · All day`
  }
  const s = new Date(start)
  const e = end ? new Date(end) : null
  if (Number.isNaN(s.getTime())) return start
  const datePart = s.toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
  const startTime = s.toLocaleTimeString(undefined, timeOpts)
  if (!e || Number.isNaN(e.getTime())) return `${datePart} · ${startTime}`
  const endTime = e.toLocaleTimeString(undefined, timeOpts)
  return `${datePart} · ${startTime} – ${endTime}`
}

function formatReminder(minutes: number): string {
  if (minutes === 0) return 'At time of event'
  if (minutes < 60) return `${minutes} min before`
  if (minutes % 60 === 0) return `${minutes / 60} hr before`
  return `${minutes} min before`
}

function responseLabel(status?: string): string {
  switch (status) {
    case 'accepted':
      return 'Accepted'
    case 'declined':
      return 'Declined'
    case 'tentative':
      return 'Maybe'
    default:
      return 'Needs action'
  }
}

export function EventDetailCard(props: {
  event: Partial<CalendarEvent>
  calendars: CalendarListEntry[]
  use24HourFormat: boolean
  onEdit: () => void
  onClose: () => void
}) {
  const { event, calendars, use24HourFormat, onEdit, onClose } = props
  const { openMap, mapChooser } = useMapOpenChooser()
  const cal =
    calendars.find((c) => c.id === event.calendarId) ??
    calendars.find((c) => c.logicalId === event.calendarId)
  const canEdit =
    !event.readOnly &&
    cal?.writable !== false &&
    cal?.source !== 'holidays' &&
    event.source !== 'holidays'
  const location = event.location?.trim()
  const description = event.description?.trim()
  const attendees = event.attendees ?? []
  const reminders = event.reminders ?? []
  const freq = freqFromRrule(event.recurrence)
  const when = formatWhen(event, use24HourFormat)

  return (
    <div className="relative flex h-full min-h-0 w-full min-w-0 flex-col overflow-hidden rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)]">
      <div className="flex shrink-0 items-start gap-3 border-b border-[var(--cc-border)] px-4 py-3">
        <span
          className="mt-1.5 inline-block h-3 w-3 shrink-0 rounded-full"
          style={{ backgroundColor: event.color || cal?.color || 'var(--cc-accent)' }}
          aria-hidden
        />
        <div className="min-w-0 flex-1">
          <p className="text-xs text-[var(--cc-muted)]">{cal?.displayName ?? 'Calendar'}</p>
          <h2 className="text-lg font-semibold leading-snug text-[var(--cc-text)]">
            {event.title?.trim() || '(No title)'}
          </h2>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          {canEdit ? (
            <button
              type="button"
              className="rounded-lg p-2 text-[var(--cc-muted)] hover:bg-[var(--cc-accent-soft)] hover:text-[var(--cc-text)]"
              aria-label="Edit event"
              title="Edit event"
              onClick={onEdit}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
                <path
                  d="M4 20h4l10.5-10.5a2.121 2.121 0 0 0-3-3L5 17v3z"
                  stroke="currentColor"
                  strokeWidth="1.75"
                  strokeLinejoin="round"
                />
                <path
                  d="M13.5 6.5l3 3"
                  stroke="currentColor"
                  strokeWidth="1.75"
                  strokeLinecap="round"
                />
              </svg>
            </button>
          ) : null}
          <button
            type="button"
            className="rounded-lg px-2 py-1.5 text-sm text-[var(--cc-muted)] hover:bg-[var(--cc-accent-soft)] hover:text-[var(--cc-text)]"
            aria-label="Close"
            onClick={onClose}
          >
            ✕
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 space-y-4 overflow-y-auto p-4">
        <section>
          <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
            When
          </h3>
          <p className="text-sm text-[var(--cc-text)]">{when}</p>
          {event.timeZone ? (
            <p className="mt-0.5 text-xs text-[var(--cc-muted)]">{event.timeZone}</p>
          ) : null}
        </section>

        {location ? (
          <section className="space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
              Location
            </h3>
            <button
              type="button"
              className="text-left text-sm text-[var(--cc-accent)] underline underline-offset-2"
              onClick={() => openMap({ query: location })}
            >
              {location}
            </button>
            <LocationMapPreview location={location} />
          </section>
        ) : null}

        {description ? (
          <section>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
              Description
            </h3>
            <div className="text-sm text-[var(--cc-text)]">
              <LinkifiedText text={description} />
            </div>
          </section>
        ) : null}

        {attendees.length > 0 ? (
          <section>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
              Guests
            </h3>
            <ul className="space-y-1 text-sm">
              {attendees.map((a) => (
                <li key={a.email} className="flex flex-wrap items-baseline gap-2">
                  <span>{a.displayName?.trim() || a.email}</span>
                  {a.displayName ? (
                    <span className="text-xs text-[var(--cc-muted)]">{a.email}</span>
                  ) : null}
                  <span className="text-xs text-[var(--cc-muted)]">
                    {responseLabel(a.responseStatus)}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {reminders.length > 0 ? (
          <section>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
              Reminders
            </h3>
            <ul className="space-y-0.5 text-sm text-[var(--cc-text)]">
              {reminders.map((r, i) => (
                <li key={`${r.minutes}-${r.method}-${i}`}>
                  {formatReminder(r.minutes)}
                  {r.method !== 'popup' ? ` · ${r.method}` : ''}
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {freq !== 'none' ? (
          <section>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
              Repeat
            </h3>
            <p className="text-sm">{FREQ_LABEL[freq] ?? freq}</p>
          </section>
        ) : null}

        {event.busy === false || (event.visibility && event.visibility !== 'default') ? (
          <section className="flex flex-wrap gap-3 text-xs text-[var(--cc-muted)]">
            {event.busy === false ? <span>Show as free</span> : null}
            {event.visibility && event.visibility !== 'default' ? (
              <span className="capitalize">{event.visibility}</span>
            ) : null}
          </section>
        ) : null}
      </div>

      <div className="flex shrink-0 justify-end border-t border-[var(--cc-border)] px-4 py-3">
        <button
          type="button"
          className="rounded px-3 py-1.5 text-sm hover:bg-[var(--cc-accent-soft)]"
          onClick={onClose}
        >
          Close
        </button>
      </div>
      {mapChooser}
    </div>
  )
}
