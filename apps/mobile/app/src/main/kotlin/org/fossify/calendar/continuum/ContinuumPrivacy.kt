package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.extensions.config
import org.fossify.calendar.models.CalendarEntity

/** Privacy mode: hide Google CalDAV calendars from the display set (local peers stay). */
object ContinuumPrivacy {
    fun isGoogleCalDav(calendar: CalendarEntity): Boolean {
        if (calendar.caldavCalendarId == 0) return false
        val email = calendar.caldavEmail.lowercase()
        return email.endsWith("@gmail.com") || email.contains("google")
    }

    fun googleCalendarIds(context: Context): Set<Long> {
        return try {
            context.calendarsDB.getCalendars()
                .filter { isGoogleCalDav(it) }
                .mapNotNull { it.id }
                .toSet()
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed listing Google calendars for privacy", e)
            emptySet()
        }
    }

    fun applyGoogleCalendarVisibility(context: Context, useGoogle: Boolean) {
        val ids = googleCalendarIds(context).map { it.toString() }.toSet()
        if (ids.isEmpty()) return
        val cfg = context.config
        if (useGoogle) {
            ids.forEach { cfg.addDisplayCalendar(it) }
        } else {
            cfg.removeDisplayCalendars(ids)
        }
        ContinuumDiagnostics.i(
            if (useGoogle) "Showing Google calendars: $ids"
            else "Privacy mode hiding Google calendars: $ids",
        )
    }
}
