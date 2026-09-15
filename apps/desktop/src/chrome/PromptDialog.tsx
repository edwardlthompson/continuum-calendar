import { useState, useEffect } from 'react'
import type { PromptField } from './promptParse'

export function PromptDialog({
  title,
  fields,
  confirmLabel = 'OK',
  onSubmit,
  onCancel,
}: {
  title: string
  fields: PromptField[]
  confirmLabel?: string
  onSubmit: (values: Record<string, string>) => void
  onCancel: () => void
}) {
  const [values, setValues] = useState<Record<string, string>>(() => {
    const init: Record<string, string> = {}
    for (const f of fields) init[f.name] = f.defaultValue ?? ''
    return init
  })

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.preventDefault()
        onCancel()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onCancel])

  return (
    <div
      className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cc-prompt-title"
    >
      <form
        className="w-full max-w-sm space-y-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4 shadow-xl"
        onSubmit={(e) => {
          e.preventDefault()
          onSubmit(values)
        }}
      >
        <h2 id="cc-prompt-title" className="text-base font-semibold">
          {title}
        </h2>
        {fields.map((f) => (
          <label key={f.name} className="flex flex-col gap-1 text-sm">
            {f.label}
            <input
              className="cc-native-field rounded border border-[var(--cc-border)] px-2 py-1.5"
              type={f.type === 'password' ? 'password' : 'text'}
              value={values[f.name] ?? ''}
              onChange={(e) => setValues((v) => ({ ...v, [f.name]: e.target.value }))}
              autoFocus={f === fields[0]}
            />
          </label>
        ))}
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" className="rounded px-3 py-1.5 text-sm" onClick={onCancel}>
            Cancel
          </button>
          <button
            type="submit"
            className="rounded bg-[var(--cc-accent)] px-3 py-1.5 text-sm font-medium text-white"
          >
            {confirmLabel}
          </button>
        </div>
      </form>
    </div>
  )
}
