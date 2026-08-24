import { isBirthdayCalendarEntry } from '@continuum/shared'
import { getSettingsSyncError } from '../services/settingsSync'
import { saveCalendars } from '../data/localStore'
import { textMatches } from './settingsCatalog'
import { SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsAccount({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
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
      {getSettingsSyncError() ? (
        <p className="text-xs text-red-500">Settings sync: {getSettingsSyncError()}</p>
      ) : form.signedIn ? (
        <p className="text-xs text-[var(--cc-muted)]">
          Peer remote: Continuum settings sync both ways with Android (Drive App Data)
        </p>
      ) : (
        <p className="text-xs text-[var(--cc-muted)]">
          Sign in to publish/pull Continuum settings with Android
        </p>
      )}
      {form.authStatus !== 'signed-in' ? (
        <>
          {form.authStatus === 'needs-reauth' ? (
            <p className="text-sm font-medium text-[var(--cc-brand-now)]">
              Sign in again. Edits you already made are still on this computer.
            </p>
          ) : (
            <p className="text-xs text-[var(--cc-muted)]">
              Google OAuth is in Testing: if sign-in shows “unknown error” or access denied, add this
              Gmail as a Test user (Google Cloud → Audience), then try again.
            </p>
          )}
          <button
            type="button"
            className="w-full rounded bg-[var(--cc-accent)] px-2 py-1 text-white"
            aria-label={form.authStatus === 'needs-reauth' ? 'Sign in again' : 'Sign in with Google'}
            onClick={() => form.onSignIn()}
          >
            {form.authStatus === 'needs-reauth' ? 'Sign in again' : 'Sign in with Google'}
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
