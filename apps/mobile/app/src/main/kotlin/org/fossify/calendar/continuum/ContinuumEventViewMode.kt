package org.fossify.calendar.continuum

import org.fossify.calendar.helpers.HOLIDAY_EVENT
import org.fossify.calendar.helpers.SOURCE_CONTACT_ANNIVERSARY
import org.fossify.calendar.helpers.SOURCE_CONTACT_BIRTHDAY

/**
 * Desktop parity: existing events open read-only first; pencil enters edit.
 * Pure helpers stay unit-testable without Android UI.
 */
object ContinuumEventViewMode {
    const val MODE_VIEW = "view"
    const val MODE_EDIT = "edit"

    fun parseMode(raw: String?, defaultMode: String = MODE_EDIT): String {
        val value = raw?.trim()?.lowercase().orEmpty()
        return when (value) {
            MODE_VIEW -> MODE_VIEW
            MODE_EDIT -> MODE_EDIT
            else -> defaultMode
        }
    }

    fun isViewMode(raw: String?): Boolean = parseMode(raw) == MODE_VIEW

    /**
     * Existing-event opens from Continuum UI default to view (desktop parity).
     * System ACTION_EDIT / ACTION_INSERT / duplicate stay on the editor unless
     * [displayModeExtra] is explicitly `view`.
     */
    fun shouldOpenExistingAsView(
        displayModeExtra: String?,
        intentAction: String?,
        isDuplicate: Boolean,
    ): Boolean {
        if (isDuplicate) return false
        if (intentAction == android.content.Intent.ACTION_EDIT ||
            intentAction == android.content.Intent.ACTION_INSERT
        ) {
            return isViewMode(displayModeExtra)
        }
        // Missing extra (day/week forgot the flag) → view, not edit.
        return parseMode(displayModeExtra, defaultMode = MODE_VIEW) == MODE_VIEW
    }

    /**
     * Hide pencil for holidays / contact birthday-anniversary sources
     * (matches desktop EventDetailCard canEdit gates for holidays).
     */
    fun canShowEditAction(
        eventSource: String?,
        calendarType: Int?,
        calendarTitle: String? = null,
        calendarDisplayName: String? = null,
        calendarEmail: String? = null,
    ): Boolean {
        val source = eventSource.orEmpty()
        if (source == SOURCE_CONTACT_BIRTHDAY || source == SOURCE_CONTACT_ANNIVERSARY) {
            return false
        }
        if (calendarType == HOLIDAY_EVENT) return false
        if (ContinuumBirthdayFilter.isHolidayCalendarLabel(calendarTitle) ||
            ContinuumBirthdayFilter.isHolidayCalendarLabel(calendarDisplayName) ||
            ContinuumBirthdayFilter.isHolidayCalendarLabel(calendarEmail)
        ) {
            return false
        }
        return true
    }

    /** FOSS static map preview (OpenStreetMap.de staticmap) — same pattern as desktop. */
    fun osmStaticMapUrl(
        lat: Double,
        lon: Double,
        width: Int = 400,
        height: Int = 180,
        zoom: Int = 14,
    ): String {
        val w = width.coerceIn(100, 800)
        val h = height.coerceIn(80, 600)
        return "https://staticmap.openstreetmap.de/staticmap.php" +
            "?center=$lat,$lon&zoom=$zoom&size=${w}x$h&markers=$lat,$lon,red-pushpin"
    }

    /** First Photon feature coordinates, or null. */
    fun parsePhotonCoords(json: String): Pair<Double, Double>? {
        val coordsKey = "\"coordinates\""
        var idx = 0
        while (true) {
            val start = json.indexOf(coordsKey, idx)
            if (start < 0) return null
            val bracket = json.indexOf('[', start)
            val end = json.indexOf(']', bracket + 1)
            if (bracket < 0 || end < 0) return null
            val inner = json.substring(bracket + 1, end)
            val parts = inner.split(',').map { it.trim() }
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                if (lat != null && lon != null &&
                    lat in -90.0..90.0 && lon in -180.0..180.0
                ) {
                    return lat to lon
                }
            }
            idx = end + 1
        }
    }
}
