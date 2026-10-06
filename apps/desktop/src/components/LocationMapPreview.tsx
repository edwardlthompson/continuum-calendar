import { useEffect, useState } from 'react'
import {
  geocodeLocation,
  osmStaticMapUrl,
  type GeocodedPlace,
} from '../services/locationSuggest'
import { useMapOpenChooser } from './useMapOpenChooser'

export function LocationMapPreview(props: { location: string }) {
  const query = props.location.trim()
  const [place, setPlace] = useState<GeocodedPlace | null>(null)
  const [status, setStatus] = useState<'idle' | 'loading' | 'ready' | 'missing'>('idle')
  const [imgFailed, setImgFailed] = useState(false)
  const { openMap, mapChooser } = useMapOpenChooser()

  useEffect(() => {
    if (!query) {
      setPlace(null)
      setStatus('idle')
      return
    }
    let cancelled = false
    setStatus('loading')
    setImgFailed(false)
    void geocodeLocation(query).then((hit) => {
      if (cancelled) return
      if (hit) {
        setPlace(hit)
        setStatus('ready')
      } else {
        setPlace(null)
        setStatus('missing')
      }
    })
    return () => {
      cancelled = true
    }
  }, [query])

  if (!query) return null

  if (status === 'loading') {
    return <p className="text-xs text-[var(--cc-muted)]">Loading map…</p>
  }

  if (status === 'missing' || !place) {
    return (
      <div className="relative">
        <button
          type="button"
          className="text-left text-xs text-[var(--cc-accent)] underline"
          onClick={() => openMap({ query })}
        >
          Open in maps…
        </button>
        {mapChooser}
      </div>
    )
  }

  const staticUrl = osmStaticMapUrl(place.lat, place.lon)

  return (
    <div className="relative flex flex-col gap-1">
      <button
        type="button"
        className="group relative block w-full overflow-hidden rounded-lg border border-[var(--cc-border)] text-left"
        aria-label={`Open map for ${place.label}`}
        onClick={() => openMap({ query: place.label || query, lat: place.lat, lon: place.lon })}
      >
        {!imgFailed ? (
          <img
            src={staticUrl}
            alt={`Map of ${place.label}`}
            className="h-[180px] w-full object-cover"
            loading="lazy"
            onError={() => setImgFailed(true)}
          />
        ) : (
          <div className="flex h-[180px] w-full items-center justify-center bg-[var(--cc-accent-soft)] text-sm text-[var(--cc-muted)]">
            Open in maps…
          </div>
        )}
      </button>
      <p className="text-[10px] text-[var(--cc-muted)]">© OpenStreetMap contributors</p>
      {mapChooser}
    </div>
  )
}
