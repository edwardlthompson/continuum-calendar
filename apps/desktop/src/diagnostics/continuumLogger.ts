/** FOSS local diagnostics — no network telemetry. Persists to localStorage + optional download. */

const LOG_KEY = 'continuum.error.log'
const CRASH_KEY = 'continuum.last.crash'
const MAX_CHARS = 200_000

export type LogLevel = 'info' | 'warn' | 'error' | 'crash'

function stamp(): string {
  return new Date().toISOString()
}

function readLog(): string {
  try {
    return localStorage.getItem(LOG_KEY) ?? ''
  } catch {
    return ''
  }
}

function writeLog(text: string): void {
  try {
    const trimmed = text.length > MAX_CHARS ? `…truncated…\n${text.slice(-MAX_CHARS / 2)}` : text
    localStorage.setItem(LOG_KEY, trimmed)
  } catch {
    /* quota / private mode */
  }
}

function append(level: LogLevel, message: string, err?: unknown): void {
  const detail =
    err instanceof Error
      ? `${err.name}: ${err.message}\n${err.stack ?? ''}`
      : err != null
        ? String(err)
        : ''
  const line = `[${stamp()}] ${level.toUpperCase()} ${message}${detail ? `\n${detail}` : ''}\n`
  if (level === 'error' || level === 'crash') {
    console.error(message, err ?? '')
  } else if (level === 'warn') {
    console.warn(message, err ?? '')
  } else {
    console.info(message)
  }
  writeLog(readLog() + line)
}

export const continuumLogger = {
  info(message: string) {
    append('info', message)
  },
  warn(message: string, err?: unknown) {
    append('warn', message, err)
  },
  error(message: string, err?: unknown) {
    append('error', message, err)
  },
  crash(message: string, err?: unknown) {
    append('crash', message, err)
    try {
      const body = `[${stamp()}] ${message}\n${err instanceof Error ? err.stack : String(err ?? '')}`
      localStorage.setItem(CRASH_KEY, body.slice(0, 8000))
    } catch {
      /* ignore */
    }
  },
  getLog(): string {
    return readLog()
  },
  clearLog() {
    try {
      localStorage.removeItem(LOG_KEY)
    } catch {
      /* ignore */
    }
  },
  consumeLastCrash(): string | null {
    try {
      const v = localStorage.getItem(CRASH_KEY)
      if (v) localStorage.removeItem(CRASH_KEY)
      return v
    } catch {
      return null
    }
  },
  downloadLog() {
    const blob = new Blob([readLog() || '(empty)'], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `continuum-errors-${Date.now()}.log`
    a.click()
    URL.revokeObjectURL(url)
  },
}

/** Install global handlers once. */
export function installDesktopDiagnostics(): void {
  window.addEventListener('error', (ev) => {
    continuumLogger.crash(`window.error: ${ev.message}`, ev.error ?? ev.message)
  })
  window.addEventListener('unhandledrejection', (ev) => {
    continuumLogger.crash('unhandledrejection', ev.reason)
  })
  continuumLogger.info('Desktop diagnostics installed')
}
