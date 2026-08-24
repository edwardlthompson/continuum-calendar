/** User-facing keyboard shortcut catalog (single source of truth). */
export type DesktopHotkeyEntry = {
  label: string
  hint: string
}

export const DESKTOP_HOTKEY_CATALOG: DesktopHotkeyEntry[] = [
  { label: 'New event', hint: 'N' },
  { label: 'Today', hint: 'T' },
  { label: 'Agenda view', hint: '1' },
  { label: 'Week view', hint: '2' },
  { label: 'Month view', hint: '3' },
  { label: 'Year view', hint: '4' },
  { label: 'Search events', hint: '/ or F' },
  { label: 'Jump to date', hint: 'G' },
]

export function hotkeyTitle(label: string, hint: string): string {
  return `${label} (${hint})`
}
