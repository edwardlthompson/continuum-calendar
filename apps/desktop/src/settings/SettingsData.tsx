import { defaultContinuumSettings } from '@continuum/shared'
import { continuumLogger } from '../diagnostics/continuumLogger'
import { loadCalendars } from '../data/localStore'
import { downloadIcsFile } from '../services/ics'
import { loadIcsSubscriptions, unsubscribeIcs } from '../services/icsSubscribe'
import { exportSettingsJson, importSettingsJson } from '../services/settingsSync'
import { textMatches } from './settingsCatalog'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsData({ form, query }: SettingsSectionProps) {
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
      'Reset',
    )
  ) {
    return null
  }
  return (
    <div className="flex flex-col gap-1">
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
    </div>
  )
}
