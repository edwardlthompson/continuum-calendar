import type { ThemeMode } from '@continuum/shared'
import { textMatches } from './settingsCatalog'
import { SettingsFieldSelect, SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'

export function SettingsAppearance({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  return (
    <div className="space-y-3">
      {show('Theme', 'appearance', 'dark', 'light') ? (
        <SettingsRow label="Theme">
          <SettingsFieldSelect
            value={form.settings.themeMode}
            onChange={(v) => form.persistSettings({ themeMode: v as ThemeMode })}
          >
            <option value="light">Light</option>
            <option value="dark">Dark</option>
            <option value="system">System ({form.resolvedTheme})</option>
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
      {show('24-hour time', 'time', 'clock') ? (
        <SettingsRow label="24-hour time">
          <input
            type="checkbox"
            checked={form.settings.use24HourFormat}
            onChange={(e) => form.persistSettings({ use24HourFormat: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
      {show('Redact titles', 'screenshots', 'privacy') ? (
        <SettingsRow label="Redact titles in screenshots">
          <input
            type="checkbox"
            checked={form.settings.redactTitlesInScreenshots}
            onChange={(e) => form.persistSettings({ redactTitlesInScreenshots: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
    </div>
  )
}
