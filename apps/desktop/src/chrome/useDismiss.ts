import { useEffect, type RefObject } from 'react'

/** Close on Escape and pointer-down outside `root`. */
export function useDismiss(
  open: boolean,
  onClose: () => void,
  root: RefObject<HTMLElement | null>,
) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.preventDefault()
        onClose()
      }
    }
    function onPointer(e: MouseEvent) {
      const el = root.current
      if (!el) return
      if (e.target instanceof Node && !el.contains(e.target)) onClose()
    }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onPointer)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.removeEventListener('mousedown', onPointer)
    }
  }, [open, onClose, root])
}
