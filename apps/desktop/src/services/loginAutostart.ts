/** Login autostart is release-only. Debug builds wait on Vite and must not register. */

export function allowEnableAutostart(isDevBuild: boolean): boolean {
  return !isDevBuild
}

export async function readStartAtLogin(): Promise<boolean> {
  try {
    const { isEnabled } = await import('@tauri-apps/plugin-autostart')
    return await isEnabled()
  } catch {
    return false
  }
}

export async function writeStartAtLogin(on: boolean): Promise<void> {
  if (on && !allowEnableAutostart(import.meta.env.DEV)) {
    throw new Error('Start at login is only available in the installed app')
  }
  const { enable, disable } = await import('@tauri-apps/plugin-autostart')
  if (on) await enable()
  else await disable()
}

/** @deprecated Use readStartAtLogin */
export const readStartWithWindows = readStartAtLogin
/** @deprecated Use writeStartAtLogin */
export const writeStartWithWindows = writeStartAtLogin
