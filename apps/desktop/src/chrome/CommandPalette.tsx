import { useRef, useState } from 'react'
import { filterPaletteCommands, type PaletteCommandId } from './commandPalette'
import { useDismiss } from './useDismiss'

export function CommandPalette(props: {
  onClose: () => void
  onRun: (id: PaletteCommandId) => void
}) {
  const root = useRef<HTMLDivElement>(null)
  const [query, setQuery] = useState('')
  const hits = filterPaletteCommands(query)
  useDismiss(true, props.onClose, root)

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/40 pt-[15vh]" role="presentation">
      <div
        ref={root}
        role="dialog"
        aria-label="Command palette"
        className="w-full max-w-md rounded-lg border border-[var(--cc-border)] bg-[var(--cc-surface)] p-2 shadow-lg"
      >
        <input
          autoFocus
          className="cc-native-field mb-2 w-full rounded border border-[var(--cc-border)] px-2 py-1.5"
          aria-label="Filter commands"
          placeholder="Search commands"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && hits[0]) {
              e.preventDefault()
              props.onRun(hits[0].id)
            }
          }}
        />
        {hits.length === 0 ? (
          <p className="px-2 py-1.5 text-sm text-[var(--cc-muted)]">No matching commands</p>
        ) : (
          <ul role="listbox" aria-label="Commands" className="max-h-64 overflow-auto">
            {hits.map((c) => (
              <li key={c.id} role="option">
                <button
                  type="button"
                  className="block w-full rounded px-2 py-1.5 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
                  onClick={() => props.onRun(c.id)}
                >
                  {c.label}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
