/** OAuth token bundle for Google Calendar + Contacts + Drive AppData APIs. */
export interface GoogleOAuthTokens {
  accessToken: string
  refreshToken?: string
  expiresAt: number
  scope: string
  tokenType?: string
  /** Client ID used when tokens were issued — needed for refresh if env is missing. */
  clientId?: string
}

export const GOOGLE_SCOPES = {
  calendar: 'https://www.googleapis.com/auth/calendar',
  contactsReadonly: 'https://www.googleapis.com/auth/contacts.readonly',
  driveAppData: 'https://www.googleapis.com/auth/drive.appdata',
  tasks: 'https://www.googleapis.com/auth/tasks',
} as const

export const GOOGLE_SCOPE_STRING = [
  GOOGLE_SCOPES.calendar,
  GOOGLE_SCOPES.contactsReadonly,
  GOOGLE_SCOPES.driveAppData,
  GOOGLE_SCOPES.tasks,
].join(' ')

/** Desktop Sign in: Calendar + private Drive App Data. Omit Contacts/Tasks (KB-028). */
export const GOOGLE_DESKTOP_SIGNIN_SCOPE = [
  GOOGLE_SCOPES.calendar,
  GOOGLE_SCOPES.driveAppData,
].join(' ')

export function isTokenExpired(tokens: GoogleOAuthTokens, skewMs = 60_000): boolean {
  return Date.now() >= tokens.expiresAt - skewMs
}
