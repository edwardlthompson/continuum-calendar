import { useEffect, useState } from 'react'
import type { CalendarEvent, CalendarListEntry, ContactSummary, EventReminder } from '@continuum/shared'
import { searchGoogleContacts } from '../services/googleCalendar'
import { suggestLocations } from '../services/locationSuggest'

interface EventEditorProps {
  initial?: Partial<CalendarEvent>
  calendars: CalendarListEntry[]
  defaultCalendarId: string
  defaultReminderMinutes: number
  onSave: (event: Omit<CalendarEvent, 'id'> & { id?: string }) => void
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
  const [reminderMinutes, setReminderMinutes] = useState(
    initial?.reminders?.[0]?.minutes ?? defaultReminderMinutes,
  )
  const [attendeeQuery, setAttendeeQuery] = useState('')
  const [attendees, setAttendees] = useState(initial?.attendees ?? [])
  const [suggestions, setSuggestions] = useState<ContactSummary[]>([])
  const [locationHits, setLocationHits] = useState<string[]>([])

  const selectedCal = writable.find((c) => c.id === calendarId) ?? writable[0]

  useEffect(() => {
    if (!googleSignedIn || attendeeQuery.trim().length < 2) {
      setSuggestions([])
      return
    }
    const t = window.setTimeout(() => {
      void searchGoogleContacts(attendeeQuery)
        .then(setSuggestions)
        .catch(() => setSuggestions([]))
    }, 250)
    return () => window.clearTimeout(t)
  }, [attendeeQuery, googleSignedIn])

  useEffect(() => {
    const q = location.trim()
    if (q.length < 2) {
      setLocationHits([])
      return
    }
    const t = window.setTimeout(() => {
      void suggestLocations(q).then(setLocationHits).catch(() => setLocationHits([]))
    }, 250)
    return () => window.clearTimeout(t)
  }, [location])

  function submit(e: React.FormEvent) {
    e.preventDefault()
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
    const reminders: EventReminder[] =
      reminderMinutes >= 0 ? [{ minutes: reminderMinutes, method: 'popup' }] : []
    onSave({
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
      reminders,
      etag: initial?.etag,
    })
  }

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4"
    >
      <h2 className="text-lg font-semibold">{initial?.id ? 'Edit event' : 'New event'}</h2>
      <label className="flex flex-col gap-1 text-sm">
        Calendar
        <span className="flex items-center gap-2">
          <span
            className="inline-block h-3 w-3 shrink-0 rounded-full"
            style={{ backgroundColor: selectedCal?.color ?? 'var(--cc-accent)' }}
            aria-hidden
          />
          <select
            className="flex-1 rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
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
      <label className="flex flex-col gap-1 text-sm">
        Title
        <input
          className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </label>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={allDay} onChange={(e) => setAllDay(e.target.checked)} />
        All day
      </label>
      <div className="grid gap-2 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-sm">
          Start
          <input
            type={allDay ? 'date' : 'datetime-local'}
            className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
            value={allDay ? start.slice(0, 10) : start}
            onChange={(e) => setStart(e.target.value)}
            required
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          End
          <input
            type={allDay ? 'date' : 'datetime-local'}
            className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
            value={allDay ? end.slice(0, 10) : end}
            onChange={(e) => setEnd(e.target.value)}
            required
          />
        </label>
      </div>
      <label className="flex flex-col gap-1 text-sm">
        Location
        <input
          className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          autoComplete="off"
        />
        {locationHits.length > 0 && (
          <ul className="max-h-36 overflow-auto rounded border border-[var(--cc-border)] bg-[var(--cc-surface)] text-sm">
            {locationHits.map((hit) => (
              <li key={hit}>
                <button
                  type="button"
                  className="w-full px-2 py-1.5 text-left hover:bg-[var(--cc-accent)]/15"
                  onClick={() => {
                    setLocation(hit)
                    setLocationHits([])
                  }}
                >
                  {hit}
                </button>
              </li>
            ))}
          </ul>
        )}
      </label>
      <label className="flex flex-col gap-1 text-sm">
        Description
        <textarea
          className="min-h-[4rem] rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </label>
      <label className="flex flex-col gap-1 text-sm">
        Reminder (minutes before)
        <input
          type="number"
          min={0}
          className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
          value={reminderMinutes}
          onChange={(e) => setReminderMinutes(Math.max(0, Number(e.target.value) || 0))}
        />
      </label>
      <label className="flex flex-col gap-1 text-sm">
        Attendees
        <input
          className="rounded border border-[var(--cc-border)] bg-transparent px-2 py-1.5"
          placeholder={googleSignedIn ? 'Search contacts…' : 'Sign in for autocomplete'}
          value={attendeeQuery}
          onChange={(e) => setAttendeeQuery(e.target.value)}
          disabled={!googleSignedIn}
        />
      </label>
      {suggestions.length > 0 ? (
        <ul className="max-h-32 overflow-auto rounded border border-[var(--cc-border)] text-sm">
          {suggestions.map((c) => (
            <li key={c.resourceName ?? c.emails[0]}>
              <button
                type="button"
                className="w-full px-2 py-1 text-left hover:bg-[var(--cc-accent-soft)]"
                onClick={() => {
                  setAttendees((prev) => [
                    ...prev,
                    { email: c.emails[0], displayName: c.displayName },
                  ])
                  setAttendeeQuery('')
                  setSuggestions([])
                }}
              >
                {c.displayName} &lt;{c.emails[0]}&gt;
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {attendees.length > 0 ? (
        <ul className="space-y-1 text-xs text-[var(--cc-muted)]">
          {attendees.map((a) => {
            const label = a.displayName ?? a.email
            const status = a.responseStatus
              ? a.responseStatus === 'needsAction'
                ? 'awaiting reply'
                : a.responseStatus
              : null
            return (
              <li key={a.email}>
                {label}
                {status ? ` · ${status}` : ''}
              </li>
            )
          })}
        </ul>
      ) : null}
      <div className="flex flex-wrap justify-end gap-2">
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
    </form>
  )
}
