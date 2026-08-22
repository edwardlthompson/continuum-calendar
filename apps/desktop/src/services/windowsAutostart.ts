export async function readStartWithWindows(): Promise<boolean> {
  try {
    const { isEnabled } = await import('@tauri-apps/plugin-autostart')
    return await isEnabled()
  } catch {
    return false
  }
}

export async function writeStartWithWindows(on: boolean): Promise<void> {
  const { enable, disable } = await import('@tauri-apps/plugin-autostart')
  if (on) await enable()
  else await disable()
}
