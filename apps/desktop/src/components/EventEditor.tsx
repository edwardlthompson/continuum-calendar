import { useState } from 'react'
import type { CalendarEvent, CalendarListEntry, RecurrenceEditScope } from '@continuum/shared'
import { freqFromRrule } from '@continuum/shared'
import { AttendeeField } from './AttendeeField'
import { EventDetailsFields, type EventDetailsValue } from './EventDetailsFields'
import { LocationField } from './LocationField'

interface EventEditorProps {
  initial?: Partial<CalendarEvent>
  calendars: CalendarListEntry[]
  defaultCalendarId: string
  defaultReminderMinutes: number
  onSave: (
    event: Omit<CalendarEvent, 'id'> & { id?: string },
    scope?: RecurrenceEditScope,
    occurrenceStart?: string,
  ) => void
  onDelete?: () => void
  onCancel: () => void
  googleSignedIn: boolean
}

function toLocalInput(iso?: string, allDay?: boolean): string {
  if (!iso) return ''
  if (allDay) return iso.slice(0, 10)
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.slice(0, 16)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function EventEditor({
  initial,
  calendars,
  defaultCalendarId,
  defaultReminderMinutes,
  onSave,
  onDelete,
  onCancel,
  googleSignedIn,
}: EventEditorProps) {
  const writable = calendars.filter((c) => c.writable !== false && c.source !== 'holidays')
  const [title, setTitle] = useState(initial?.title ?? '')
  const [allDay, setAllDay] = useState(Boolean(initial?.allDay))
  const [start, setStart] = useState(toLocalInput(initial?.start, initial?.allDay))
  const [end, setEnd] = useState(toLocalInput(initial?.end, initial?.allDay))
  const [calendarId, setCalendarId] = useState(initial?.calendarId ?? defaultCalendarId)
  const [description, setDescription] = useState(initial?.description ?? '')
  const [location, setLocation] = useState(initial?.location ?? '')
  const [attendees, setAttendees] = useState(initial?.attendees ?? [])
  const [details, setDetails] = useState<EventDetailsValue>({
    recurrence: initial?.recurrence,
    timeZone: initial?.timeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone,
    reminders: initial?.reminders ?? [{ minutes: defaultReminderMinutes, method: 'popup' }],
    busy: initial?.busy !== false,
    visibility: initial?.visibility ?? 'default',
    color: initial?.color,
  })

  const [scopeOpen, setScopeOpen] = useState(false)
  const selectedCal = writable.find((c) => c.id === calendarId) ?? writable[0]
  const occurrenceStart = initial?.occurrenceStart
  const isSeries = Boolean(initial?.id && (initial.recurrence?.length || freqFromRrule(details.recurrence) !== 'none'))

  function finishSave(scope?: RecurrenceEditScope) {
    let startIso: string
    let endIso: string
    if (allDay) {
      startIso = start.slice(0, 10)
      endIso = end.slice(0, 10) || startIso
    } else {
      startIso = start.includes('T') ? new Date(start).toISOString() : start
      endIso = end.includes('T') ? new Date(end).toISOString() : end
    }
    const cal = writable.find((c) => c.id === calendarId)
    onSave(
      {
        id: initial?.id,
        calendarId,
        source: cal?.source ?? initial?.source ?? 'local',
        title: title || '(No title)',
        description: description || undefined,
        location: location || undefined,
        start: startIso,
        end: endIso,
        allDay,
        attendees,
        reminders: details.reminders,
        recurrence: details.recurrence,
        timeZone: details.timeZone,
        busy: details.busy,
        visibility: details.visibility,
        color: details.color,
        etag: initial?.etag,
        recurrenceExceptions: initial?.recurrenceExceptions,
      },
      scope,
      occurrenceStart,
    )
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    if (isSeries && occurrenceStart) {
      setScopeOpen(true)
      return
    }
    finishSave(isSeries ? 'all' : undefined)
  }

  return (
    <form
      onSubmit={submit}
      className="relative flex h-full min-h-0 w-full min-w-0 flex-col overflow-hidden rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)]"
    >
      <div className="min-h-0 min-w-0 flex-1 overflow-x-hidden overflow-y-auto p-4">
        <h2 className="mb-3 text-lg font-semibold">{initial?.id ? 'Edit event' : 'New event'}</h2>
        <div className="grid min-w-0 gap-4 lg:grid-cols-2 lg:items-start">
          <div className="flex min-w-0 flex-col gap-3">
            <label className="flex min-w-0 flex-col gap-1 text-sm">
              Calendar
              <span className="flex min-w-0 items-center gap-2">
                <span
                  className="inline-block h-3 w-3 shrink-0 rounded-full"
                  style={{ backgroundColor: selectedCal?.color ?? 'var(--cc-accent)' }}
                  aria-hidden
                />
                <select
                  className="cc-native-field w-full min-w-0 flex-1 rounded border border-[var(--cc-border)] px-2 py-1.5"
                  value={calendarId}
                  onChange={(e) => setCalendarId(e.target.value)}
                >
                  {writable.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName}
                      {c.id === defaultCalendarId ? ' (default)' : ''}
                    </option>
                  ))}
                </select>
              </span>
            </label>
            <label className="flex min-w-0 flex-col gap-1 text-sm">
              Title
              <input
                className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={allDay} onChange={(e) => setAllDay(e.target.checked)} />
              All day
            </label>
            <div className="grid min-w-0 gap-2 sm:grid-cols-2">
              <label className="flex min-w-0 flex-col gap-1 text-sm">
                Start
                <input
                  type={allDay ? 'date' : 'datetime-local'}
                  className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
                  value={allDay ? start.slice(0, 10) : start}
                  onChange={(e) => setStart(e.target.value)}
                  required
                />
              </label>
              <label className="flex min-w-0 flex-col gap-1 text-sm">
                End
                <input
                  type={allDay ? 'date' : 'datetime-local'}
                  className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
                  value={allDay ? end.slice(0, 10) : end}
                  onChange={(e) => setEnd(e.target.value)}
                  required
                />
              </label>
            </div>
            <LocationField value={location} onChange={setLocation} />
            <label className="flex min-w-0 flex-col gap-1 text-sm">
              Description
              <textarea
                className="cc-native-field min-h-[6rem] w-full min-w-0 flex-1 rounded border border-[var(--cc-border)] px-2 py-1.5"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </label>
          </div>
          <div className="flex min-w-0 flex-col gap-3">
            <EventDetailsFields
              value={details}
              onChange={setDetails}
              defaultReminderMinutes={defaultReminderMinutes}
              start={start}
            />
            <AttendeeField attendees={attendees} onChange={setAttendees} googleSignedIn={googleSignedIn} />
          </div>
        </div>
      </div>
      <div className="flex shrink-0 flex-wrap justify-end gap-2 border-t border-[var(--cc-border)] px-4 py-3">
        {initial?.id && onDelete ? (
          <button
            type="button"
            className="mr-auto rounded px-3 py-1.5 text-sm text-red-600"
            onClick={onDelete}
          >
            Delete
          </button>
        ) : null}
        <button type="button" className="rounded px-3 py-1.5 text-sm" onClick={onCancel}>
          Cancel
        </button>
        <button
          type="submit"
          className="rounded bg-[var(--cc-accent)] px-3 py-1.5 text-sm font-medium text-white"
        >
          Save
        </button>
      </div>
      {scopeOpen ? (
        <div className="absolute inset-0 z-30 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-sm space-y-2 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4">
            <p className="text-sm font-medium">This is a repeating event</p>
            <button
              type="button"
              className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
              onClick={() => finishSave('this')}
            >
              This event only
            </button>
            <button
              type="button"
              className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
              onClick={() => finishSave('following')}
            >
              This and following events
            </button>
            <button
              type="button"
              className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
              onClick={() => finishSave('all')}
            >
              All events
            </button>
            <button type="button" className="text-sm underline" onClick={() => setScopeOpen(false)}>
              Cancel
            </button>
          </div>
        </div>
      ) : null}
    </form>
  )
}
