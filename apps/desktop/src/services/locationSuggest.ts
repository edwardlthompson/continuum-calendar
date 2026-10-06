/** Live place labels for the event location field (Photon / OSM). */

export interface PhotonProperties {
  name?: string
  housenumber?: string
  street?: string
  city?: string
  locality?: string
  state?: string
  country?: string
}

export interface GeocodedPlace {
  label: string
  lat: number
  lon: number
}

export function formatPhotonProperties(p: PhotonProperties): string {
  const street = [p.housenumber, p.street].filter(Boolean).join(' ')
  return [p.name || street, p.city || p.locality, p.state, p.country]
    .map((s) => (s ?? '').trim())
    .filter(Boolean)
    .filter((v, i, a) => a.indexOf(v) === i)
    .join(', ')
}

export function parsePhotonFeatures(raw: unknown): string[] {
  if (!raw || typeof raw !== 'object') return []
  const features = (raw as { features?: unknown }).features
  if (!Array.isArray(features)) return []
  const out: string[] = []
  for (const f of features) {
    if (!f || typeof f !== 'object') continue
    const p = (f as { properties?: PhotonProperties }).properties ?? {}
    const line = formatPhotonProperties(p)
    if (line && !out.includes(line)) out.push(line)
  }
  return out
}

/** Parse Photon GeoJSON features into labeled coordinates (lon, lat order in geometry). */
export function parsePhotonPlaces(raw: unknown): GeocodedPlace[] {
  if (!raw || typeof raw !== 'object') return []
  const features = (raw as { features?: unknown }).features
  if (!Array.isArray(features)) return []
  const out: GeocodedPlace[] = []
  const seen = new Set<string>()
  for (const f of features) {
    if (!f || typeof f !== 'object') continue
    const p = (f as { properties?: PhotonProperties }).properties ?? {}
    const line = formatPhotonProperties(p)
    if (!line || seen.has(line)) continue
    const coords = (f as { geometry?: { coordinates?: unknown } }).geometry?.coordinates
    if (!Array.isArray(coords) || coords.length < 2) continue
    const lon = Number(coords[0])
    const lat = Number(coords[1])
    if (!Number.isFinite(lat) || !Number.isFinite(lon)) continue
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue
    seen.add(line)
    out.push({ label: line, lat, lon })
  }
  return out
}

/** @deprecated Prefer mapProviders + MapOpenChooser. Kept for callers expecting a single Google URL. */
export function mapsSearchUrl(query: string): string | null {
  const q = query.trim()
  if (!q) return null
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`
}

export function mergeLocationSuggestions(history: string[], remote: string[], limit = 12): string[] {
  const out: string[] = []
  for (const item of [...history, ...remote]) {
    const line = item.trim()
    if (!line || out.includes(line)) continue
    out.push(line)
    if (out.length >= limit) break
  }
  return out
}

export function recentEventLocations(
  events: Array<{ location?: string }>,
  query: string,
  limit = 8,
): string[] {
  const q = query.trim().toLowerCase()
  const seen = new Set<string>()
  const out: string[] = []
  for (const ev of events) {
    const loc = ev.location?.trim()
    if (!loc || seen.has(loc)) continue
    if (q && !loc.toLowerCase().includes(q)) continue
    seen.add(loc)
    out.push(loc)
    if (out.length >= limit) break
  }
  return out
}

async function fetchPhotonBrowser(query: string, limit: number): Promise<unknown> {
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=${limit}`
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), 4_000)
  try {
    const res = await fetch(url, { signal: ctrl.signal })
    if (!res.ok) return null
    return await res.json()
  } catch {
    return null
  } finally {
    clearTimeout(timer)
  }
}

async function fetchPhotonRaw(query: string, limit: number): Promise<unknown> {
  const q = query.trim()
  if (q.length < 2) return null
  try {
    if (typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window) {
      const { invoke } = await import('@tauri-apps/api/core')
      const raw = await invoke<string>('suggest_locations', { query: q })
      try {
        return JSON.parse(raw) as unknown
      } catch {
        return null
      }
    }
  } catch {
    /* Vite / denied command: browser fetch */
  }
  return fetchPhotonBrowser(q, limit)
}

export async function suggestLocations(query: string, limit = 8): Promise<string[]> {
  const raw = await fetchPhotonRaw(query, limit)
  return parsePhotonFeatures(raw)
}

/** Geocode a place query; returns the first Photon hit with coordinates. */
export async function geocodeLocation(query: string): Promise<GeocodedPlace | null> {
  const raw = await fetchPhotonRaw(query, 1)
  return parsePhotonPlaces(raw)[0] ?? null
}

export function osmSearchUrl(query: string): string {
  return `https://www.openstreetmap.org/search?query=${encodeURIComponent(query.trim())}`
}

export function osmMapUrl(lat: number, lon: number, zoom = 15): string {
  return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=${zoom}/${lat}/${lon}`
}

/** FOSS static map preview (OpenStreetMap.de staticmap). */
export function osmStaticMapUrl(lat: number, lon: number, width = 400, height = 180, zoom = 14): string {
  const w = Math.min(800, Math.max(100, Math.round(width)))
  const h = Math.min(600, Math.max(80, Math.round(height)))
  return `https://staticmap.openstreetmap.de/staticmap.php?center=${lat},${lon}&zoom=${zoom}&size=${w}x${h}&markers=${lat},${lon},red-pushpin`
}
