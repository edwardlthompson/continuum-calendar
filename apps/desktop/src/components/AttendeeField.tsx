import { useEffect, useState } from 'react'
import type { CalendarAttendee, ContactSummary } from '@continuum/shared'
import { searchGoogleContacts } from '../services/googleCalendar'

export function AttendeeField(props: {
  attendees: CalendarAttendee[]
  onChange: (next: CalendarAttendee[]) => void
  googleSignedIn: boolean
}) {
  const [query, setQuery] = useState('')
  const [hits, setHits] = useState<ContactSummary[]>([])

  useEffect(() => {
    if (!props.googleSignedIn || query.trim().length < 2) {
      setHits([])
      return
    }
    const t = window.setTimeout(() => {
      void searchGoogleContacts(query)
        .then(setHits)
        .catch(() => setHits([]))
    }, 250)
    return () => window.clearTimeout(t)
  }, [query, props.googleSignedIn])

  function add(email: string, displayName?: string) {
    const clean = email.trim()
    if (!clean || props.attendees.some((a) => a.email === clean)) return
    props.onChange([...props.attendees, { email: clean, displayName }])
    setQuery('')
    setHits([])
  }

  return (
    <label className="flex flex-col gap-1 text-sm">
      Attendees
      <input
        className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
        placeholder={props.googleSignedIn ? 'Search contacts or type an email…' : 'Type an email…'}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && query.includes('@')) {
            e.preventDefault()
            add(query)
          }
        }}
      />
      {hits.length > 0 ? (
        <ul className="max-h-32 overflow-auto rounded border border-[var(--cc-border)] text-sm">
          {hits.map((c) => (
            <li key={c.resourceName ?? c.emails[0]}>
              <button
                type="button"
                className="w-full px-2 py-1 text-left hover:bg-[var(--cc-accent-soft)]"
                onClick={() => add(c.emails[0], c.displayName)}
              >
                {c.displayName} &lt;{c.emails[0]}&gt;
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {props.attendees.length > 0 ? (
        <ul className="space-y-1 text-xs text-[var(--cc-muted)]">
          {props.attendees.map((a) => {
            const label = a.displayName ?? a.email
            const status =
              a.responseStatus === 'needsAction'
                ? 'awaiting reply'
                : (a.responseStatus ?? null)
            return (
              <li key={a.email} className="flex items-center justify-between gap-2">
                <span>
                  {label}
                  {status ? ` · ${status}` : ''}
                </span>
                <button
                  type="button"
                  className="text-[var(--cc-accent)] underline"
                  onClick={() => props.onChange(props.attendees.filter((x) => x.email !== a.email))}
                >
                  Remove
                </button>
              </li>
            )
          })}
        </ul>
      ) : null}
    </label>
  )
}
