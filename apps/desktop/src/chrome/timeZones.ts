const COMMON_TIME_ZONES = [
  'UTC',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Puerto_Rico',
  'America/Toronto',
  'America/Mexico_City',
  'America/Sao_Paulo',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Madrid',
  'Africa/Johannesburg',
  'Asia/Dubai',
  'Asia/Kolkata',
  'Asia/Singapore',
  'Asia/Shanghai',
  'Asia/Tokyo',
  'Australia/Sydney',
  'Pacific/Auckland',
] as const

const MAX_COMMON = 24

function uniq(ids: string[]): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const id of ids) {
    if (!id || seen.has(id)) continue
    seen.add(id)
    out.push(id)
  }
  return out
}

/** Device zone first, then a short common list. Unknown current ids stay selectable. */
export function listTimeZones(opts: { deviceZone?: string; current?: string; query?: string } = {}): string[] {
  const device = opts.deviceZone || 'UTC'
  const current = (opts.current || '').trim()
  const base = uniq([device, ...(current ? [current] : []), ...COMMON_TIME_ZONES])
  const q = (opts.query || '').trim().toLowerCase()
  const filtered = q ? base.filter((z) => z.toLowerCase().includes(q)) : base
  return filtered.slice(0, MAX_COMMON)
}
