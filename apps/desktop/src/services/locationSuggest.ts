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

async function fetchPhotonBrowser(query: string, limit: number): Promise<string[]> {
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=${limit}`
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), 4_000)
  try {
    const res = await fetch(url, { signal: ctrl.signal })
    if (!res.ok) return []
    return parsePhotonFeatures(await res.json())
  } catch {
    return []
  } finally {
    clearTimeout(timer)
  }
}

export async function suggestLocations(query: string, limit = 8): Promise<string[]> {
  const q = query.trim()
  if (q.length < 2) return []
  try {
    if (typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window) {
      const { invoke } = await import('@tauri-apps/api/core')
      const raw = await invoke<string>('suggest_locations', { query: q })
      try {
        return parsePhotonFeatures(JSON.parse(raw) as unknown)
      } catch {
        return []
      }
    }
  } catch {
    /* Vite / denied command: browser fetch */
  }
  return fetchPhotonBrowser(q, limit)
}
