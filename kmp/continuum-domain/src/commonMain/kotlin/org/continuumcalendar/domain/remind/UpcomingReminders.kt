package org.continuumcalendar.domain.remind

import org.continuumcalendar.domain.agenda.isAllDayEvent
import org.continuumcalendar.domain.conflict.eventStartMs
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.ContinuumSettings

data class ReminderDue(
    val eventId: String,
    val title: String,
    val fireAtMs: Long,
    val startMs: Long,
)

/**
 * Events whose reminder window is due in (now - skew, now + lookAhead].
 * Uses event.reminders or settings.defaultReminderMinutes.
 */
fun dueReminders(
    events: List<CalendarEvent>,
    settings: ContinuumSettings,
    nowMs: Long,
    lookAheadMs: Long = 60_000,
    skewMs: Long = 30_000,
    alreadyFired: Set<String> = emptySet(),
): List<ReminderDue> {
    if (!settings.notificationEnabled) return emptyList()
    val out = mutableListOf<ReminderDue>()
    for (ev in events) {
        if (isAllDayEvent(ev)) continue
        if (ev.id in alreadyFired) continue
        val start = eventStartMs(ev)
        if (start <= 0L) continue
        val minutes = ev.reminders.firstOrNull()?.minutes ?: settings.defaultReminderMinutes
        val fireAt = start - minutes * 60_000L
        if (fireAt in (nowMs - skewMs)..(nowMs + lookAheadMs)) {
            out += ReminderDue(ev.id, ev.title, fireAt, start)
        }
    }
    return out.sortedBy { it.fireAtMs }
}
