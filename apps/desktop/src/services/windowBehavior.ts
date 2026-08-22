export type MinimizeTarget = 'taskbar' | 'tray'
export type CloseTarget = 'quit' | 'tray'

export interface WindowBehavior {
  minimizeTo: MinimizeTarget
  closeTo: CloseTarget
}

export const WINDOW_BEHAVIOR_KEY = 'continuum.windowBehavior'

export function defaultWindowBehavior(): WindowBehavior {
  return { minimizeTo: 'taskbar', closeTo: 'tray' }
}

export function parseWindowBehavior(raw: unknown): WindowBehavior {
  const d = defaultWindowBehavior()
  if (!raw || typeof raw !== 'object') return d
  const o = raw as Record<string, unknown>
  return {
    minimizeTo: o.minimizeTo === 'tray' ? 'tray' : 'taskbar',
    closeTo: o.closeTo === 'quit' ? 'quit' : 'tray',
  }
}

export function loadWindowBehavior(): WindowBehavior {
  try {
    return parseWindowBehavior(JSON.parse(localStorage.getItem(WINDOW_BEHAVIOR_KEY) ?? 'null'))
  } catch {
    return defaultWindowBehavior()
  }
}

export function saveWindowBehavior(next: WindowBehavior): WindowBehavior {
  const parsed = parseWindowBehavior(next)
  localStorage.setItem(WINDOW_BEHAVIOR_KEY, JSON.stringify(parsed))
  return parsed
}

export function toNativeArgs(b: WindowBehavior): { closeToTray: boolean; minimizeToTray: boolean } {
  return { closeToTray: b.closeTo === 'tray', minimizeToTray: b.minimizeTo === 'tray' }
}
