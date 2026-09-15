/** Jump-date string from the in-app dialog (replaces window.prompt). */
export function parseJumpDate(raw: string): string | null {
  const s = raw.trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return null
  const [y, m, d] = s.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  if (dt.getFullYear() !== y || dt.getMonth() !== m - 1 || dt.getDate() !== d) return null
  return s
}

export type PromptField = {
  name: string
  label: string
  type?: 'text' | 'password' | 'url'
  defaultValue?: string
}

export type ChromePromptId = 'jump' | 'ics-open' | 'ics-subscribe' | 'caldav'

export function promptFields(id: ChromePromptId, jumpDate: string): PromptField[] {
  if (id === 'jump') {
    return [{ name: 'date', label: 'Date (YYYY-MM-DD)', defaultValue: jumpDate }]
  }
  if (id === 'ics-open') {
    return [{ name: 'url', label: 'webcal:// or https://…ics link', type: 'url' }]
  }
  if (id === 'ics-subscribe') {
    return [{ name: 'url', label: 'Subscribe URL (refreshes on sync)', type: 'url' }]
  }
  return [
    {
      name: 'serverUrl',
      label: 'CalDAV server URL',
      type: 'url',
      defaultValue: 'https://example.com/remote.php/dav/',
    },
    { name: 'username', label: 'Username' },
    { name: 'password', label: 'App password', type: 'password' },
  ]
}

export function promptTitle(id: ChromePromptId): string {
  if (id === 'jump') return 'Go to date'
  if (id === 'ics-open') return 'Open calendar link'
  if (id === 'ics-subscribe') return 'Subscribe to ICS URL'
  return 'Add CalDAV account'
}
