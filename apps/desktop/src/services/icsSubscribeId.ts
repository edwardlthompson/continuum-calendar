/** Stable ICS subscription ids (no storage / fetch). */

export function subscriptionCalendarId(url: string): string {
  const n = url.trim().toLowerCase()
  let hash = 0
  for (let i = 0; i < n.length; i++) hash = (Math.imul(31, hash) + n.charCodeAt(i)) | 0
  return `ics-sub-${(hash >>> 0).toString(16)}`
}

export function displayNameFromIcsUrl(url: string): string {
  try {
    const path = new URL(url).pathname.split('/').filter(Boolean).pop() ?? 'ICS'
    return decodeURIComponent(path.replace(/\.ics$/i, '') || 'ICS subscribe')
  } catch {
    return 'ICS subscribe'
  }
}
