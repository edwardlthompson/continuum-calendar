package org.fossify.calendar.continuum

import android.content.Context
import android.provider.CalendarContract
import org.fossify.calendar.R
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.extensions.calDAVHelper
import org.fossify.calendar.extensions.config
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.helpers.BIRTHDAY_EVENT
import org.fossify.calendar.helpers.CALDAV
import org.fossify.calendar.helpers.SOURCE_CONTACT_BIRTHDAY
import org.fossify.calendar.models.CalendarEntity
import org.fossify.calendar.models.Event
import org.fossify.commons.helpers.PERMISSION_READ_CALENDAR
import org.fossify.commons.extensions.hasPermission

/**
 * Hides **Google/Android automated contact birthdays** only.
 *
 * Manual yearly events you created (e.g. "Sofia's Birthday" on Family) are kept.
 * Automated ones are identified via CalendarContract extended property
 * `shared:calendarProviderEventType = BIRTHDAY`, plus Fossify's local contact-birthday source
 * and any dedicated Birthdays calendar.
 *
 * Google Holidays calendars (`en.usa#wendy.h@example.net`) must never be treated as
 * birthday calendars — they share the `#wendy.h@example.net` domain.
 */
object ContinuumBirthdayFilter {
    private const val PROVIDER_TYPE_NAME = "shared:calendarProviderEventType"
    private const val PROVIDER_TYPE_BIRTHDAY = "BIRTHDAY"
    private const val CACHE_TTL_MS = 60_000L

    private val NAME_HINTS = listOf(
        "birthday",
        "birthdays",
        "Geburtstag",
        "Geburtstage",
        "anniversaire",
        "cumpleaños",
        "compleanno",
        "verjaardag",
        "urodziny",
    )

    /** Contacts Birthdays account only — not Holidays / other group calendars. */
    private val GOOGLE_BIRTHDAY_ACCOUNT_HINTS = listOf(
        "addressbook#group.v.calendar.google.com",
        "addressbook#group.calendar.google.com",
    )

    private val HOLIDAY_ACCOUNT_HINTS = listOf(
        "holiday@group",
        "#wendy.h@example.net",
        "holidays",
    )

    @Volatile
    private var cachedProviderBirthdayIds: Set<Long> = emptySet()

    @Volatile
    private var cacheAtMs: Long = 0L

    fun invalidateCache() {
        cacheAtMs = 0L
        cachedProviderBirthdayIds = emptySet()
    }

    fun isHolidayCalendarLabel(raw: String?): Boolean {
        val value = raw?.trim()?.lowercase().orEmpty()
        if (value.isEmpty()) return false
        return HOLIDAY_ACCOUNT_HINTS.any { value.contains(it) }
    }

    fun isBirthdayCalendar(entity: CalendarEntity, localizedBirthdaysLabel: String): Boolean {
        if (entity.type == BIRTHDAY_EVENT) return true
        if (isHolidayCalendarLabel(entity.title) ||
            isHolidayCalendarLabel(entity.caldavDisplayName) ||
            isHolidayCalendarLabel(entity.caldavEmail)
        ) {
            return false
        }
        return looksLikeBirthdayName(entity.title, localizedBirthdaysLabel) ||
            looksLikeBirthdayName(entity.caldavDisplayName, localizedBirthdaysLabel) ||
            looksLikeGoogleBirthdayAccount(entity.caldavEmail) ||
            looksLikeGoogleBirthdayAccount(entity.caldavDisplayName)
    }

    fun looksLikeBirthdayName(raw: String?, localizedBirthdaysLabel: String): Boolean {
        val name = raw?.trim()?.lowercase().orEmpty()
        if (name.isEmpty()) return false
        if (isHolidayCalendarLabel(name)) return false
        val label = localizedBirthdaysLabel.trim().lowercase()
        if (label.isNotEmpty() && name == label) return true
        // Exact calendar title match only — do not treat Family/primary as birthday calendars.
        return NAME_HINTS.any { hint -> name == hint }
    }

    fun looksLikeGoogleBirthdayAccount(raw: String?): Boolean {
        val value = raw?.trim()?.lowercase().orEmpty()
        if (value.isEmpty()) return false
        if (isHolidayCalendarLabel(value)) return false
        return GOOGLE_BIRTHDAY_ACCOUNT_HINTS.any { value.contains(it) }
    }

    /** CalendarContract event _ids marked as automated birthdays. */
    fun providerBirthdayEventIds(context: Context): Set<Long> {
        val now = System.currentTimeMillis()
        if (now - cacheAtMs < CACHE_TTL_MS && cachedProviderBirthdayIds.isNotEmpty()) {
            return cachedProviderBirthdayIds
        }
        if (now - cacheAtMs < CACHE_TTL_MS && cacheAtMs > 0L) {
            return cachedProviderBirthdayIds
        }
        if (!context.hasPermission(PERMISSION_READ_CALENDAR)) {
            return cachedProviderBirthdayIds
        }
        val ids = LinkedHashSet<Long>()
        try {
            context.contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
                "${CalendarContract.ExtendedProperties.NAME}=? AND ${CalendarContract.ExtendedProperties.VALUE}=?",
                arrayOf(PROVIDER_TYPE_NAME, PROVIDER_TYPE_BIRTHDAY),
                null,
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(CalendarContract.ExtendedProperties.EVENT_ID)
                while (cursor.moveToNext()) {
                    if (idx >= 0) ids.add(cursor.getLong(idx))
                }
            }
            ContinuumDiagnostics.i("Loaded ${ids.size} Google/Android automated BIRTHDAY event ids")
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed reading provider BIRTHDAY markers", e)
        }
        cachedProviderBirthdayIds = ids
        cacheAtMs = now
        return ids
    }

    fun birthdayCalendarIds(context: Context): Set<Long> {
        val label = context.getString(R.string.birthdays)
        val ids = LinkedHashSet<Long>()
        try {
            val localId = context.eventsHelper.getLocalBirthdaysCalendarId(createIfNotExists = false)
            if (localId != -1L) ids.add(localId)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("local birthday calendar lookup failed", e)
        }
        try {
            context.calendarsDB.getCalendars().forEach { cal ->
                val id = cal.id ?: return@forEach
                if (isBirthdayCalendar(cal, label)) ids.add(id)
            }
        } catch (e: Exception) {
            ContinuumDiagnostics.w("calendar scan for birthdays failed", e)
        }
        try {
            context.calDAVHelper.getCalDAVCalendars("", false).forEach { remote ->
                if (isHolidayCalendarLabel(remote.displayName) ||
                    isHolidayCalendarLabel(remote.ownerName) ||
                    isHolidayCalendarLabel(remote.accountName)
                ) {
                    return@forEach
                }
                val remoteLooksBirthday =
                    looksLikeBirthdayName(remote.displayName, label) ||
                        looksLikeGoogleBirthdayAccount(remote.ownerName) ||
                        looksLikeGoogleBirthdayAccount(remote.accountName)
                if (!remoteLooksBirthday) return@forEach
                val local = context.calendarsDB.getCalendarWithCalDAVCalendarId(remote.id)
                val id = local?.id
                if (id != null) ids.add(id)
            }
        } catch (e: Exception) {
            ContinuumDiagnostics.w("CalDAV birthday calendar scan failed", e)
        }
        return ids
    }

    fun isGoogleAutomatedBirthday(event: Event, providerBirthdayIds: Set<Long>): Boolean {
        if (event.source == SOURCE_CONTACT_BIRTHDAY) return true
        val remoteId = remoteEventIdFromImportId(event.importId) ?: return false
        return providerBirthdayIds.contains(remoteId)
    }

    /** `Caldav-{calendarId}-{eventId}` → eventId (CalendarContract Events._ID). */
    fun remoteEventIdFromImportId(importId: String): Long? {
        if (!importId.startsWith("$CALDAV-", ignoreCase = true)) return null
        val remote = importId.substringAfterLast('-')
        return remote.toLongOrNull()
    }

    fun shouldHideEvent(
        event: Event,
        birthdayCalendarIds: Set<Long>,
        providerBirthdayIds: Set<Long>,
    ): Boolean {
        if (birthdayCalendarIds.contains(event.calendarId)) return true
        return isGoogleAutomatedBirthday(event, providerBirthdayIds)
    }

    fun applyDisplayCalendarVisibility(context: Context, show: Boolean) {
        invalidateCache()
        val ids = birthdayCalendarIds(context).map { it.toString() }.toSet()
        if (ids.isEmpty()) {
            ContinuumDiagnostics.i("No dedicated birthday calendars found to toggle (show=$show)")
            return
        }
        val cfg = context.config
        if (show) {
            ids.forEach { cfg.addDisplayCalendar(it) }
        } else {
            cfg.removeDisplayCalendars(ids)
        }
        ContinuumDiagnostics.i(
            if (show) "Showing birthday calendars: $ids"
            else "Hiding birthday calendars: $ids",
        )
    }
}
