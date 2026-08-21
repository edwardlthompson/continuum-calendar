export async function openExternal(url: string): Promise<void> {
  try {
    if (typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window) {
      const { openUrl } = await import('@tauri-apps/plugin-opener')
      await openUrl(url)
      return
    }
  } catch {
    /* fall through to window.open */
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}
