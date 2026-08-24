import { useState } from 'react'
import { SettingsAccount } from './SettingsAccount'
import { SettingsAppearance } from './SettingsAppearance'
import { SettingsCalendar } from './SettingsCalendar'
import { SettingsReminders, SettingsScheduling } from './SettingsAlerts'
import { SettingsWindow } from './SettingsWindow'
import { SettingsData } from './SettingsData'
import {
  SETTINGS_CATEGORIES,
  categoryVisible,
  normalizeSettingsQuery,
  type SettingsCategoryId,
} from './settingsCatalog'
import type { SettingsFormModel } from './settingsTypes'

function SectionBody(props: { id: SettingsCategoryId; form: SettingsFormModel; query: string }) {
  const p = { form: props.form, query: props.query }
  switch (props.id) {
    case 'account':
      return <SettingsAccount {...p} />
    case 'appearance':
      return <SettingsAppearance {...p} />
    case 'calendar':
      return <SettingsCalendar {...p} />
    case 'reminders':
      return <SettingsReminders {...p} />
    case 'scheduling':
      return <SettingsScheduling {...p} />
    case 'window':
      return <SettingsWindow {...p} />
    case 'files':
      return <SettingsData {...p} />
  }
}

export function SettingsPanel(props: { form: SettingsFormModel }) {
  const { form } = props
  const [openId, setOpenId] = useState<SettingsCategoryId | null>(null)
  const searching = Boolean(normalizeSettingsQuery(form.query))
  const visibleCats = SETTINGS_CATEGORIES.filter((c) => categoryVisible(c, form.query))

  return (
    <aside className="w-80 shrink-0 space-y-3 overflow-auto rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-3 text-sm">
      <div className="flex items-center gap-2">
        {openId && !searching ? (
          <button
            type="button"
            className="rounded px-1 text-[var(--cc-accent)]"
            aria-label="Back to settings categories"
            onClick={() => setOpenId(null)}
          >
            ←
          </button>
        ) : null}
        <h2 className="font-semibold">
          {openId && !searching
            ? (SETTINGS_CATEGORIES.find((c) => c.id === openId)?.title ?? 'Settings')
            : 'Settings'}
        </h2>
      </div>
      <input
        type="search"
        placeholder="Search settings…"
        value={form.query}
        onChange={(e) => form.onQuery(e.target.value)}
        className="w-full rounded border border-[var(--cc-border)] cc-native-field px-2 py-1 text-sm"
        aria-label="Search settings"
      />
      {searching ? (
        <div className="space-y-5">
          {visibleCats.map((cat) => (
            <section key={cat.id} className="space-y-2">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
                {cat.title}
              </h3>
              <SectionBody id={cat.id} form={form} query={form.query} />
            </section>
          ))}
          {visibleCats.length === 0 ? (
            <p className="text-xs text-[var(--cc-muted)]">No settings match that search.</p>
          ) : null}
        </div>
      ) : openId ? (
        <SectionBody id={openId} form={form} query="" />
      ) : (
        <nav className="flex flex-col gap-1" aria-label="Settings categories">
          {SETTINGS_CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              type="button"
              className="rounded-lg border border-[var(--cc-border)] px-3 py-2 text-left hover:bg-[var(--cc-accent-soft)]"
              onClick={() => setOpenId(cat.id)}
            >
              <span className="block font-medium">{cat.title}</span>
              <span className="block text-xs text-[var(--cc-muted)]">{cat.blurb}</span>
            </button>
          ))}
        </nav>
      )}
      {form.lastSyncError ? <p className="text-xs text-red-500">{form.lastSyncError}</p> : null}
    </aside>
  )
}
