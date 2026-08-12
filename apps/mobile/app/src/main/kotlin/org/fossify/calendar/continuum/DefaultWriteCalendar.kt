package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.helpers.Config
import org.fossify.calendar.models.CalendarEntity

/** Map Fossify Room calendars ↔ Continuum `defaultWriteCalendarId` logical ids. */
object DefaultWriteCalendar {
    fun logicalIdFor(calendar: CalendarEntity): String {
        return when {
            calendar.caldavCalendarId != 0 -> {
                val email = calendar.caldavEmail.lowercase()
                if (email.endsWith("@gmail.com") || email.contains("google")) {
                    ContinuumConsts.logicalId(CalendarSource.GOOGLE, calendar.caldavCalendarId.toString())
                } else {
                    ContinuumConsts.logicalId(CalendarSource.CALDAV, calendar.caldavCalendarId.toString())
                }
            }
            else -> ContinuumConsts.logicalId(CalendarSource.LOCAL, (calendar.id ?: -1L).toString())
        }
    }

    fun resolveLocalId(context: Context, logicalId: String): Long? {
        if (logicalId == "last_used" || logicalId.isBlank()) return -1L
        val calendars = context.calendarsDB.getCalendars()
        calendars.firstOrNull { logicalIdFor(it) == logicalId }?.id?.let { return it }

        val parts = logicalId.split(':', limit = 2)
        if (parts.size != 2) return null
        val (source, id) = parts
        return when (source) {
            "local" -> id.toLongOrNull()?.takeIf { lid -> calendars.any { it.id == lid } }
            "caldav" -> {
                val caldavId = id.toIntOrNull() ?: return null
                calendars.firstOrNull { it.caldavCalendarId == caldavId }?.id
            }
            "google" -> {
                if (id == "primary") {
                    calendars.firstOrNull { isGoogleCalDav(it) }?.id
                } else {
                    val caldavId = id.toIntOrNull()
                    calendars.firstOrNull { it.caldavCalendarId == caldavId }?.id
                        ?: calendars.firstOrNull { isGoogleCalDav(it) }?.id
                }
            }
            else -> null
        }
    }

    private fun isGoogleCalDav(calendar: CalendarEntity): Boolean {
        if (calendar.caldavCalendarId == 0) return false
        val email = calendar.caldavEmail.lowercase()
        return email.endsWith("@gmail.com") || email.contains("google")
    }

    fun applyLogicalIdToConfig(context: Context, logicalId: String) {
        val cfg = Config.newInstance(context)
        val resolved = resolveLocalId(context, logicalId) ?: return
        cfg.defaultCalendarId = resolved
        if (resolved != -1L) {
            context.calendarsDB.getCalendarWithId(resolved)?.let {
                if (it.caldavCalendarId != 0) {
                    cfg.lastUsedCaldavCalendarId = it.caldavCalendarId
                }
            }
        }
    }
}
