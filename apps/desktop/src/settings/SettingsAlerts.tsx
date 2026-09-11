import { textMatches } from './settingsCatalog'
import { SettingsNumber, SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'
import { TimeOnlyField } from '../components/DateTimeLocalField'

export function SettingsReminders({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  return (
    <div className="space-y-3">
      {show('Notifications', 'notify', 'permission') ? (
        <SettingsRow label="Notifications">
          <input
            type="checkbox"
            checked={form.settings.notificationEnabled}
            onChange={(e) => form.onToggleNotifications(e.target.checked)}
          />
        </SettingsRow>
      ) : null}
      {show('Default reminder', 'reminder') ? (
        <SettingsRow label="Default reminder (min)">
          <SettingsNumber
            min={0}
            value={form.settings.defaultReminderMinutes}
            onChange={(n) => form.persistSettings({ defaultReminderMinutes: Math.max(0, n || 0) })}
          />
        </SettingsRow>
      ) : null}
      {show('Default snooze', 'snooze') ? (
        <SettingsRow label="Default snooze (min)">
          <SettingsNumber
            min={1}
            value={form.settings.defaultSnoozeMinutes}
            onChange={(n) => form.persistSettings({ defaultSnoozeMinutes: Math.max(1, n || 10) })}
          />
        </SettingsRow>
      ) : null}
    </div>
  )
}

export function SettingsScheduling({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  return (
    <div className="space-y-3">
      {show('Working hours', 'hours') ? (
        <SettingsRow label="Working hours">
          <span className="flex items-center gap-1">
            <TimeOnlyField
              ariaLabel="Working hours start"
              value={form.settings.workingHours.start}
              onChange={(start) =>
                form.persistSettings({
                  workingHours: { ...form.settings.workingHours, start: start || '09:00' },
                })
              }
            />
            –
            <TimeOnlyField
              ariaLabel="Working hours end"
              value={form.settings.workingHours.end}
              onChange={(end) =>
                form.persistSettings({
                  workingHours: { ...form.settings.workingHours, end: end || '17:00' },
                })
              }
            />
          </span>
        </SettingsRow>
      ) : null}
      {show('Travel buffer', 'travel') ? (
        <SettingsRow label="Travel buffer (min)">
          <SettingsNumber
            min={0}
            value={form.settings.travelBufferMinutes}
            onChange={(n) => form.persistSettings({ travelBufferMinutes: Math.max(0, n || 0) })}
          />
        </SettingsRow>
      ) : null}
      {show('Min free slot', 'slot') ? (
        <SettingsRow label="Min free slot (min)">
          <SettingsNumber
            min={1}
            value={form.settings.slotMinMinutes}
            onChange={(n) => form.persistSettings({ slotMinMinutes: Math.max(1, n || 30) })}
          />
        </SettingsRow>
      ) : null}
    </div>
  )
}
