import {
  GOOGLE_SCOPE_STRING,
  isTokenExpired,
  type GoogleOAuthTokens,
} from '@continuum/shared'
import { createPkcePair, randomString } from './pkce'
import { loadTokens, saveTokens } from './tokenStore'

const AUTH_STATE_KEY = 'continuum.oauth.state'
const AUTH_VERIFIER_KEY = 'continuum.oauth.verifier'
const AUTH_REDIRECT_KEY = 'continuum.oauth.redirect'

const DEFAULT_WEB_REDIRECT = 'http://localhost:5173/oauth/callback'

const CLIENT_ID_STORE_KEY = 'continuum.google.clientId'

function envClientId(): string {
  return (import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '').trim()
}

function clientSecret(): string {
  if (import.meta.env.PROD) return ''
  return (import.meta.env.VITE_GOOGLE_CLIENT_SECRET ?? '').trim()
}

/** Prefer env, then last-used client id persisted beside tokens. */
function clientId(fallbackFromTokens?: string | null): string {
  return envClientId() || (fallbackFromTokens ?? '').trim() || (localStorage.getItem(CLIENT_ID_STORE_KEY) ?? '').trim()
}

function rememberClientId(id: string): void {
  const trimmed = id.trim()
  if (trimmed) localStorage.setItem(CLIENT_ID_STORE_KEY, trimmed)
}

function configuredRedirectUri(): string {
  return (import.meta.env.VITE_GOOGLE_REDIRECT_URI ?? DEFAULT_WEB_REDIRECT).trim() || DEFAULT_WEB_REDIRECT
}

/** Google Desktop/Web clients often require client_secret on the token endpoint. */
function withClientAuth(body: URLSearchParams): URLSearchParams {
  const secret = clientSecret()
  if (secret) body.set('client_secret', secret)
  return body
}

function requireClientId(fallbackFromTokens?: string | null): string {
  const id = clientId(fallbackFromTokens)
  if (!id) {
    throw new Error(
      'Missing Google Client ID. Run: python scripts/set-desktop-google-client-id.py <CLIENT_ID> <CLIENT_SECRET> ' +
        'then restart the desktop app.',
    )
  }
  return id
}

export function isGoogleConfigured(): boolean {
  return Boolean(clientId())
}

function persistPkce(state: string, verifier: string, redirectUri: string): void {
  sessionStorage.setItem(AUTH_STATE_KEY, state)
  sessionStorage.setItem(AUTH_VERIFIER_KEY, verifier)
  sessionStorage.setItem(AUTH_REDIRECT_KEY, redirectUri)
  // Survive WebView navigations / soft reloads in the same origin.
  localStorage.setItem(AUTH_STATE_KEY, state)
  localStorage.setItem(AUTH_VERIFIER_KEY, verifier)
  localStorage.setItem(AUTH_REDIRECT_KEY, redirectUri)
}

function readPkce(): { state: string | null; verifier: string | null; redirectUri: string | null } {
  return {
    state: sessionStorage.getItem(AUTH_STATE_KEY) ?? localStorage.getItem(AUTH_STATE_KEY),
    verifier: sessionStorage.getItem(AUTH_VERIFIER_KEY) ?? localStorage.getItem(AUTH_VERIFIER_KEY),
    redirectUri: sessionStorage.getItem(AUTH_REDIRECT_KEY) ?? localStorage.getItem(AUTH_REDIRECT_KEY),
  }
}

function clearPkce(): void {
  sessionStorage.removeItem(AUTH_STATE_KEY)
  sessionStorage.removeItem(AUTH_VERIFIER_KEY)
  sessionStorage.removeItem(AUTH_REDIRECT_KEY)
  localStorage.removeItem(AUTH_STATE_KEY)
  localStorage.removeItem(AUTH_VERIFIER_KEY)
  localStorage.removeItem(AUTH_REDIRECT_KEY)
}

async function isTauri(): Promise<boolean> {
  try {
    const { isTauri } = await import('@tauri-apps/api/core')
    return isTauri()
  } catch {
    return false
  }
}

export async function beginGoogleSignIn(redirectUri = configuredRedirectUri()): Promise<string> {
  const id = requireClientId()
  const { verifier, challenge } = await createPkcePair()
  const state = randomString(16)
  persistPkce(state, verifier, redirectUri)
  rememberClientId(id)
  const params = new URLSearchParams({
    client_id: id,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: GOOGLE_SCOPE_STRING,
    access_type: 'offline',
    prompt: 'consent',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  })
  return `https://accounts.google.com/o/oauth2/v2/auth?${params}`
}

async function parseGoogleOAuthError(res: Response): Promise<string> {
  const status = res.status
  try {
    const data = (await res.json()) as { error?: string; error_description?: string }
    const parts = [data.error, data.error_description].filter(Boolean)
    if (parts.length) return `Token exchange failed (${status}): ${parts.join(' — ')}`
  } catch {
    /* ignore body parse */
  }
  return `Token exchange failed: ${status}`
}

export async function exchangeCodeForTokens(code: string, state: string): Promise<GoogleOAuthTokens> {
  const pkce = readPkce()
  if (!pkce.state || pkce.state !== state) throw new Error('OAuth state mismatch')
  if (!pkce.verifier) throw new Error('Missing PKCE verifier')
  const redirectUri = pkce.redirectUri ?? configuredRedirectUri()
  const id = requireClientId()

  const body = withClientAuth(
    new URLSearchParams({
      client_id: id,
      code,
      code_verifier: pkce.verifier,
      grant_type: 'authorization_code',
      redirect_uri: redirectUri,
    }),
  )
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) {
    const detail = await parseGoogleOAuthError(res)
    if (/client_secret is missing|Could not determine client ID/i.test(detail)) {
      throw new Error(
        `${detail} Ensure apps/desktop/.env has VITE_GOOGLE_CLIENT_ID and VITE_GOOGLE_CLIENT_SECRET ` +
          '(python scripts/set-desktop-google-client-id.py …), then restart the app.',
      )
    }
    throw new Error(detail)
  }
  const data = (await res.json()) as {
    access_token: string
    refresh_token?: string
    expires_in: number
    scope?: string
    token_type?: string
  }
  rememberClientId(id)
  const tokens: GoogleOAuthTokens = {
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresAt: Date.now() + data.expires_in * 1000,
    scope: data.scope ?? GOOGLE_SCOPE_STRING,
    tokenType: data.token_type,
    clientId: id,
  }
  await saveTokens(tokens)
  clearPkce()
  return tokens
}

export async function refreshAccessToken(tokens: GoogleOAuthTokens): Promise<GoogleOAuthTokens> {
  if (!tokens.refreshToken) throw new Error('No refresh token')
  const id = requireClientId(tokens.clientId)
  const body = withClientAuth(
    new URLSearchParams({
      client_id: id,
      refresh_token: tokens.refreshToken,
      grant_type: 'refresh_token',
    }),
  )
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) {
    const detail = await parseGoogleOAuthError(res)
    if (/Could not determine client ID|invalid_request/i.test(detail)) {
      throw new Error(
        `${detail} Restart after setting apps/desktop/.env (VITE_GOOGLE_CLIENT_ID + SECRET), then Sign in again.`,
      )
    }
    throw new Error(detail)
  }
  const data = (await res.json()) as {
    access_token: string
    expires_in: number
    scope?: string
    token_type?: string
  }
  rememberClientId(id)
  const next: GoogleOAuthTokens = {
    ...tokens,
    accessToken: data.access_token,
    expiresAt: Date.now() + data.expires_in * 1000,
    scope: data.scope ?? tokens.scope,
    tokenType: data.token_type ?? tokens.tokenType,
    clientId: id,
  }
  await saveTokens(next)
  return next
}

let freshTokensInFlight: Promise<GoogleOAuthTokens | null> | null = null

/** Single-flight token refresh — parallel sync callers share one refresh. */
export async function ensureFreshTokens(): Promise<GoogleOAuthTokens | null> {
  if (freshTokensInFlight) return freshTokensInFlight
  freshTokensInFlight = (async () => {
    let tokens = await loadTokens()
    if (!tokens) return null
    if (isTokenExpired(tokens)) {
      if (!tokens.refreshToken) return null
      tokens = await refreshAccessToken(tokens)
    }
    return tokens
  })().finally(() => {
    freshTokensInFlight = null
  })
  return freshTokensInFlight
}

export async function signOutGoogle(): Promise<void> {
  await saveTokens(null)
}

export async function getStoredTokens(): Promise<GoogleOAuthTokens | null> {
  return loadTokens()
}

export function parseOAuthCallback(url: string): { code: string; state: string } | null {
  try {
    const u = new URL(url)
    const code = u.searchParams.get('code')
    const state = u.searchParams.get('state')
    if (code && state) return { code, state }
  } catch {
    return null
  }
  return null
}

/**
 * Tauri: system browser + loopback redirect (Desktop OAuth client).
 * Browser/Vite: in-window navigation to Google, callback on localhost:5173.
 */
export async function signInWithGoogle(): Promise<'pending-redirect' | GoogleOAuthTokens> {
  if (!isGoogleConfigured()) {
    throw new Error('Missing VITE_GOOGLE_CLIENT_ID — see docs/GOOGLE_API_SETUP.md')
  }

  if (await isTauri()) {
    const { invoke } = await import('@tauri-apps/api/core')
    const { listen } = await import('@tauri-apps/api/event')
    const { openUrl } = await import('@tauri-apps/plugin-opener')

    const port = await invoke<number>('start_oauth_loopback')
    const redirectUri = `http://127.0.0.1:${port}/`
    const authUrl = await beginGoogleSignIn(redirectUri)

    const tokens = await new Promise<GoogleOAuthTokens>((resolve, reject) => {
      let unlisten: (() => void) | undefined
      const timer = window.setTimeout(() => {
        unlisten?.()
        reject(new Error('Sign-in timed out — complete Google consent in your browser'))
      }, 300_000)

      void (async () => {
        try {
          unlisten = await listen<{ code: string; state: string }>('oauth-callback', (event) => {
            window.clearTimeout(timer)
            unlisten?.()
            void exchangeCodeForTokens(event.payload.code, event.payload.state)
              .then(resolve)
              .catch(reject)
          })
          await openUrl(authUrl)
        } catch (e) {
          window.clearTimeout(timer)
          unlisten?.()
          reject(e instanceof Error ? e : new Error('Failed to open system browser'))
        }
      })()
    })
    return tokens
  }

  const authUrl = await beginGoogleSignIn(configuredRedirectUri())
  window.location.href = authUrl
  return 'pending-redirect'
}
