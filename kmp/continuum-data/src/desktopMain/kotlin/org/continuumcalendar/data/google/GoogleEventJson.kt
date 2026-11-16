package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.CalendarAttendee
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.EventReminder

internal object GoogleEventJson {
    fun mapEvent(raw: String, calendarId: String): CalendarEvent? {
        val id = stringField(raw, "id") ?: return null
        val startBlock = objectField(raw, "start") ?: "{}"
        val endBlock = objectField(raw, "end") ?: "{}"
        val startDt = stringField(startBlock, "dateTime")
        val startDate = stringField(startBlock, "date")
        val endDt = stringField(endBlock, "dateTime")
        val endDate = stringField(endBlock, "date")
        val allDay = startDate != null && startDt == null
        val start = startDt ?: startDate ?: return null
        val end = endDt ?: endDate ?: start
        val eventType = stringField(raw, "eventType")
        val transparency = stringField(raw, "transparency")
        val isBirthday = eventType.equals("birthday", ignoreCase = true)
        val visibilityRaw = stringField(raw, "visibility")
        val visibility = when (visibilityRaw) {
            "public", "private" -> visibilityRaw
            else -> visibilityRaw ?: "default"
        }
        return CalendarEvent(
            id = id,
            calendarId = calendarId,
            title = stringField(raw, "summary") ?: "(No title)",
            description = stringField(raw, "description"),
            location = stringField(raw, "location"),
            start = start,
            end = end,
            allDay = allDay,
            attendees = mapAttendees(raw),
            reminders = mapReminders(raw),
            source = CalendarSource.GOOGLE,
            etag = stringField(raw, "etag"),
            updated = stringField(raw, "updated"),
            htmlLink = stringField(raw, "htmlLink"),
            busy = transparency != "transparent" && !isBirthday,
            eventType = eventType,
            recurrence = stringArray(raw, "recurrence"),
            recurringEventId = stringField(raw, "recurringEventId"),
            timeZone = stringField(startBlock, "timeZone"),
            visibility = visibility,
            readOnly = false,
        )
    }

    fun eventBody(event: CalendarEvent): String {
        val start = if (event.allDay) {
            """{"date":${jsonString(event.start.take(10))}}"""
        } else {
            val tz = event.timeZone?.takeIf { it.isNotBlank() }
            if (tz != null) {
                """{"dateTime":${jsonString(event.start)},"timeZone":${jsonString(tz)}}"""
            } else {
                """{"dateTime":${jsonString(event.start)}}"""
            }
        }
        val end = if (event.allDay) {
            """{"date":${jsonString(event.end.take(10))}}"""
        } else {
            val tz = event.timeZone?.takeIf { it.isNotBlank() }
            if (tz != null) {
                """{"dateTime":${jsonString(event.end)},"timeZone":${jsonString(tz)}}"""
            } else {
                """{"dateTime":${jsonString(event.end)}}"""
            }
        }
        val remindersJson = if (event.reminders.isNotEmpty()) {
            val overrides = event.reminders.joinToString(",") { r ->
                """{"method":${jsonString(r.method)},"minutes":${r.minutes}}"""
            }
            """{"useDefault":false,"overrides":[$overrides]}"""
        } else {
            """{"useDefault":true}"""
        }
        val attendeesJson = event.attendees
            .filter { it.email.isNotBlank() }
            .joinToString(",") { a ->
                buildString {
                    append('{')
                    append("\"email\":${jsonString(a.email)}")
                    a.displayName?.takeIf { it.isNotBlank() }?.let {
                        append(",\"displayName\":${jsonString(it)}")
                    }
                    if (a.optional) append(",\"optional\":true")
                    append('}')
                }
            }
        return buildString {
            append('{')
            append("\"summary\":${jsonString(event.title)},")
            append("\"start\":$start,")
            append("\"end\":$end,")
            append("\"reminders\":$remindersJson,")
            append("\"transparency\":${jsonString(if (event.busy) "opaque" else "transparent")},")
            append("\"visibility\":${jsonString(event.visibility ?: "default")}")
            event.description?.let { append(",\"description\":${jsonString(it)}") }
            event.location?.let { append(",\"location\":${jsonString(it)}") }
            if (attendeesJson.isNotBlank()) {
                append(",\"attendees\":[$attendeesJson]")
            }
            if (event.recurrence.isNotEmpty()) {
                val rules = event.recurrence.joinToString(",") { jsonString(it) }
                append(",\"recurrence\":[$rules]")
            }
            append('}')
        }
    }

    fun mapReminders(raw: String): List<EventReminder> {
        val block = objectField(raw, "reminders") ?: return emptyList()
        val overrides = objectArray(block, "overrides")
        if (overrides.isEmpty()) return emptyList()
        return overrides.mapNotNull { o ->
            val minutes = intField(o, "minutes") ?: return@mapNotNull null
            EventReminder(
                minutes = minutes,
                method = stringField(o, "method") ?: "popup",
            )
        }
    }

    fun mapAttendees(raw: String): List<CalendarAttendee> {
        return objectArray(raw, "attendees").mapNotNull { a ->
            val email = stringField(a, "email")?.trim().orEmpty()
            if (email.isEmpty()) return@mapNotNull null
            CalendarAttendee(
                email = email,
                displayName = stringField(a, "displayName"),
                responseStatus = stringField(a, "responseStatus"),
                optional = a.contains("\"optional\":true"),
            )
        }
    }

    fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    fun intField(json: String, key: String): Int? =
        Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()

    fun objectField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*\{""")
        val m = re.find(json) ?: return null
        val start = m.range.last
        var depth = 1
        var i = start + 1
        while (i < json.length && depth > 0) {
            when (json[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        return json.substring(start, i)
    }

    fun objectArray(json: String, key: String): List<String> {
        val re = Regex(""""$key"\s*:\s*\[""")
        val m = re.find(json) ?: return emptyList()
        var i = m.range.last + 1
        val out = mutableListOf<String>()
        while (i < json.length) {
            while (i < json.length && json[i].isWhitespace()) i++
            if (i >= json.length || json[i] == ']') break
            if (json[i] != '{') break
            val start = i
            var depth = 0
            while (i < json.length) {
                when (json[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            i++
                            out += json.substring(start, i)
                            break
                        }
                    }
                }
                i++
            }
            while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
        }
        return out
    }

    fun stringArray(json: String, key: String): List<String> {
        val re = Regex(""""$key"\s*:\s*\[""")
        val m = re.find(json) ?: return emptyList()
        var i = m.range.last + 1
        val out = mutableListOf<String>()
        while (i < json.length) {
            while (i < json.length && json[i].isWhitespace()) i++
            if (i >= json.length || json[i] == ']') break
            if (json[i] == '"') {
                i++
                val start = i
                while (i < json.length) {
                    if (json[i] == '\\' && i + 1 < json.length) {
                        i += 2
                        continue
                    }
                    if (json[i] == '"') break
                    i++
                }
                out += json.substring(start, i)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                if (i < json.length) i++
            } else {
                break
            }
            while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
        }
        return out
    }
}
