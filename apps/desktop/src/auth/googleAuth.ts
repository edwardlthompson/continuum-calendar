import {
  GOOGLE_SCOPES,
  GOOGLE_SCOPE_STRING,
  isTokenExpired,
  type GoogleOAuthTokens,
} from '@continuum/shared'

/**
 * Calendar only while the OAuth app is in Testing (KB-028). Extra scopes
 * (Drive, Contacts, Tasks) plus prompt=consent make Google show
 * "Sorry, something went wrong there" after Continue.
 */
const SIGNIN_SCOPE = GOOGLE_SCOPES.calendar
import { createPkcePair, randomString } from './pkce'
import { loadTokens, saveTokens } from './tokenStore'
import { openExternal } from '../about/openExternal'
import { humanizeOAuthFailure, isExpiredGoogleAuth } from './oauthErrors'
import { handleExpiredGoogleAuth } from './authSession'

export {
  hasDriveAppDataScope,
  humanizeOAuthFailure,
  isInsufficientDriveScope,
  isTestingModeOAuthError,
} from './oauthErrors'

const AUTH_STATE_KEY = 'continuum.oauth.state'
const AUTH_VERIFIER_KEY = 'continuum.oauth.verifier'
const AUTH_REDIRECT_KEY = 'continuum.oauth.redirect'

const DEFAULT_WEB_REDIRECT = 'http://localhost:5173/oauth/callback'

const CLIENT_ID_STORE_KEY = 'continuum.google.clientId'

function envClientId(): string {
  return (import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '').trim()
}

function clientSecret(): string {
  // Desktop/installed OAuth clients: Google does not treat this value as confidential
  // and the token endpoint rejects the code without it. Keep it out of git (.env).
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
    scope: SIGNIN_SCOPE,
    access_type: 'offline',
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
  const data = await postGoogleToken(body)
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
  const data = await postGoogleToken(body)
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
      if (!tokens.refreshToken) {
        await saveTokens(null)
        handleExpiredGoogleAuth(new Error('No refresh token'))
        return null
      }
      try {
        tokens = await refreshAccessToken(tokens)
      } catch (e) {
        if (isExpiredGoogleAuth(e)) {
          await saveTokens(null)
          handleExpiredGoogleAuth(e)
          return null
        }
        throw e
      }
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

type GoogleTokenJson = {
  access_token: string
  refresh_token?: string
  expires_in: number
  scope?: string
  token_type?: string
}

async function postGoogleToken(body: URLSearchParams): Promise<GoogleTokenJson> {
  if (await isTauri()) {
    const { invoke } = await import('@tauri-apps/api/core')
    try {
      const text = await invoke<string>('google_oauth_token', { body: body.toString() })
      return parseTokenJson(text)
    } catch (e) {
      const raw = typeof e === 'string' ? e : e instanceof Error ? e.message : 'Token exchange failed'
      throw new Error(raw)
    }
  }
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) throw new Error(await parseGoogleOAuthError(res))
  return parseTokenJson(await res.text())
}

function parseTokenJson(text: string): GoogleTokenJson {
  let data: GoogleTokenJson
  try {
    data = JSON.parse(text) as GoogleTokenJson
  } catch {
    throw new Error('Token exchange failed: Google returned an invalid response')
  }
  if (!data.access_token || !Number.isFinite(data.expires_in)) {
    throw new Error('Token exchange failed: Google returned an empty token')
  }
  return data
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

    const port = await invoke<number>('start_oauth_loopback')
    const redirectUri = `http://127.0.0.1:${port}`
    const authUrl = await beginGoogleSignIn(redirectUri)

    const tokens = await new Promise<GoogleOAuthTokens>((resolve, reject) => {
      let unlistenOk: (() => void) | undefined
      let unlistenErr: (() => void) | undefined
      const finish = () => {
        unlistenOk?.()
        unlistenErr?.()
      }
      const timer = window.setTimeout(() => {
        finish()
        reject(new Error('Sign-in timed out — complete Google consent in your browser'))
      }, 300_000)

      void (async () => {
        try {
          unlistenOk = await listen<{ code: string; state: string }>('oauth-callback', (event) => {
            window.clearTimeout(timer)
            finish()
            void exchangeCodeForTokens(event.payload.code, event.payload.state)
              .then(resolve)
              .catch((e) => reject(new Error(humanizeOAuthFailure(e))))
          })
          unlistenErr = await listen<{ error: string; errorDescription: string }>(
            'oauth-error',
            (event) => {
              window.clearTimeout(timer)
              finish()
              const raw = [event.payload.error, event.payload.errorDescription]
                .filter(Boolean)
                .join(' — ')
              reject(new Error(humanizeOAuthFailure(raw)))
            },
          )
          await openExternal(authUrl)
        } catch (e) {
          window.clearTimeout(timer)
          finish()
          reject(e instanceof Error ? e : new Error('Failed to open the Google sign-in page in your browser'))
        }
      })()
    })
    return tokens
  }

  const authUrl = await beginGoogleSignIn(configuredRedirectUri())
  window.location.href = authUrl
  return 'pending-redirect'
}
