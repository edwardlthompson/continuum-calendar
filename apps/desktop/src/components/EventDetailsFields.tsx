import type { EventReminder } from '@continuum/shared'
import {
  freqFromRrule,
  monthlyModeFromRrule,
  rruleFromParts,
  untilFromRrule,
  type MonthlyMode,
  type RepeatFreq,
} from '@continuum/shared'

const COLORS = ['', '#0f6e8c', '#d32f2f', '#f9a825', '#43a047', '#8e24aa', '#039be5']

function timeZones(): string[] {
  const local = Intl.DateTimeFormat().resolvedOptions().timeZone
  const supported =
    typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : [local, 'UTC']
  return [local, ...supported.filter((z) => z !== local)].slice(0, 80)
}

export interface EventDetailsValue {
  recurrence?: string[]
  timeZone?: string
  reminders: EventReminder[]
  busy: boolean
  visibility: 'default' | 'public' | 'private'
  color?: string
}

export function EventDetailsFields(props: {
  value: EventDetailsValue
  onChange: (next: EventDetailsValue) => void
  defaultReminderMinutes: number
  start?: string
}) {
  const freq = freqFromRrule(props.value.recurrence)
  const until = untilFromRrule(props.value.recurrence) ?? ''
  const monthly = monthlyModeFromRrule(props.value.recurrence)
  const reminders = [0, 1, 2].map((i) => props.value.reminders[i])

  function setRule(next: RepeatFreq, month: MonthlyMode = monthly) {
    props.onChange({
      ...props.value,
      recurrence: rruleFromParts({
        freq: next,
        until: until || undefined,
        monthly: month,
        start: props.start,
      }),
    })
  }

  function setReminder(index: number, minutes: number) {
    const next = [...props.value.reminders]
    if (minutes < 0) next.splice(index, 1)
    else next[index] = { minutes, method: 'popup' }
    props.onChange({ ...props.value, reminders: next.filter((r) => r && r.minutes >= 0) })
  }

  return (
    <>
      <label className="flex flex-col gap-1 text-sm">
        Repeat
        <select
          className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
          value={freq}
          onChange={(e) => setRule(e.target.value as RepeatFreq)}
        >
          <option value="none">Does not repeat</option>
          <option value="daily">Daily</option>
          <option value="weekly">Weekly</option>
          <option value="monthly">Monthly</option>
          <option value="yearly">Yearly</option>
        </select>
      </label>
      {freq === 'monthly' ? (
        <label className="flex flex-col gap-1 text-sm">
          Monthly on
          <select
            className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
            value={monthly}
            onChange={(e) => setRule('monthly', e.target.value as MonthlyMode)}
          >
            <option value="bydate">Same date each month</option>
            <option value="byweekday">Same weekday (e.g. 2nd Tuesday)</option>
            <option value="last">Last day of the month</option>
          </select>
        </label>
      ) : null}
      {freq !== 'none' ? (
        <label className="flex flex-col gap-1 text-sm">
          Repeat until
          <input
            type="date"
            className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
            value={until}
            onChange={(e) =>
              props.onChange({
                ...props.value,
                recurrence: rruleFromParts({
                  freq,
                  until: e.target.value || undefined,
                  monthly,
                  start: props.start,
                }),
              })
            }
          />
        </label>
      ) : null}
      <label className="flex flex-col gap-1 text-sm">
        Time zone
        <select
          className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
          value={props.value.timeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone}
          onChange={(e) => props.onChange({ ...props.value, timeZone: e.target.value })}
        >
          {timeZones().map((z) => (
            <option key={z} value={z}>
              {z}
            </option>
          ))}
        </select>
      </label>
      {reminders.map((r, i) => (
        <label key={i} className="flex flex-col gap-1 text-sm">
          Reminder {i + 1} (minutes before, blank to skip)
          <input
            type="number"
            min={-1}
            className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
            value={r?.minutes ?? (i === 0 ? props.defaultReminderMinutes : '')}
            onChange={(e) => {
              const raw = e.target.value
              setReminder(i, raw === '' ? -1 : Math.max(0, Number(raw) || 0))
            }}
          />
        </label>
      ))}
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={props.value.busy}
          onChange={(e) => props.onChange({ ...props.value, busy: e.target.checked })}
        />
        Show as busy
      </label>
      <label className="flex flex-col gap-1 text-sm">
        Visibility
        <select
          className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
          value={props.value.visibility}
          onChange={(e) =>
            props.onChange({
              ...props.value,
              visibility: e.target.value as EventDetailsValue['visibility'],
            })
          }
        >
          <option value="default">Default</option>
          <option value="public">Public</option>
          <option value="private">Private</option>
        </select>
      </label>
      <fieldset className="flex flex-col gap-1 text-sm">
        <legend>Event color</legend>
        <div className="flex flex-wrap gap-2">
          {COLORS.map((hex) => (
            <button
              key={hex || 'cal'}
              type="button"
              title={hex || 'Calendar default'}
              className="h-6 w-6 rounded-full border border-[var(--cc-border)]"
              style={{ backgroundColor: hex || 'var(--cc-accent)' }}
              aria-pressed={(props.value.color ?? '') === hex}
              onClick={() => props.onChange({ ...props.value, color: hex || undefined })}
            />
          ))}
        </div>
      </fieldset>
    </>
  )
}
