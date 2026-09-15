import type { AgendaDaySection } from './events.js'
import { dayShouldShowOpen, todayAgendaPhase } from './agenda.ts'

const DEFAULT_MAX_EMPTY = 7

export type AgendaFoldOpts = {
  todayKey: string
  maxEmpty?: number
  /** When set, today counts as Open if Agenda would show an Open row (past-only days). */
  nowMs?: number
  workingHoursEnd?: string
}

function looksOpen(section: AgendaDaySection, opts: AgendaFoldOpts): boolean {
  if (section.date === opts.todayKey && opts.nowMs != null) {
    const end = opts.workingHoursEnd || '17:00'
    return todayAgendaPhase(section.events, opts.nowMs, end, opts.todayKey) !== 'active'
  }
  return dayShouldShowOpen(section.events)
}

/**
 * Cap empty / Open days so Agenda stays scannable.
 * Today and busy (timed) days always stay. Leftover Opens are counted, not listed.
 */
export function collapseEmptyAgendaSections(
  sections: AgendaDaySection[],
  opts: AgendaFoldOpts,
): { visible: AgendaDaySection[]; foldedCount: number } {
  const maxEmpty = !opts.maxEmpty ? DEFAULT_MAX_EMPTY : opts.maxEmpty
  const visible: AgendaDaySection[] = []
  let emptyKept = 0
  let foldedCount = 0

  for (const section of sections) {
    if (!section.date) continue
    const isToday = section.date === opts.todayKey
    const openDay = looksOpen(section, opts)
    if (!openDay || isToday) {
      visible.push(section)
      if (openDay) emptyKept += 1
      continue
    }
    if (emptyKept < maxEmpty) {
      visible.push(section)
      emptyKept += 1
    } else {
      foldedCount += 1
    }
  }

  return { visible, foldedCount }
}
