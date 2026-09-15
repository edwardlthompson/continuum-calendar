import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { HOUR_OPTIONS, MINUTE_OPTIONS } from '../utils/dateTimeLocal'

export const fieldClass =
  'cc-native-field min-w-0 rounded border border-[var(--cc-border)] bg-[var(--cc-surface)] px-2 py-1.5 text-sm'

export function FloatingPanel({
  anchor,
  onClose,
  widthClass,
  labelledBy,
  children,
}: {
  anchor: HTMLElement | null
  onClose: () => void
  widthClass: string
  labelledBy?: string
  children: ReactNode
}) {
  const [pos, setPos] = useState({ top: 0, left: 0 })
  const panelRef = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    if (!anchor) return
    const r = anchor.getBoundingClientRect()
    const width = Math.min(272, window.innerWidth - 16)
    const left = Math.min(Math.max(8, r.left), window.innerWidth - width - 8)
    const below = r.bottom + 4
    const estimated = 288
    const top =
      below + estimated > window.innerHeight - 8 ? Math.max(8, r.top - estimated - 4) : below
    setPos({ top, left })
  }, [anchor])

  useEffect(() => {
    const onDoc = (e: PointerEvent) => {
      const t = e.target as Node
      if (panelRef.current?.contains(t) || anchor?.contains(t)) return
      onClose()
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('pointerdown', onDoc)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('pointerdown', onDoc)
      document.removeEventListener('keydown', onKey)
    }
  }, [anchor, onClose])

  return createPortal(
    <div
      ref={panelRef}
      role="dialog"
      aria-labelledby={labelledBy}
      className={`fixed z-[200] ${widthClass} rounded-lg border border-[var(--cc-border)] bg-[var(--cc-surface)] p-2 shadow-lg`}
      style={{ top: pos.top, left: pos.left }}
    >
      {children}
    </div>,
    document.body,
  )
}

function OptionMenu({
  value,
  options,
  onChange,
  labelledBy,
  ariaLabel,
}: {
  value: string
  options: string[]
  onChange: (next: string) => void
  labelledBy: string
  ariaLabel: string
}) {
  const [open, setOpen] = useState(false)
  const btnRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    listRef.current?.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'center' })
  }, [open, value])

  return (
    <span className="relative">
      <button
        ref={btnRef}
        type="button"
        className={`${fieldClass} min-w-[3.25rem]`}
        aria-labelledby={ariaLabel ? undefined : labelledBy}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        {value}
      </button>
      {open ? (
        <FloatingPanel anchor={btnRef.current} onClose={() => setOpen(false)} widthClass="w-[4.5rem] p-1">
          <div ref={listRef} role="listbox" aria-label={ariaLabel} className="max-h-48 overflow-y-auto">
            {options.map((opt) => (
              <button
                key={opt}
                type="button"
                role="option"
                aria-selected={opt === value}
                className={`block w-full rounded px-2 py-1 text-left text-sm hover:bg-[var(--cc-accent-soft)] ${
                  opt === value ? 'bg-[var(--cc-accent)] text-[var(--cc-on-accent)]' : ''
                }`}
                onClick={() => {
                  onChange(opt)
                  setOpen(false)
                }}
              >
                {opt}
              </button>
            ))}
          </div>
        </FloatingPanel>
      ) : null}
    </span>
  )
}

export function TimeSelects({
  time,
  onChange,
  labelledBy,
  name = '',
}: {
  time: string
  onChange: (next: string) => void
  labelledBy: string
  name?: string
}) {
  const [h = '09', m = '00'] = (time || '09:00').split(':')
  const hour = HOUR_OPTIONS.includes(h) ? h : '09'
  const minute = MINUTE_OPTIONS.includes(m) ? m : '00'
  const hourLabel = name ? `${name} hour` : 'Hour'
  const minuteLabel = name ? `${name} minute` : 'Minute'
  return (
    <span className="flex min-w-0 items-center gap-1">
      <OptionMenu
        value={hour}
        options={HOUR_OPTIONS}
        labelledBy={labelledBy}
        ariaLabel={hourLabel}
        onChange={(next) => onChange(`${next}:${minute}`)}
      />
      <span aria-hidden>:</span>
      <OptionMenu
        value={minute}
        options={MINUTE_OPTIONS}
        labelledBy={labelledBy}
        ariaLabel={minuteLabel}
        onChange={(next) => onChange(`${hour}:${next}`)}
      />
    </span>
  )
}
