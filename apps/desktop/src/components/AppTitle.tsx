import { useEffect, useState } from 'react'

/** Header title plus the installed binary version (Tauri `getVersion`). */
export function AppTitle() {
  const [version, setVersion] = useState('0.17.3')

  useEffect(() => {
    void import('@tauri-apps/api/app')
      .then((m) => m.getVersion())
      .then(setVersion)
      .catch(() => {
        /* Vite / tests: keep fallback */
      })
  }, [])

  return (
    <h1 className="flex items-baseline gap-2 text-2xl font-semibold tracking-tight">
      <span>Continuum Calendar</span>
      <span className="text-xs font-normal tabular-nums text-[var(--cc-muted)]">{version}</span>
    </h1>
  )
}
