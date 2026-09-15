import type { RecurrenceEditScope } from '@continuum/shared'

export function RecurrenceScopeDialog({
  onPick,
  onCancel,
}: {
  onPick: (scope: RecurrenceEditScope) => void
  onCancel: () => void
}) {
  return (
    <div
      className="absolute inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cc-grid-scope-title"
    >
      <div className="w-full max-w-sm space-y-2 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4">
        <p id="cc-grid-scope-title" className="text-sm font-medium">
          This is a repeating event
        </p>
        <button
          type="button"
          className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
          onClick={() => onPick('this')}
        >
          This event only
        </button>
        <button
          type="button"
          className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
          onClick={() => onPick('following')}
        >
          This and following events
        </button>
        <button
          type="button"
          className="block w-full rounded border border-[var(--cc-border)] px-3 py-2 text-left text-sm"
          onClick={() => onPick('all')}
        >
          All events
        </button>
        <button type="button" className="text-sm underline" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  )
}
