export type PaletteCommandId = 'agenda' | 'week' | 'month' | 'year' | 'today' | 'settings' | 'new'

export type PaletteCommand = {
  id: PaletteCommandId
  label: string
}

export const PALETTE_COMMANDS: PaletteCommand[] = [
  { id: 'agenda', label: 'Agenda' },
  { id: 'week', label: 'Week' },
  { id: 'month', label: 'Month' },
  { id: 'year', label: 'Year' },
  { id: 'today', label: 'Go to today' },
  { id: 'settings', label: 'Settings' },
  { id: 'new', label: 'New event' },
]

/** Filter by command label/id. Never treats free text as an event to create. */
export function filterPaletteCommands(query: string, commands: PaletteCommand[] = PALETTE_COMMANDS): PaletteCommand[] {
  const q = query.trim().toLowerCase()
  if (!q) return [...commands]
  return commands.filter((c) => c.label.toLowerCase().includes(q) || c.id.includes(q))
}
