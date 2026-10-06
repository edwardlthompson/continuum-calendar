/** Shared destination for opening a place in an external maps app. */
export type MapOpenTarget = {
  query: string
  lat?: number
  lon?: number
}

export type MapProviderId = 'osm' | 'google' | 'apple'

export function googleMapsUrl(target: MapOpenTarget): string {
  const q =
    target.lat != null && target.lon != null
      ? `${target.lat},${target.lon}`
      : target.query.trim()
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`
}

export function appleMapsUrl(target: MapOpenTarget): string {
  const q = encodeURIComponent(target.query.trim() || 'Location')
  if (target.lat != null && target.lon != null) {
    return `https://maps.apple.com/?ll=${target.lat},${target.lon}&q=${q}`
  }
  return `https://maps.apple.com/?q=${q}`
}

export function osmMapsUrl(target: MapOpenTarget): string {
  if (target.lat != null && target.lon != null) {
    const { lat, lon } = target
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=15/${lat}/${lon}`
  }
  return `https://www.openstreetmap.org/search?query=${encodeURIComponent(target.query.trim())}`
}

export function mapProviderUrl(provider: MapProviderId, target: MapOpenTarget): string {
  switch (provider) {
    case 'google':
      return googleMapsUrl(target)
    case 'apple':
      return appleMapsUrl(target)
    default:
      return osmMapsUrl(target)
  }
}

export const MAP_PROVIDERS: Array<{ id: MapProviderId; label: string }> = [
  { id: 'osm', label: 'OpenStreetMap' },
  { id: 'google', label: 'Google Maps' },
  { id: 'apple', label: 'Apple Maps' },
]
