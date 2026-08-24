/** Login autostart is release-only. Debug EXEs open a console and wait on Vite. */

export function allowEnableAutostart(isDevBuild: boolean): boolean {
  return !isDevBuild
}

export async function readStartWithWindows(): Promise<boolean> {
  try {
    const { isEnabled } = await import('@tauri-apps/plugin-autostart')
    return await isEnabled()
  } catch {
    return false
  }
}

export async function writeStartWithWindows(on: boolean): Promise<void> {
  if (on && !allowEnableAutostart(import.meta.env.DEV)) {
    throw new Error('Start with Windows is only available in the installed app')
  }
  const { enable, disable } = await import('@tauri-apps/plugin-autostart')
  if (on) await enable()
  else await disable()
}
