/** Live place labels for the event location field (Photon / OSM). */
export async function suggestLocations(query: string, limit = 8): Promise<string[]> {
  const q = query.trim()
  if (q.length < 2) return []
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}&limit=${limit}`
  const res = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!res.ok) return []
  const data = (await res.json()) as {
    features?: Array<{
      properties?: {
        name?: string
        housenumber?: string
        street?: string
        city?: string
        locality?: string
        state?: string
        country?: string
      }
    }>
  }
  const out: string[] = []
  for (const f of data.features ?? []) {
    const p = f.properties ?? {}
    const street = [p.housenumber, p.street].filter(Boolean).join(' ')
    const line = [p.name || street, p.city || p.locality, p.state, p.country]
      .map((s) => (s ?? '').trim())
      .filter(Boolean)
      .filter((v, i, a) => a.indexOf(v) === i)
      .join(', ')
    if (line && !out.includes(line)) out.push(line)
  }
  return out
}
