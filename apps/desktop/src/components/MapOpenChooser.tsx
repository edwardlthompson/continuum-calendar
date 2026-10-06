import { openExternal } from '../about/openExternal'
import {
  MAP_PROVIDERS,
  mapProviderUrl,
  type MapOpenTarget,
  type MapProviderId,
} from '../lib/mapProviders'

export function MapOpenChooser(props: {
  target: MapOpenTarget | null
  onClose: () => void
}) {
  if (!props.target) return null
  const target = props.target
  const label = target.query.trim() || 'this location'

  function choose(id: MapProviderId) {
    void openExternal(mapProviderUrl(id, target))
    props.onClose()
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cc-map-chooser-title"
      onClick={props.onClose}
    >
      <div
        className="w-full max-w-sm space-y-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="cc-map-chooser-title" className="text-base font-semibold text-[var(--cc-text)]">
          Open in maps
        </h2>
        <p className="text-sm text-[var(--cc-muted)] line-clamp-2">{label}</p>
        <div className="flex flex-col gap-2">
          {MAP_PROVIDERS.map((p) => (
            <button
              key={p.id}
              type="button"
              className="rounded-lg border border-[var(--cc-border)] px-3 py-2 text-left text-sm hover:bg-[var(--cc-accent-soft)]"
              onClick={() => choose(p.id)}
            >
              {p.label}
            </button>
          ))}
        </div>
        <div className="flex justify-end pt-1">
          <button type="button" className="rounded px-3 py-1.5 text-sm" onClick={props.onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  )
}
