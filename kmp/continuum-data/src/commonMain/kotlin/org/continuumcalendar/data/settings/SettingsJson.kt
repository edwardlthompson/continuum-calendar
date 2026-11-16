package org.continuumcalendar.data.settings

import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours
import org.continuumcalendar.domain.model.normalizeFirstDayOfWeek
import org.continuumcalendar.domain.model.normalizeWeekHourDisplay

/** Minimal camelCase ContinuumSettings JSON (peer-compatible subset). */
fun continuumSettingsToJson(s: ContinuumSettings): String {
    val prefs = s.calendarNotifyPrefs.entries.joinToString(",") { (k, v) ->
        """${jsonString(k)}:{"newEvent":${v.newEvent},"reminder":${v.reminder}}"""
    }
    return buildString {
        append('{')
        append("\"themeMode\":${jsonString(s.themeMode)},")
        append("\"showEmptyDaysInAgenda\":${s.showEmptyDaysInAgenda},")
        append("\"rollingWeekFromToday\":${s.rollingWeekFromToday},")
        append("\"weeklyViewDays\":${s.weeklyViewDays},")
        append("\"workingHours\":{\"start\":${jsonString(s.workingHours.start)},\"end\":${jsonString(s.workingHours.end)}},")
        append("\"defaultReminderMinutes\":${s.defaultReminderMinutes},")
        append("\"notificationEnabled\":${s.notificationEnabled},")
        append("\"defaultSnoozeMinutes\":${s.defaultSnoozeMinutes},")
        append("\"visibleCalendarIds\":[${s.visibleCalendarIds.joinToString(",") { jsonString(it) }}],")
        append("\"defaultWriteCalendarId\":${jsonString(s.defaultWriteCalendarId)},")
        append("\"agendaRangeDays\":${s.agendaRangeDays},")
        append("\"slotMinMinutes\":${s.slotMinMinutes},")
        append("\"redactTitlesInScreenshots\":${s.redactTitlesInScreenshots},")
        append("\"travelBufferMinutes\":${s.travelBufferMinutes},")
        append("\"agendaDensity\":${jsonString(s.agendaDensity)},")
        append("\"showContactBirthdays\":${s.showContactBirthdays},")
        append("\"use24HourFormat\":${s.use24HourFormat},")
        append("\"firstDayOfWeek\":${s.firstDayOfWeek},")
        append("\"useGoogleCalendar\":${s.useGoogleCalendar},")
        append("\"calendarNotifyPrefs\":{$prefs},")
        append("\"weekHourDisplay\":${jsonString(s.weekHourDisplay)},")
        append("\"holidayPack\":${jsonString(s.holidayPack)},")
        append("\"showGoogleTasks\":${s.showGoogleTasks}")
        s.secondaryTimeZone?.let { append(",\"secondaryTimeZone\":${jsonString(it)}") }
        append('}')
    }
}

fun parseContinuumSettingsJson(raw: String): ContinuumSettings? {
    if (raw.isBlank() || raw.length > 256_000) return null
    return runCatching {
        ContinuumSettings(
            themeMode = stringField(raw, "themeMode") ?: "system",
            showEmptyDaysInAgenda = boolField(raw, "showEmptyDaysInAgenda") ?: true,
            rollingWeekFromToday = boolField(raw, "rollingWeekFromToday") ?: true,
            weeklyViewDays = (intField(raw, "weeklyViewDays") ?: 7).coerceIn(1, 14),
            workingHours = WorkingHours(
                start = nestedString(raw, "workingHours", "start") ?: "09:00",
                end = nestedString(raw, "workingHours", "end") ?: "17:00",
            ),
            defaultReminderMinutes = intField(raw, "defaultReminderMinutes") ?: 10,
            notificationEnabled = boolField(raw, "notificationEnabled") ?: true,
            defaultSnoozeMinutes = intField(raw, "defaultSnoozeMinutes") ?: 10,
            visibleCalendarIds = stringArray(raw, "visibleCalendarIds"),
            defaultWriteCalendarId = stringField(raw, "defaultWriteCalendarId") ?: "google:primary",
            agendaRangeDays = (intField(raw, "agendaRangeDays") ?: 30).coerceIn(1, 90),
            slotMinMinutes = intField(raw, "slotMinMinutes") ?: 30,
            redactTitlesInScreenshots = boolField(raw, "redactTitlesInScreenshots") ?: false,
            travelBufferMinutes = intField(raw, "travelBufferMinutes") ?: 0,
            agendaDensity = stringField(raw, "agendaDensity") ?: "comfortable",
            secondaryTimeZone = stringField(raw, "secondaryTimeZone"),
            showContactBirthdays = boolField(raw, "showContactBirthdays") ?: true,
            use24HourFormat = boolField(raw, "use24HourFormat") ?: false,
            firstDayOfWeek = normalizeFirstDayOfWeek(intField(raw, "firstDayOfWeek") ?: 0),
            useGoogleCalendar = boolField(raw, "useGoogleCalendar") ?: true,
            calendarNotifyPrefs = emptyMap(),
            weekHourDisplay = normalizeWeekHourDisplay(stringField(raw, "weekHourDisplay")),
            holidayPack = stringField(raw, "holidayPack") ?: "none",
            showGoogleTasks = boolField(raw, "showGoogleTasks") ?: false,
        )
    }.getOrNull()
}

private fun jsonString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun stringField(json: String, key: String): String? {
    val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
    return re.find(json)?.groupValues?.get(1)
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")
}

private fun intField(json: String, key: String): Int? {
    val re = Regex(""""$key"\s*:\s*(-?\d+)""")
    return re.find(json)?.groupValues?.get(1)?.toIntOrNull()
}

private fun boolField(json: String, key: String): Boolean? {
    val re = Regex(""""$key"\s*:\s*(true|false)""")
    return re.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}

private fun stringArray(json: String, key: String): List<String> {
    val re = Regex(""""$key"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
    val inner = re.find(json)?.groupValues?.get(1) ?: return emptyList()
    return Regex(""""((?:\\.|[^"\\])*)"""").findAll(inner).map {
        it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
    }.toList()
}

private fun nestedString(json: String, obj: String, key: String): String? {
    val re = Regex(""""$obj"\s*:\s*\{([^}]*)\}""")
    val inner = re.find(json)?.groupValues?.get(1) ?: return null
    return stringField("{$inner}", key)
}
