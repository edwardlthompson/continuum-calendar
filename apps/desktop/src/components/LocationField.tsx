import { useEffect, useRef, useState } from 'react'
import { loadEvents } from '../data/localStore'
import {
  mergeLocationSuggestions,
  recentEventLocations,
  suggestLocations,
} from '../services/locationSuggest'
import { openExternal } from '../about/openExternal'

export function LocationField(props: { value: string; onChange: (next: string) => void }) {
  const [hits, setHits] = useState<string[]>([])
  const pickedRef = useRef(false)

  useEffect(() => {
    if (pickedRef.current) {
      pickedRef.current = false
      return
    }
    const q = props.value.trim()
    const history = recentEventLocations(loadEvents(), q)
    setHits(history)
    if (q.length < 2) return
    const t = window.setTimeout(() => {
      void suggestLocations(q)
        .then((remote) => setHits(mergeLocationSuggestions(history, remote)))
        .catch(() => setHits(history))
    }, 250)
    return () => window.clearTimeout(t)
  }, [props.value])

  return (
    <label className="flex flex-col gap-1 text-sm">
      <span className="flex items-center justify-between gap-2">
        Location
        <button
          type="button"
          className="text-xs text-[var(--cc-accent)] underline"
          disabled={!props.value.trim()}
          onClick={() =>
            void openExternal(
              `https://www.openstreetmap.org/search?query=${encodeURIComponent(props.value.trim())}`,
            )
          }
        >
          Map
        </button>
      </span>
      <input
        className="cc-native-field w-full min-w-0 rounded border border-[var(--cc-border)] px-2 py-1.5"
        value={props.value}
        onChange={(e) => props.onChange(e.target.value)}
        onFocus={() => {
          if (hits.length === 0) {
            setHits(recentEventLocations(loadEvents(), props.value))
          }
        }}
        autoComplete="off"
        placeholder="Start typing an address…"
      />
      {hits.length > 0 ? (
        <ul className="max-h-36 overflow-auto rounded border border-[var(--cc-border)] bg-[var(--cc-surface)] text-sm text-[var(--cc-text)]">
          {hits.map((hit) => (
            <li key={hit}>
              <button
                type="button"
                className="w-full px-2 py-1.5 text-left hover:bg-[var(--cc-accent-soft)]"
                onClick={() => {
                  pickedRef.current = true
                  props.onChange(hit)
                  setHits([])
                }}
              >
                {hit}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </label>
  )
}
