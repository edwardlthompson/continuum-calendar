export type SettingsCategoryId =
  | 'account'
  | 'appearance'
  | 'calendar'
  | 'reminders'
  | 'scheduling'
  | 'window'
  | 'files'

export type SettingsCategory = {
  id: SettingsCategoryId
  title: string
  blurb: string
  keywords: string[]
}

export const SETTINGS_CATEGORIES: SettingsCategory[] = [
  {
    id: 'account',
    title: 'Account & Google',
    blurb: 'Sign-in, Calendar sync, birthdays',
    keywords: ['google', 'sign in', 'privacy', 'birthdays', 'contacts', 'peer', 'sync'],
  },
  {
    id: 'appearance',
    title: 'Appearance',
    blurb: 'Theme, clock, screenshots',
    keywords: ['theme', 'dark', 'light', 'time', 'clock', '24', 'redact', 'screenshots'],
  },
  {
    id: 'calendar',
    title: 'Calendar & views',
    blurb: 'Week, agenda, holidays, default calendar',
    keywords: ['week', 'agenda', 'holiday', 'calendar', 'rolling', 'empty', 'density'],
  },
  {
    id: 'reminders',
    title: 'Reminders',
    blurb: 'Notifications, default alert, snooze',
    keywords: ['notify', 'permission', 'reminder', 'snooze'],
  },
  {
    id: 'scheduling',
    title: 'Scheduling',
    blurb: 'Work hours, travel, free slots',
    keywords: ['hours', 'travel', 'slot', 'working'],
  },
  {
    id: 'window',
    title: 'Window & startup',
    blurb: 'Minimize, close, Start with Windows, shortcuts',
    keywords: ['minimize', 'taskbar', 'tray', 'close', 'quit', 'boot', 'startup', 'login', 'keyboard', 'shortcut', 'hotkey'],
  },
  {
    id: 'files',
    title: 'Import & export',
    blurb: 'ICS, CalDAV, settings backup, logs',
    keywords: ['export', 'import', 'ics', 'caldav', 'subscribe', 'reset', 'error log', 'json'],
  },
]

export function normalizeSettingsQuery(q: string): string {
  return q.trim().toLowerCase()
}

export function textMatches(query: string, ...labels: string[]): boolean {
  const q = normalizeSettingsQuery(query)
  if (!q) return true
  return labels.some((l) => l.toLowerCase().includes(q))
}

export function categoryVisible(cat: SettingsCategory, query: string): boolean {
  return textMatches(query, cat.title, cat.blurb, ...cat.keywords)
}
