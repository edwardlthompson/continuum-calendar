import type { GoogleOAuthTokens } from '@continuum/shared'

const MEMORY_KEY = 'continuum.google.tokens'
const DEVICE_KEY = 'continuum.deviceId'

let scopeCache = ''

export function getDeviceId(): string {
  let id = localStorage.getItem(DEVICE_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(DEVICE_KEY, id)
  }
  return id
}

export function parseStoredTokens(raw: string | null): GoogleOAuthTokens | null {
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as GoogleOAuthTokens
    if (!parsed || typeof parsed !== 'object') return null
    if (typeof parsed.accessToken !== 'string' || !parsed.accessToken) return null
    return parsed
  } catch {
    return null
  }
}

export function scopeFromTokens(tokens: GoogleOAuthTokens | null): string {
  const scope = tokens?.scope
  return typeof scope === 'string' ? scope : ''
}

function inTauri(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

async function vaultSave(json: string | null): Promise<void> {
  const { invoke } = await import('@tauri-apps/api/core')
  await invoke('save_google_tokens', { json })
}

async function vaultLoad(): Promise<string | null> {
  const { invoke } = await import('@tauri-apps/api/core')
  return invoke<string | null>('load_google_tokens')
}

function remember(tokens: GoogleOAuthTokens | null): void {
  scopeCache = scopeFromTokens(tokens)
}

/**
 * Persist tokens in the native app-config vault (mode 0600) under Tauri,
 * with a one-time migration off localStorage. Browser/dev keeps localStorage.
 */
export async function saveTokens(tokens: GoogleOAuthTokens | null): Promise<void> {
  remember(tokens)
  const raw = tokens ? JSON.stringify(tokens) : null
  if (inTauri()) {
    await vaultSave(raw)
    localStorage.removeItem(MEMORY_KEY)
    return
  }
  if (raw) localStorage.setItem(MEMORY_KEY, raw)
  else localStorage.removeItem(MEMORY_KEY)
}

export async function loadTokens(): Promise<GoogleOAuthTokens | null> {
  if (inTauri()) {
    const vault = parseStoredTokens(await vaultLoad())
    if (vault) {
      remember(vault)
      localStorage.removeItem(MEMORY_KEY)
      return vault
    }
    const legacy = parseStoredTokens(localStorage.getItem(MEMORY_KEY))
    if (legacy) {
      await saveTokens(legacy)
      return legacy
    }
    remember(null)
    return null
  }
  const tokens = parseStoredTokens(localStorage.getItem(MEMORY_KEY))
  remember(tokens)
  return tokens
}

/** Sync read of the stored OAuth scope string (empty when signed out or corrupt). */
export function peekStoredTokenScope(): string {
  if (scopeCache) return scopeCache
  return scopeFromTokens(parseStoredTokens(localStorage.getItem(MEMORY_KEY)))
}
