import { isExpiredGoogleAuth } from './oauthErrors'

export type GoogleAuthStatus = 'signed-out' | 'signed-in' | 'needs-reauth'

const NEEDS_REAUTH = 'continuum.google.needsReauth'
const NOTIFIED = 'continuum.google.reauthNotified'
const LOCAL_EVENTS_PENDING = 'continuum.localEvents.pendingPeerPush'
const SETTINGS_PENDING = 'continuum.settings.pendingPeerPush'

const listeners = new Set<() => void>()

export function loadNeedsReauth(): boolean {
  return localStorage.getItem(NEEDS_REAUTH) === '1'
}

export function clearNeedsReauth(): void {
  localStorage.removeItem(NEEDS_REAUTH)
  localStorage.removeItem(NOTIFIED)
}

/** Queue local edits and tell the UI — never drop calendar data. */
export function handleExpiredGoogleAuth(err?: unknown): void {
  if (err && !isExpiredGoogleAuth(err)) return
  localStorage.setItem(NEEDS_REAUTH, '1')
  localStorage.setItem(LOCAL_EVENTS_PENDING, '1')
  localStorage.setItem(SETTINGS_PENDING, '1')
  notifyReauthOnce()
  for (const cb of listeners) cb()
}

export function onAuthExpired(cb: () => void): () => void {
  listeners.add(cb)
  return () => {
    listeners.delete(cb)
  }
}

function notifyReauthOnce(): void {
  if (localStorage.getItem(NOTIFIED) === '1') return
  localStorage.setItem(NOTIFIED, '1')
  void (async () => {
    try {
      if (typeof Notification === 'undefined') return
      if (Notification.permission === 'default') await Notification.requestPermission()
      if (Notification.permission !== 'granted') return
      new Notification('Sign in to Google again', {
        body: 'Your session expired. Edits on this PC are saved. Sign in again to sync.',
        tag: 'continuum-google-reauth',
      })
    } catch {
      /* WebView may reject Notification */
    }
  })()
}

export function authStatusFromTokens(hasFreshTokens: boolean): GoogleAuthStatus {
  if (hasFreshTokens) return 'signed-in'
  if (loadNeedsReauth()) return 'needs-reauth'
  return 'signed-out'
}
