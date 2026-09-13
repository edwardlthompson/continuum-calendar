import type { CalendarEvent } from '@continuum/shared'
import { ensureFreshTokens } from '../auth/googleAuth'
import { hasTasksScope, mapGoogleTask } from './googleTasksMap'

export { hasTasksScope, mapGoogleTask, queueGoogleTasksScope, TASKS_CALENDAR_ID } from './googleTasksMap'

const TASKS_BASE = 'https://www.googleapis.com/tasks/v1'

export async function listGoogleTasks(): Promise<CalendarEvent[]> {
  const tokens = await ensureFreshTokens()
  if (!tokens || !hasTasksScope(tokens.scope)) return []
  const headers = new Headers()
  headers.set('Authorization', `Bearer ${tokens.accessToken}`)
  const listsRes = await fetch(`${TASKS_BASE}/users/@me/lists?maxResults=100`, { headers })
  if (!listsRes.ok) throw new Error(`Tasks lists failed: ${listsRes.status}`)
  const lists = (await listsRes.json()) as { items?: Array<{ id?: string }> }
  const out: CalendarEvent[] = []
  for (const list of lists.items ?? []) {
    const listId = list.id?.trim()
    if (!listId) continue
    const res = await fetch(
      `${TASKS_BASE}/lists/${encodeURIComponent(listId)}/tasks?showCompleted=false&maxResults=100`,
      { headers },
    )
    if (!res.ok) throw new Error(`Tasks failed: ${res.status}`)
    const data = (await res.json()) as { items?: Array<{ id?: string; title?: string; due?: string; notes?: string }> }
    for (const item of data.items ?? []) {
      const mapped = mapGoogleTask(item, listId)
      if (mapped) out.push(mapped)
    }
  }
  return out
}
