export type RepeatFreq = 'none' | 'daily' | 'weekly' | 'monthly' | 'yearly'
export type MonthlyMode = 'bydate' | 'byweekday' | 'last'

const FREQ_ICS: Record<Exclude<RepeatFreq, 'none'>, string> = {
  daily: 'DAILY',
  weekly: 'WEEKLY',
  monthly: 'MONTHLY',
  yearly: 'YEARLY',
}
const DOW = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'] as const

export function seriesEventId(id: string): string {
  const i = id.indexOf('::')
  return i >= 0 ? id.slice(0, i) : id
}

export function occurrenceDateKey(iso: string): string {
  return iso.slice(0, 10)
}

export function rruleBlob(rules?: string[]): string {
  return (rules ?? []).join(';').toUpperCase()
}

export function freqFromRrule(rules?: string[]): RepeatFreq {
  const blob = rruleBlob(rules)
  if (blob.includes('FREQ=DAILY')) return 'daily'
  if (blob.includes('FREQ=WEEKLY')) return 'weekly'
  if (blob.includes('FREQ=MONTHLY')) return 'monthly'
  if (blob.includes('FREQ=YEARLY')) return 'yearly'
  return 'none'
}

export function untilFromRrule(rules?: string[]): string | undefined {
  const m = rruleBlob(rules).match(/UNTIL=(\d{8})/)
  if (!m) return undefined
  return `${m[1].slice(0, 4)}-${m[1].slice(4, 6)}-${m[1].slice(6, 8)}`
}

export function monthlyModeFromRrule(rules?: string[]): MonthlyMode {
  const blob = rruleBlob(rules)
  if (blob.includes('BYMONTHDAY=-1')) return 'last'
  if (/BYDAY=-?\d/.test(blob)) return 'byweekday'
  return 'bydate'
}

function weekdayToken(iso?: string): string {
  const d = iso ? new Date(`${iso.slice(0, 10)}T12:00:00`) : new Date()
  const nth = Math.ceil(d.getDate() / 7)
  return `${Math.min(nth, 4)}${DOW[d.getDay()]}`
}

export function rruleFromParts(opts: {
  freq: RepeatFreq
  until?: string
  monthly?: MonthlyMode
  start?: string
}): string[] | undefined {
  if (opts.freq === 'none') return undefined
  let rule = `RRULE:FREQ=${FREQ_ICS[opts.freq]}`
  if (opts.freq === 'monthly' && opts.monthly === 'last') rule += ';BYMONTHDAY=-1'
  if (opts.freq === 'monthly' && opts.monthly === 'byweekday') {
    rule += `;BYDAY=${weekdayToken(opts.start)}`
  }
  const compact = opts.until?.replace(/-/g, '').slice(0, 8)
  if (compact && /^\d{8}$/.test(compact)) rule += `;UNTIL=${compact}`
  return [rule]
}

/** @deprecated prefer rruleFromParts */
export function rruleFromFreq(freq: RepeatFreq, until?: string): string[] | undefined {
  return rruleFromParts({ freq, until })
}

export function dayBefore(dateKey: string): string {
  const d = new Date(`${dateKey}T12:00:00`)
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
