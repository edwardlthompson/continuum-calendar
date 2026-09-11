import type { CalendarEvent } from '../../../../packages/shared/src/events.ts'
import { GOOGLE_SCOPES } from '../../../../packages/shared/src/oauth.ts'

export const TASKS_CALENDAR_ID = 'google-tasks'

export function hasTasksScope(scope: string): boolean {
  return scope.includes(GOOGLE_SCOPES.tasks)
}

export function queueGoogleTasksScope(): void {
  sessionStorage.setItem('continuum.oauth.extraScopes', GOOGLE_SCOPES.tasks)
}

export function mapGoogleTask(
  raw: { id?: string; title?: string; due?: string; notes?: string },
  listId: string,
): CalendarEvent | null {
  const id = raw.id?.trim()
  const due = raw.due?.trim()
  if (!id || !due) return null
  const day = due.slice(0, 10)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) return null
  return {
    id: `${listId}:${id}`,
    calendarId: TASKS_CALENDAR_ID,
    title: raw.title?.trim() || 'Task',
    description: raw.notes ? String(raw.notes) : undefined,
    start: day,
    end: day,
    allDay: true,
    source: 'google',
    busy: false,
    readOnly: true,
  }
}
