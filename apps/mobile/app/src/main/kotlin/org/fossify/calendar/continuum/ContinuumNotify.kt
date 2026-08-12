package org.fossify.calendar.continuum

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.fossify.calendar.R
import org.fossify.calendar.activities.MainActivity
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.helpers.LOCAL_CALENDAR_ID
import org.fossify.calendar.helpers.SOURCE_IMPORTED_ICS
import org.fossify.calendar.helpers.SOURCE_SIMPLE_CALENDAR
import org.fossify.calendar.models.Event
import org.json.JSONObject

/** Per-calendar New-event / Reminder gates + new-event notification channel. */
object ContinuumNotify {
    private const val PREFS = "continuum_notify"
    private const val KEY_LAST = "last_notified_peer_ids"
    private const val PRUNE_MS = 90L * 24 * 60 * 60 * 1000

    fun logicalIdForCalendarId(calendarId: Long, sourceHint: String? = null): String {
        return when {
            sourceHint == "holidays" || sourceHint?.startsWith("holidays") == true ->
                ContinuumConsts.logicalId(CalendarSource.HOLIDAYS, calendarId.toString())
            calendarId == LOCAL_CALENDAR_ID ->
                ContinuumConsts.logicalId(CalendarSource.LOCAL, "local-default")
            else -> ContinuumConsts.logicalId(CalendarSource.LOCAL, calendarId.toString())
        }
    }

    fun logicalIdForEvent(event: Event): String {
        val source = when (event.source) {
            SOURCE_IMPORTED_ICS -> CalendarSource.ICS_IMPORT
            SOURCE_SIMPLE_CALENDAR -> CalendarSource.LOCAL
            else -> if (event.source.contains("holiday", ignoreCase = true)) {
                CalendarSource.HOLIDAYS
            } else {
                CalendarSource.LOCAL
            }
        }
        val calKey =
            if (event.calendarId == LOCAL_CALENDAR_ID) "local-default" else event.calendarId.toString()
        return ContinuumConsts.logicalId(source, calKey)
    }

    fun resolvePrefs(settings: ContinuumSettings, logicalId: String): CalendarNotifyPrefs {
        val isHoliday = logicalId.startsWith("holidays:")
        val d = CalendarNotifyPrefs(newEvent = !isHoliday, reminder = true)
        val raw = settings.calendarNotifyPrefs[logicalId] ?: return d
        return CalendarNotifyPrefs(
            newEvent = raw.newEvent,
            reminder = raw.reminder,
        )
    }

    fun reminderAllowed(context: Context, event: Event): Boolean {
        val settings = ContinuumSettingsSync(context).loadLocal()
        if (!settings.notificationEnabled) return false
        return resolvePrefs(settings, logicalIdForEvent(event)).reminder
    }

    fun newEventAllowed(context: Context, logicalId: String): Boolean {
        val settings = ContinuumSettingsSync(context).loadLocal()
        if (!settings.notificationEnabled) return false
        return resolvePrefs(settings, logicalId).newEvent
    }

    fun notifyNewPeerEvent(
        context: Context,
        peerId: String,
        calendarId: Long,
        title: String,
        logicalId: String,
    ) {
        try {
            if (!newEventAllowed(context, logicalId)) return
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            if (!markNotified(context, "$calendarId:$peerId")) return

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "continuum_new_event_$calendarId"
            val calTitle =
                context.calendarsDB.getCalendarWithId(calendarId)?.getDisplayTitle()
                    ?: context.getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        context.getString(R.string.continuum_new_event_channel, calTitle),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.continuum_notify_new_event)
                        enableLights(true)
                        enableVibration(true)
                    },
                )
            }
            val open = PendingIntent.getActivity(
                context,
                peerId.hashCode(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_calendar_vector)
                .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
                .setContentText(context.getString(R.string.continuum_new_event_notification_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            nm.notify(("ne:$peerId").hashCode(), n)
        } catch (e: Exception) {
            ContinuumDiagnostics.e("New-event notify failed", e)
        }
    }

    /** @return false if already notified (dedupe). */
    private fun markNotified(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val map = try {
            JSONObject(prefs.getString(KEY_LAST, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        if (map.has(key)) return false
        map.put(key, now)
        val pruneBefore = now - PRUNE_MS
        val keys = map.keys().asSequence().toList()
        for (k in keys) {
            if (map.optLong(k, 0L) < pruneBefore) map.remove(k)
        }
        prefs.edit().putString(KEY_LAST, map.toString()).apply()
        return true
    }
}
