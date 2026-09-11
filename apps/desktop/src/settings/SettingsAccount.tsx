import { isBirthdayCalendarEntry } from '@continuum/shared'
import { isGoogleClientIdFromEnv, isGoogleConfigured } from '../auth/googleAuth'
import { queueGoogleTasksScope } from '../services/googleTasksMap'
import { humanizeOAuthFailure, isInsufficientDriveScope } from '../auth/oauthErrors'
import { getSettingsSyncError } from '../services/settingsSync'
import { saveCalendars } from '../data/localStore'
import { textMatches } from './settingsCatalog'
import { SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsAccount({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  const googleReady = isGoogleConfigured()
  const envLocked = isGoogleClientIdFromEnv()
  const syncError = getSettingsSyncError()
  const showSyncError = Boolean(syncError && !isInsufficientDriveScope(syncError))

  return (
    <div className="space-y-3">
      <p className="text-xs text-[var(--cc-muted)]">
        {form.authStatus === 'signed-in'
          ? 'Signed in with Google'
          : form.authStatus === 'needs-reauth'
            ? 'Google sign-in expired — events stay on this PC'
            : 'Not signed in'}
        {form.lastSyncedAt ? ` · last sync ${new Date(form.lastSyncedAt).toLocaleTimeString()}` : ''}
      </p>
      {form.needsDriveReconnect ? (
        <p className="text-xs text-[var(--cc-muted)]">
          Phone settings sync needs Google Drive App Data. Sign in again and approve Drive (app data
          only) — Calendar stays connected.
        </p>
      ) : showSyncError ? (
        <p className="text-xs text-red-500">
          Settings sync: {humanizeOAuthFailure(syncError ?? '')}
        </p>
      ) : form.signedIn ? (
        <p className="text-xs text-[var(--cc-muted)]">
          Peer remote: Continuum settings sync both ways with Android (Drive App Data)
        </p>
      ) : (
        <p className="text-xs text-[var(--cc-muted)]">
          Sign in to publish/pull Continuum settings with Android
        </p>
      )}
      {!googleReady ? (
        <p className="text-xs text-[var(--cc-muted)]">
          This Continuum build is missing OAuth packaging. Maintainers bake Continuum’s Desktop Client
          ID with <code className="text-[0.7rem]">scripts/set-desktop-google-client-id.py</code> then
          rebuild — see docs/GOOGLE_API_SETUP.md. End users never paste a Client ID.
        </p>
      ) : envLocked ? (
        <p className="text-xs text-[var(--cc-muted)]">
          Google Sign-in uses Continuum’s embedded Desktop OAuth client (system browser + loopback).
        </p>
      ) : null}
      {form.authStatus !== 'signed-in' || form.needsDriveReconnect ? (
        <>
          {form.authStatus === 'needs-reauth' ? (
            <p className="text-sm font-medium text-[var(--cc-brand-now)]">
              Sign in again. Edits you already made are still on this computer.
            </p>
          ) : form.needsDriveReconnect ? null : (
            <p className="text-xs text-[var(--cc-muted)]">
              Google OAuth is in Testing: if sign-in shows “unknown error” or access denied, add this
              Gmail as a Test user (Google Cloud → Audience), then try again.
            </p>
          )}
          <button
            type="button"
            className="w-full rounded bg-[var(--cc-accent)] px-2 py-1 text-white disabled:opacity-60"
            aria-label={
              form.authStatus === 'needs-reauth' || form.needsDriveReconnect
                ? 'Sign in again'
                : 'Sign in with Google'
            }
            disabled={Boolean(form.signInPending)}
            onClick={() => form.onSignIn()}
          >
            {form.signInPending
              ? 'Waiting for browser…'
              : form.needsDriveReconnect
                ? 'Enable phone settings sync'
                : form.authStatus === 'needs-reauth'
                  ? 'Sign in again'
                  : 'Sign in with Google'}
          </button>
        </>
      ) : null}
      {show('Use Google Calendar', 'Google', 'privacy') ? (
        <SettingsRow
          label="Use Google Calendar"
          title="When off, Continuum uses local calendars only (still peer-syncs via Drive App Data)."
        >
          <input
            type="checkbox"
            checked={form.settings.useGoogleCalendar}
            onChange={(e) => form.persistSettings({ useGoogleCalendar: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
      {form.signedIn && show('tasks', 'Google') ? (
        <SettingsRow
          label="Google Tasks"
          title="Optional. Default Sign in stays Calendar + Drive (KB-028). Connect Tasks separately."
        >
          {form.tasksConnected ? (
            <span className="text-xs text-[var(--cc-muted)]">Connected</span>
          ) : (
            <button
              type="button"
              className="rounded bg-[var(--cc-accent)] px-2 py-1 text-white disabled:opacity-60"
              disabled={Boolean(form.signInPending)}
              onClick={() => {
                queueGoogleTasksScope()
                form.onConnectTasks?.()
              }}
            >
              Connect Google Tasks
            </button>
          )}
        </SettingsRow>
      ) : null}
      {show('birthdays', 'Google', 'contacts') ? (
        <SettingsRow
          label="Show Google automated birthdays"
          title="Hides Google Contacts automated birthday events only. Manual yearly birthday events stay."
        >
          <input
            type="checkbox"
            checked={form.settings.showContactBirthdays}
            onChange={(e) => {
              const showBday = e.target.checked
              const nextCals = form.calendars.map((c) =>
                isBirthdayCalendarEntry(c) ? { ...c, visible: showBday } : c,
              )
              form.setCalendars(nextCals)
              saveCalendars(nextCals)
              form.persistSettings({ showContactBirthdays: showBday })
            }}
          />
        </SettingsRow>
      ) : null}
    </div>
  )
}
