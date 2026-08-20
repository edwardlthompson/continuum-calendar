/** Map Google OAuth failures to Testing-mode guidance (unverified app / test users). */

export function isTestingModeOAuthError(raw: string): boolean {
  return /access_denied|verification process|403:access_denied|unknown error/i.test(raw)
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
  if (/unknown error/i.test(raw)) {
    return (
      'Google returned “An unknown error has occurred” after the unverified-app warning. ' +
      'Add this Google account as an OAuth Test user (Audience → Test users), retry without ad blockers, ' +
      'and use a recent Continuum Calendar installer from GitHub Releases.'
    )
  }
  if (/redirect_uri_mismatch/i.test(raw)) {
    return 'Google rejected the sign-in redirect. The OAuth client must be type Desktop app, not Web.'
  }
  if (/invalid_grant/i.test(raw)) {
    return 'The Google sign-in code expired or was reused. Click Sign in with Google again and finish in one try.'
  }
  return raw
}
