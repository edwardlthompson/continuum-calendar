import { HOLIDAY_PACKS } from '../services/holidayPacks'
import { textMatches } from './settingsCatalog'
import { SettingsFieldSelect, SettingsNumber, SettingsRow } from './settingsUi'
import type { SettingsSectionProps } from './settingsTypes'

const WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

export function SettingsCalendar({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  return (
    <div className="space-y-3">
      {show('First day of week', 'week') ? (
        <SettingsRow label="First day of week">
          <SettingsFieldSelect
            value={form.settings.firstDayOfWeek}
            onChange={(v) => form.persistSettings({ firstDayOfWeek: Number(v) || 0 })}
          >
            {WEEKDAYS.map((name, i) => (
              <option key={name} value={i}>
                {name}
              </option>
            ))}
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
      {show('Weekly view days', 'week') ? (
        <SettingsRow label="Weekly view days">
          <SettingsNumber
            min={1}
            max={14}
            value={form.settings.weeklyViewDays}
            onChange={(n) => form.persistSettings({ weeklyViewDays: Math.min(14, Math.max(1, n || 7)) })}
          />
        </SettingsRow>
      ) : null}
      {show('Rolling week', 'week') ? (
        <SettingsRow label="Rolling week from today">
          <input
            type="checkbox"
            checked={form.settings.rollingWeekFromToday}
            onChange={(e) => form.persistSettings({ rollingWeekFromToday: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
      {show('Holiday', 'holidays', 'pack') ? (
        <SettingsRow label="Holiday pack">
          <SettingsFieldSelect value={form.holidayPack} onChange={(v) => form.setHolidayPack(v as typeof form.holidayPack)}>
            {HOLIDAY_PACKS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.label}
              </option>
            ))}
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
      {show('Default calendar', 'calendar') ? (
        <SettingsRow label="Default calendar for new events" column>
          <select
            value={form.settings.defaultWriteCalendarId}
            onChange={(e) => form.persistSettings({ defaultWriteCalendarId: e.target.value })}
            className="rounded border border-[var(--cc-border)] cc-native-field px-1 py-1"
          >
            {form.displayCalendars
              .filter((c) => c.writable !== false && c.source !== 'holidays')
              .map((c) => (
                <option key={c.logicalId} value={c.logicalId}>
                  {c.displayName} ({c.source})
                </option>
              ))}
          </select>
        </SettingsRow>
      ) : null}
      {show('Show empty days', 'agenda', 'open') ? (
        <SettingsRow label="Show empty days in agenda">
          <input
            type="checkbox"
            checked={form.settings.showEmptyDaysInAgenda}
            onChange={(e) => form.persistSettings({ showEmptyDaysInAgenda: e.target.checked })}
          />
        </SettingsRow>
      ) : null}
      {show('Agenda range', 'agenda') ? (
        <SettingsRow label="Agenda range (days)">
          <SettingsNumber
            min={1}
            max={90}
            value={form.settings.agendaRangeDays}
            onChange={(n) => form.persistSettings({ agendaRangeDays: Math.min(90, Math.max(1, n || 30)) })}
          />
        </SettingsRow>
      ) : null}
      {show('Agenda density', 'agenda', 'compact') ? (
        <SettingsRow label="Agenda density">
          <SettingsFieldSelect
            value={form.settings.agendaDensity}
            onChange={(v) =>
              form.persistSettings({ agendaDensity: v as typeof form.settings.agendaDensity })
            }
          >
            <option value="comfortable">Comfortable</option>
            <option value="compact">Compact</option>
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
    </div>
  )
}
