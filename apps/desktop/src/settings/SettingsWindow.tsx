import { saveWindowBehavior } from '../services/windowBehavior'
import { writeStartWithWindows } from '../services/windowsAutostart'
import { DESKTOP_HOTKEY_CATALOG } from '../hooks/desktopHotkeys'
import { textMatches } from './settingsCatalog'
import { SettingsFieldSelect, SettingsRow } from './settingsUi'
import type { CloseTarget, MinimizeTarget, SettingsSectionProps } from './settingsTypes'

export function SettingsWindow({ form, query }: SettingsSectionProps) {
  const show = (...labels: string[]) => textMatches(query, ...labels)
  const showShortcuts = show(
    'Keyboard',
    'shortcut',
    'hotkey',
    'Today',
    'Agenda',
    'Search',
    'Jump',
  )
  return (
    <div className="space-y-3">
      {show('Minimize', 'taskbar', 'tray') ? (
        <SettingsRow label="Minimize to">
          <SettingsFieldSelect
            value={form.windowBehavior.minimizeTo}
            onChange={(v) =>
              form.setWindowBehavior(
                saveWindowBehavior({ ...form.windowBehavior, minimizeTo: v as MinimizeTarget }),
              )
            }
          >
            <option value="taskbar">Taskbar</option>
            <option value="tray">Notification area</option>
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
      {show('Close', 'quit', 'tray', 'notification') ? (
        <SettingsRow label="Close button">
          <SettingsFieldSelect
            value={form.windowBehavior.closeTo}
            onChange={(v) =>
              form.setWindowBehavior(
                saveWindowBehavior({ ...form.windowBehavior, closeTo: v as CloseTarget }),
              )
            }
          >
            <option value="tray">Notification area</option>
            <option value="quit">Quit Continuum</option>
          </SettingsFieldSelect>
        </SettingsRow>
      ) : null}
      {show('Start', 'Windows', 'boot', 'startup', 'login') ? (
        <SettingsRow label="Start with Windows">
          <input
            type="checkbox"
            checked={form.startWithWindows}
            onChange={(e) => {
              const on = e.target.checked
              form.setStartWithWindows(on)
              void writeStartWithWindows(on).catch((err: unknown) => {
                form.setStartWithWindows(!on)
                form.flash(err instanceof Error ? err.message : 'Could not update Start with Windows')
              })
            }}
          />
        </SettingsRow>
      ) : null}
      {showShortcuts ? (
        <section className="space-y-2 border-t border-[var(--cc-border)] pt-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--cc-muted)]">
            Keyboard shortcuts
          </h3>
          <p className="text-xs text-[var(--cc-muted)]">
            Single keys only — not while typing in a field. Hover toolbar buttons for hints.
          </p>
          <ul className="space-y-1">
            {DESKTOP_HOTKEY_CATALOG.map((row) => (
              <li
                key={row.label}
                className="flex items-center justify-between gap-3 rounded border border-[var(--cc-border)] px-2 py-1"
              >
                <span>{row.label}</span>
                <kbd className="shrink-0 rounded bg-[var(--cc-accent-soft)] px-1.5 py-0.5 font-mono text-xs">
                  {row.hint}
                </kbd>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  )
}
