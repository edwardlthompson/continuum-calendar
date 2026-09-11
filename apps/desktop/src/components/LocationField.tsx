import { useEffect, useId, useRef, useState } from 'react'
import { loadEvents } from '../data/localStore'
import {
  mergeLocationSuggestions,
  mapsSearchUrl,
  recentEventLocations,
  suggestLocations,
} from '../services/locationSuggest'
import { openExternal } from '../about/openExternal'

export function LocationField(props: { value: string; onChange: (next: string) => void }) {
  const [hits, setHits] = useState<string[]>([])
  const pickedRef = useRef(false)
  const locId = useId()
  const mapUrl = mapsSearchUrl(props.value)

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
    <div className="flex flex-col gap-1 text-sm">
      <div className="flex items-center justify-between gap-2">
        <span id={locId}>Location</span>
        <button
          type="button"
          className="text-xs text-[var(--cc-accent)] underline"
          disabled={!mapUrl}
          onClick={() => {
            if (mapUrl) void openExternal(mapUrl)
          }}
        >
          Map
        </button>
      </div>
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
        aria-labelledby={locId}
        placeholder="Start typing an address…"
      />
      {hits.length > 0 ? (
        <ul className="max-h-36 overflow-auto rounded border border-[var(--cc-border)] bg-[var(--cc-surface)] text-sm text-[var(--cc-text)]">
          {hits.map((hit) => (
            <li key={hit}>
              <button
                type="button"
                className="w-full px-2 py-1.5 text-left hover:bg-[var(--cc-accent-soft)]"
                onClick={(e) => {
                  e.preventDefault()
                  e.stopPropagation()
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
    </div>
  )
}
