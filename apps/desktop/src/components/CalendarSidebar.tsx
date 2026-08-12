import type { CalendarListEntry, CalendarNotifyPrefs, ContinuumSettings } from '@continuum/shared'
import { resolveCalendarNotifyPrefs } from '@continuum/shared'

interface CalendarSidebarProps {
  calendars: CalendarListEntry[]
  onToggle: (id: string, visible: boolean) => void
  onSetDefaultWrite: (logicalId: string) => void
  defaultWriteCalendarId: string
  calendarNotifyPrefs: ContinuumSettings['calendarNotifyPrefs']
  onNotifyPrefsChange: (logicalId: string, prefs: CalendarNotifyPrefs) => void
}

export function CalendarSidebar({
  calendars,
  onToggle,
  onSetDefaultWrite,
  defaultWriteCalendarId,
  calendarNotifyPrefs,
  onNotifyPrefsChange,
}: CalendarSidebarProps) {
  return (
    <aside className="flex w-64 shrink-0 flex-col gap-2 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-3">
      <h2 className="text-sm font-semibold">Calendars</h2>
      <p className="text-xs text-[var(--cc-muted)]">
        Star sets the default for new events. New / Reminder control peer alerts.
      </p>
      <ul className="space-y-2 text-sm">
        {calendars.map((c) => {
          const isDefault = defaultWriteCalendarId === c.logicalId
          const canWrite = c.writable !== false && c.source !== 'holidays'
          const prefs = resolveCalendarNotifyPrefs(calendarNotifyPrefs, c.logicalId)
          return (
            <li key={c.logicalId} className="flex flex-col gap-1">
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={c.visible}
                  onChange={(e) => onToggle(c.id, e.target.checked)}
                  id={`cal-${c.id}`}
                />
                <span
                  className="inline-block h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ background: c.color }}
                  aria-hidden
                />
                <label htmlFor={`cal-${c.id}`} className="min-w-0 flex-1 truncate">
                  {c.displayName}
                  <span className="ml-1 text-xs text-[var(--cc-muted)]">({c.source})</span>
                </label>
                {canWrite ? (
                  <button
                    type="button"
                    title={
                      isDefault
                        ? 'Default calendar for new events'
                        : 'Set as default for new events'
                    }
                    aria-pressed={isDefault}
                    className={`shrink-0 text-sm leading-none ${
                      isDefault
                        ? 'text-[var(--cc-accent)]'
                        : 'text-[var(--cc-muted)] hover:text-[var(--cc-text)]'
                    }`}
                    onClick={() => onSetDefaultWrite(c.logicalId)}
                  >
                    {isDefault ? '★' : '☆'}
                  </button>
                ) : null}
              </div>
              <div className="ml-6 flex flex-wrap gap-3 text-xs text-[var(--cc-muted)]">
                <label className="inline-flex items-center gap-1">
                  <input
                    type="checkbox"
                    checked={prefs.newEvent}
                    onChange={(e) =>
                      onNotifyPrefsChange(c.logicalId, {
                        ...prefs,
                        newEvent: e.target.checked,
                      })
                    }
                  />
                  New
                </label>
                <label className="inline-flex items-center gap-1">
                  <input
                    type="checkbox"
                    checked={prefs.reminder}
                    onChange={(e) =>
                      onNotifyPrefsChange(c.logicalId, {
                        ...prefs,
                        reminder: e.target.checked,
                      })
                    }
                  />
                  Reminder
                </label>
              </div>
            </li>
          )
        })}
      </ul>
    </aside>
  )
}
