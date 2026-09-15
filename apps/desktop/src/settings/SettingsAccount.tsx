import { isBirthdayCalendarEntry } from '@continuum/shared'
import { isGoogleConfigured } from '../auth/googleAuth'
import { queueGoogleTasksScope } from '../services/googleTasksMap'
import { humanizeOAuthFailure, isInsufficientDriveScope } from '../auth/oauthErrors'
import { getSettingsSyncError } from '../services/settingsSync'
import { saveCalendars } from '../data/localStore'
import { VENMO_DONATE_URL } from '../about/donate'
import { openExternal } from '../about/openExternal'
import { textMatches } from './settingsCatalog'
import { SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsAccount({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  const googleReady = isGoogleConfigured()
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
          Sign in again so settings can sync to your phone.
        </p>
      ) : showSyncError ? (
        <p className="text-xs text-red-500">
          Settings sync: {humanizeOAuthFailure(syncError ?? '')}
        </p>
      ) : form.signedIn ? (
        <p className="text-xs text-[var(--cc-muted)]">Settings also sync to your phone</p>
      ) : (
        <p className="text-xs text-[var(--cc-muted)]">
          Sign in to use Google Calendar and sync with your phone
        </p>
      )}
      {!googleReady ? (
        <p className="text-xs text-[var(--cc-muted)]">Sign-in is not available in this build.</p>
      ) : null}
      {form.authStatus !== 'signed-in' || form.needsDriveReconnect ? (
        <>
          {form.authStatus === 'needs-reauth' ? (
            <p className="text-sm font-medium text-[var(--cc-brand-now)]">
              Sign in again. Edits you already made are still on this computer.
            </p>
          ) : null}
          <button
            type="button"
            className="cc-btn-accent w-full disabled:opacity-60"
            aria-label={
              form.authStatus === 'needs-reauth' || form.needsDriveReconnect
                ? 'Sign in again'
                : 'Sign in with Google'
            }
            disabled={Boolean(form.signInPending)}
            onClick={() => form.onSignIn()}
          >
            {form.signInPending
              ? 'Opening Google…'
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
          title="When off, Continuum uses local calendars only."
        >
          <input
            type="checkbox"
            checked={form.settings.useGoogleCalendar}
            onChange={(e) => form.persistSettings({ useGoogleCalendar: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
      {form.signedIn && show('tasks', 'Google') ? (
        <SettingsRow label="Google Tasks" title="Optional. Connect Tasks separately from Calendar.">
          {form.tasksConnected ? (
            <span className="text-xs text-[var(--cc-muted)]">Connected</span>
          ) : (
            <button
              type="button"
              className="cc-btn-accent disabled:opacity-60"
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
      <p>
        <button
          type="button"
          className="text-sm text-[var(--cc-accent)] underline-offset-2 hover:underline"
          onClick={() => void openExternal(VENMO_DONATE_URL)}
        >
          Donate via Venmo
        </button>
      </p>
    </div>
  )
}
