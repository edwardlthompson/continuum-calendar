export async function openExternal(url: string): Promise<void> {
  const trimmed = url.trim()
  if (!trimmed) throw new Error('Missing URL to open')

  const inTauri = typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
  if (!inTauri) {
    const opened = window.open(trimmed, '_blank', 'noopener,noreferrer')
    if (!opened) throw new Error('Could not open the link (popup blocked)')
    return
  }

  // Prefer plugin-opener; fall back to our native command. Never use window.open in
  // Tauri — WebView returns null without throwing, so OAuth would hang silently.
  let lastError: unknown
  try {
    const { openUrl } = await import('@tauri-apps/plugin-opener')
    await openUrl(trimmed)
    return
  } catch (e) {
    lastError = e
  }

  try {
    const { invoke } = await import('@tauri-apps/api/core')
    await invoke('open_external_url', { url: trimmed })
    return
  } catch (e) {
    lastError = e
  }

  const detail =
    lastError instanceof Error
      ? lastError.message
      : typeof lastError === 'string'
        ? lastError
        : 'unknown error'
  throw new Error(`Could not open your browser: ${detail}`)
}
