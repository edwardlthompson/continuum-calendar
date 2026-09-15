import { useEffect, useId, useRef, useState } from 'react'
import {
  isIsoDate,
  joinDateTime,
  monthCells,
  shiftMonth,
  splitDateTime,
  weekdayLabels,
} from '../utils/dateTimeLocal'
import { fieldClass, FloatingPanel, TimeSelects } from './DateTimePopovers'

function DateCalendar({
  value,
  firstDayOfWeek,
  onPick,
  onClose,
  anchor,
}: {
  value: string
  firstDayOfWeek: number
  onPick: (iso: string) => void
  onClose: () => void
  anchor: HTMLElement | null
}) {
  const headingId = useId()
  const parsed = isIsoDate(value) ? value : ''
  const now = new Date()
  const initial = parsed ? new Date(`${parsed}T12:00:00`) : now
  const [year, setYear] = useState(initial.getFullYear())
  const [month0, setMonth0] = useState(initial.getMonth())
  const cells = monthCells(year, month0, firstDayOfWeek)
  const heading = new Date(year, month0, 1).toLocaleString(undefined, { month: 'long', year: 'numeric' })

  return (
    <FloatingPanel anchor={anchor} onClose={onClose} widthClass="w-[17rem]" labelledBy={headingId}>
      <div className="mb-2 flex items-center justify-between gap-1">
        <button
          type="button"
          className="rounded px-2 py-1 text-sm hover:bg-[var(--cc-accent-soft)]"
          aria-label="Previous month"
          onClick={() => {
            const next = shiftMonth(year, month0, -1)
            setYear(next.year)
            setMonth0(next.month0)
          }}
        >
          ‹
        </button>
        <p id={headingId} className="text-sm font-medium">
          {heading}
        </p>
        <button
          type="button"
          className="rounded px-2 py-1 text-sm hover:bg-[var(--cc-accent-soft)]"
          aria-label="Next month"
          onClick={() => {
            const next = shiftMonth(year, month0, 1)
            setYear(next.year)
            setMonth0(next.month0)
          }}
        >
          ›
        </button>
      </div>
      <div className="grid grid-cols-7 gap-0.5 text-center text-[0.7rem] text-[var(--cc-muted)]">
        {weekdayLabels(firstDayOfWeek).map((w) => (
          <span key={w}>{w}</span>
        ))}
      </div>
      <div className="mt-1 grid grid-cols-7 gap-0.5">
        {cells.map((cell, i) =>
          cell ? (
            <button
              key={cell.iso}
              type="button"
              className={`rounded py-1 text-sm hover:bg-[var(--cc-accent-soft)] ${
                cell.iso === parsed ? 'bg-[var(--cc-accent)] text-[var(--cc-on-accent)]' : ''
              }`}
              onClick={() => onPick(cell.iso)}
            >
              {cell.day}
            </button>
          ) : (
            <span key={`e-${i}`} />
          ),
        )}
      </div>
    </FloatingPanel>
  )
}

export function DateOnlyField({
  value,
  onChange,
  required,
  firstDayOfWeek = 0,
  labelledBy,
  ariaLabel,
  anchorRef,
}: {
  value: string
  onChange: (isoDate: string) => void
  required?: boolean
  firstDayOfWeek?: number
  labelledBy?: string
  ariaLabel?: string
  /** Calendar opens below this node so it does not cover sibling time controls. */
  anchorRef?: React.RefObject<HTMLElement | null>
}) {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState(value.slice(0, 10))
  const wrapRef = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    setDraft(value.slice(0, 10))
  }, [value])

  function commit(raw: string) {
    const next = raw.slice(0, 10)
    setDraft(next)
    if (isIsoDate(next)) onChange(next)
  }

  return (
    <span ref={wrapRef} className="flex min-w-0 items-center gap-1">
      <input
        type="text"
        inputMode="numeric"
        autoComplete="off"
        spellCheck={false}
        placeholder="YYYY-MM-DD"
        className={`${fieldClass} w-full min-w-[11ch] tabular-nums`}
        aria-labelledby={labelledBy}
        aria-label={ariaLabel}
        required={required}
        value={draft}
        onChange={(e) => commit(e.target.value)}
        onBlur={() => {
          if (value.slice(0, 10) !== draft && !isIsoDate(draft)) setDraft(value.slice(0, 10))
        }}
      />
      <button
        type="button"
        className="shrink-0 rounded border border-[var(--cc-border)] px-2 py-1.5 text-sm"
        aria-label="Open calendar"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        Pick
      </button>
      {open ? (
        <DateCalendar
          value={isIsoDate(draft) ? draft : value.slice(0, 10)}
          firstDayOfWeek={firstDayOfWeek}
          anchor={anchorRef?.current ?? wrapRef.current}
          onClose={() => setOpen(false)}
          onPick={(iso) => {
            commit(iso)
            setOpen(false)
          }}
        />
      ) : null}
    </span>
  )
}

export function DateTimeLocalField({
  label,
  allDay,
  value,
  onChange,
  required,
  firstDayOfWeek = 0,
}: {
  label: string
  allDay: boolean
  value: string
  onChange: (next: string) => void
  required?: boolean
  firstDayOfWeek?: number
}) {
  const labelId = useId()
  const blockRef = useRef<HTMLDivElement>(null)
  const { date, time } = splitDateTime(value)

  return (
    <div ref={blockRef} className="flex min-w-0 flex-col gap-1 text-sm">
      <span id={labelId}>{label}</span>
      <span className="flex min-w-0 flex-col gap-1">
        <DateOnlyField
          value={date}
          onChange={(nextDate) => onChange(joinDateTime(nextDate, time, allDay))}
          required={required}
          firstDayOfWeek={firstDayOfWeek}
          labelledBy={labelId}
          anchorRef={blockRef}
        />
        {allDay ? null : (
          <TimeSelects
            time={time}
            onChange={(nextTime) => onChange(joinDateTime(date, nextTime, false))}
            labelledBy={labelId}
            name={label}
          />
        )}
      </span>
    </div>
  )
}

export function TimeOnlyField({
  value,
  onChange,
  ariaLabel,
}: {
  value: string
  onChange: (hhmm: string) => void
  ariaLabel: string
}) {
  const labelId = useId()
  return (
    <span className="inline-flex items-center gap-1">
      <span id={labelId} className="sr-only">
        {ariaLabel}
      </span>
      <TimeSelects time={value.slice(0, 5) || '09:00'} onChange={onChange} labelledBy={labelId} name={ariaLabel} />
    </span>
  )
}
