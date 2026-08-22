/** Map Google OAuth failures to Testing-mode guidance (unverified app / test users). */

export function isTestingModeOAuthError(raw: string): boolean {
  return /access_denied|verification process|403:access_denied|unknown error|something went wrong/i.test(raw)
}

export function humanizeOAuthFailure(err: unknown): string {
  const raw = err instanceof Error ? err.message : String(err)
  if (/access_denied|verification process|403:access_denied/i.test(raw)) {
    return (
      'Google blocked this account (Testing mode). Open Google Cloud → Audience / OAuth consent ' +
      'screen, add this exact Gmail as a Test user (or publish the app), wait a minute, then Sign in again.'
    )
  }
  if (/client_secret is missing/i.test(raw)) {
    return 'This installer cannot finish Google sign-in. Reinstall Continuum Calendar from GitHub Releases.'
  }
  if (/Failed to fetch|NetworkError|Load failed|error sending request/i.test(raw)) {
    return 'Continuum could not reach Google to finish sign-in. Check the network and try again.'
  }
  if (/unknown error|something went wrong/i.test(raw)) {
    return (
      'Google blocked that sign-in screen (Testing mode). Use Sign in with Google for Calendar only, ' +
      'then save the event again. On the phone: Settings → Continuum → Connect Google calendars.'
    )
  }
  if (isInsufficientDriveScope(raw)) {
    return (
      'Local Continuum files cannot sync to the phone while Google sign-in is Calendar-only. ' +
      'Save the event to Google Calendar, then pull calendars on the phone.'
    )
  }
  if (/redirect_uri_mismatch/i.test(raw)) {
    return 'Google rejected the sign-in redirect. The OAuth client must be type Desktop app, not Web.'
  }
  if (isExpiredGoogleAuth(raw)) {
    return (
      'Google sign-in expired. Your events are still saved on this PC. ' +
      'Click Sign in again to sync with your phone and Google Calendar.'
    )
  }
  return raw
}

/** Token can call Calendar API but not Drive App Data (phone peer sync). */
export function hasDriveAppDataScope(scope: string): boolean {
  return scope.includes('https://www.googleapis.com/auth/drive.appdata')
}

export function isInsufficientDriveScope(err: unknown): boolean {
  const raw = err instanceof Error ? err.message : String(err)
  return /insufficient (authentication )?scopes|Insufficient Permission|Drive .* 403/i.test(raw)
}

/** Refresh token revoked/expired — user must complete Sign in again. */
export function isExpiredGoogleAuth(err: unknown): boolean {
  const raw = err instanceof Error ? err.message : String(err)
  return /invalid_grant|token has been expired or revoked/i.test(raw)
}
