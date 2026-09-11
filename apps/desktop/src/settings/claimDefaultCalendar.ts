import { invoke } from '@tauri-apps/api/core'

/** Register Continuum as the default calendar handler (Linux MIME + webcal). */
export async function claimDefaultCalendar(): Promise<string> {
  return invoke<string>('claim_default_calendar')
}

export async function isDefaultCalendar(): Promise<boolean> {
  try {
    return await invoke<boolean>('is_default_calendar')
  } catch {
    return false
  }
}
