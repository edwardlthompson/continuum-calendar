import { useEffect } from 'react'
import type { MainView } from '../components/CalendarToolbar'

function typingInField(el: EventTarget | null): boolean {
  if (!(el instanceof HTMLElement)) return false
  const tag = el.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable
}

export function useDesktopHotkeys(opts: {
  enabled?: boolean
  onNew: () => void
  onToday: () => void
  onView: (view: MainView) => void
  onSearch: () => void
  onJump: () => void
}) {
  useEffect(() => {
    if (opts.enabled === false) return
    function onKey(e: KeyboardEvent) {
      if (e.ctrlKey || e.metaKey || e.altKey) return
      if (typingInField(e.target)) return
      if (e.key === 'n' || e.key === 'N') {
        e.preventDefault()
        opts.onNew()
      } else if (e.key === 't' || e.key === 'T') {
        e.preventDefault()
        opts.onToday()
      } else if (e.key === '1') opts.onView('agenda')
      else if (e.key === '2') opts.onView('rolling')
      else if (e.key === '3') opts.onView('month')
      else if (e.key === '4') opts.onView('year')
      else if (e.key === '/' || e.key === 'f' || e.key === 'F') {
        e.preventDefault()
        opts.onSearch()
      } else if (e.key === 'g' || e.key === 'G') {
        e.preventDefault()
        opts.onJump()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [opts])
}
