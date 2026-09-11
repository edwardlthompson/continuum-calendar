import { defaultContinuumSettings } from '@continuum/shared'
import { useEffect, useState } from 'react'
import { openExternal } from '../about/openExternal'
import { onSaveCrashesChanged, readPendingCrash } from '../crash-capture/pendingCrash.ts'
import { continuumLogger } from '../diagnostics/continuumLogger.ts'
import { getSaveCrashes, setSaveCrashes } from '../feedback/saveCrashes.ts'
import { buildIssueFormUrl, crashTitle } from '../github-feedback/issueFormUrl.ts'
import { loadCalendars } from '../data/localStore'
import { downloadIcsFile } from '../services/ics'
import { loadIcsSubscriptions, unsubscribeIcs } from '../services/icsSubscribe'
import { exportSettingsJson, importSettingsJson } from '../services/settingsSync'
import { claimDefaultCalendar, isDefaultCalendar } from './claimDefaultCalendar'
import { textMatches } from './settingsCatalog'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsData({ form, query }: SettingsSectionProps) {
  const [isDefault, setIsDefault] = useState(false)
  useEffect(() => {
    void isDefaultCalendar().then(setIsDefault).catch(() => setIsDefault(false))
  }, [])

  if (
    !textMatches(
      query,
      'Export',
      'Import',
      'ICS',
      'CalDAV',
      'subscribe',
      'settings',
      'error log',
      'crash',
      'feedback',
      'Reset',
      'default calendar',
      'webcal',
    )
  ) {
    return null
  }
  return (
    <div className="flex flex-col gap-1">
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1"
        onClick={() => {
          void claimDefaultCalendar()
            .then((msg) => {
              setIsDefault(true)
              form.flash(msg.split('\n')[0] ?? 'Continuum is the default calendar app')
            })
            .catch((e) => {
              continuumLogger.error('claim_default_calendar failed', e)
              form.flash(e instanceof Error ? e.message : String(e))
            })
        }}
      >
        {isDefault ? 'Continuum is the default calendar app' : 'Make Continuum the default calendar app'}
      </button>
      {typeof navigator !== 'undefined' && /Win/i.test(navigator.userAgent) ? (
        <p className="text-xs text-[var(--cc-muted)]">
          Windows: also check Settings → Apps → Default apps → Calendar / .ics.
        </p>
      ) : null}
      <button type="button" className="rounded border border-[var(--cc-border)] px-2 py-1" onClick={() => void downloadIcsFile(form.visibleEvents)}>
        Export ICS
      </button>
      <label className="rounded border border-[var(--cc-border)] px-2 py-1 text-center">
        Import ICS
        <input
          type="file"
          accept=".ics,text/calendar"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) form.onImportIcs(f)
          }}
        />
      </label>
      <button type="button" className="rounded border border-[var(--cc-border)] px-2 py-1" onClick={() => form.onOpenCalendarLink()}>
        Open calendar link…
      </button>
      <button type="button" className="rounded border border-[var(--cc-border)] px-2 py-1" onClick={() => form.onSubscribeIcs()}>
        Subscribe to ICS URL…
      </button>
      {loadIcsSubscriptions().map((sub) => (
        <div key={sub.id} className="flex items-center justify-between gap-2 text-xs">
          <span className="min-w-0 truncate" title={sub.url}>
            {sub.displayName}
            {sub.lastError ? ` · ${sub.lastError}` : ''}
          </span>
          <button
            type="button"
            className="shrink-0 underline"
            onClick={() => {
              form.setEvents(unsubscribeIcs(sub.calendarId))
              form.setCalendars(loadCalendars())
              form.flash(`Unsubscribed ${sub.displayName}`)
            }}
          >
            Remove
          </button>
        </div>
      ))}
      <button type="button" className="rounded border border-[var(--cc-border)] px-2 py-1" onClick={() => form.onAddCalDav()}>
        Add CalDAV account
      </button>
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1"
        onClick={() => {
          const blob = new Blob([exportSettingsJson()], { type: 'application/json' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = 'continuum-settings.json'
          a.click()
        }}
      >
        Export settings JSON
      </button>
      <label className="rounded border border-[var(--cc-border)] px-2 py-1 text-center">
        Import settings JSON
        <input
          type="file"
          accept="application/json"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (!f) return
            void f.text().then((t) => {
              try {
                form.applySettings(importSettingsJson(t), 'Settings imported')
              } catch (err) {
                form.flash(err instanceof Error ? err.message : 'Import failed')
              }
            })
          }}
        />
      </label>
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1"
        onClick={() => form.persistSettings(defaultContinuumSettings(), 'Reset to Continuum defaults')}
      >
        Reset Continuum defaults
      </button>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          defaultChecked={getSaveCrashes()}
          onChange={(ev) => {
            setSaveCrashes(ev.target.checked)
            onSaveCrashesChanged(ev.target.checked)
          }}
        />
        Save last crash locally (never auto-sends)
      </label>
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1"
        onClick={() => {
          continuumLogger.downloadLog()
          form.flash('Error log downloaded')
        }}
      >
        Download error log
      </button>
      <button
        type="button"
        className="rounded border border-[var(--cc-border)] px-2 py-1"
        onClick={() => {
          const crash = readPendingCrash()
          const built = buildIssueFormUrl('edwardlthompson/continuum-calendar', 'bug_report.yml', {
            title: crash ? crashTitle('pending', 'Error') : 'Continuum Calendar feedback',
            description: crash?.message ?? '',
            stack: crash?.stack ?? '',
          })
          if (!built.url) {
            form.flash('Could not build a GitHub feedback link')
            return
          }
          void openExternal(built.url).catch((e) => {
            continuumLogger.error('Open feedback URL failed', e)
            form.flash('Could not open GitHub')
          })
        }}
      >
        Report a problem on GitHub
      </button>
    </div>
  )
}
