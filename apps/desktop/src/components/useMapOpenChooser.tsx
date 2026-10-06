import { useState } from 'react'
import type { MapOpenTarget } from '../lib/mapProviders'
import { MapOpenChooser } from './MapOpenChooser'

/** Local state + dialog for OSM / Google Maps / Apple Maps chooser. */
export function useMapOpenChooser() {
  const [target, setTarget] = useState<MapOpenTarget | null>(null)
  return {
    openMap: (next: MapOpenTarget) => setTarget(next),
    mapChooser: <MapOpenChooser target={target} onClose={() => setTarget(null)} />,
  }
}
