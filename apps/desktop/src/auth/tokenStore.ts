import type { GoogleOAuthTokens } from '@continuum/shared'

const MEMORY_KEY = 'continuum.google.tokens'
const DEVICE_KEY = 'continuum.deviceId'

export function getDeviceId(): string {
  let id = localStorage.getItem(DEVICE_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(DEVICE_KEY, id)
  }
  return id
}

/**
 * Persist tokens locally. When running under Tauri with plugin-store installed,
 * callers can later swap this for OS secure storage without API changes.
 */
export async function saveTokens(tokens: GoogleOAuthTokens | null): Promise<void> {
  if (tokens) localStorage.setItem(MEMORY_KEY, JSON.stringify(tokens))
  else localStorage.removeItem(MEMORY_KEY)
}

export async function loadTokens(): Promise<GoogleOAuthTokens | null> {
  const raw = localStorage.getItem(MEMORY_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as GoogleOAuthTokens
  } catch {
    return null
  }
}
