package org.continuumcalendar.domain.ics

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource

/** Minimal VEVENT import/export for Continuum-owned events. */
fun exportEventsToIcs(events: List<CalendarEvent>): String {
    val body = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//Continuum Calendar//CMP//EN")
        appendLine("CALSCALE:GREGORIAN")
        for (ev in events) {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${escape(ev.id)}@continuum")
            appendLine("SUMMARY:${escape(ev.title)}")
            if (ev.allDay || ev.start.length == 10) {
                appendLine("DTSTART;VALUE=DATE:${ev.start.take(10).replace("-", "")}")
                appendLine("DTEND;VALUE=DATE:${ev.end.take(10).replace("-", "")}")
            } else {
                appendLine("DTSTART:${toIcsUtc(ev.start)}")
                appendLine("DTEND:${toIcsUtc(ev.end)}")
            }
            ev.description?.takeIf { it.isNotBlank() }?.let {
                appendLine("DESCRIPTION:${escape(it)}")
            }
            ev.location?.takeIf { it.isNotBlank() }?.let {
                appendLine("LOCATION:${escape(it)}")
            }
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }
    return body
}

fun importEventsFromIcs(text: String, calendarId: String = "local:ics"): List<CalendarEvent> {
    if (text.length > 2_000_000) return emptyList()
    val blocks = text.split("BEGIN:VEVENT").drop(1).map { it.substringBefore("END:VEVENT") }
    return blocks.mapIndexedNotNull { index, block ->
        val summary = icsField(block, "SUMMARY") ?: "(No title)"
        val startRaw = icsField(block, "DTSTART") ?: return@mapIndexedNotNull null
        val endRaw = icsField(block, "DTEND") ?: startRaw
        val allDay = !startRaw.contains("T")
        val start = normalizeIcsDate(startRaw, allDay) ?: return@mapIndexedNotNull null
        val end = normalizeIcsDate(endRaw, allDay) ?: start
        val uid = icsField(block, "UID") ?: "ics-$index"
        CalendarEvent(
            id = uid.take(120),
            calendarId = calendarId,
            title = summary,
            description = icsField(block, "DESCRIPTION"),
            location = icsField(block, "LOCATION"),
            start = start,
            end = end,
            allDay = allDay,
            source = CalendarSource.ICS_IMPORT,
            busy = true,
        )
    }
}

private fun icsField(block: String, key: String): String? {
    val re = Regex("""(?im)^$key(?:;[^:]*)?:(.*)$""")
    val raw = re.find(block)?.groupValues?.get(1)?.trim() ?: return null
    return raw.replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")
}

private fun normalizeIcsDate(raw: String, allDay: Boolean): String? {
    val cleaned = raw.trim().removeSuffix("Z")
    if (allDay || cleaned.length == 8) {
        if (cleaned.length < 8) return null
        return "${cleaned.substring(0, 4)}-${cleaned.substring(4, 6)}-${cleaned.substring(6, 8)}"
    }
    if (cleaned.length >= 15 && cleaned[8] == 'T') {
        return "${cleaned.substring(0, 4)}-${cleaned.substring(4, 6)}-${cleaned.substring(6, 8)}" +
            "T${cleaned.substring(9, 11)}:${cleaned.substring(11, 13)}:${cleaned.substring(13, 15)}"
    }
    return cleaned.takeIf { it.contains("-") }
}

private fun toIcsUtc(iso: String): String {
    val digits = iso.filter { it.isDigit() }
    return if (digits.length >= 14) digits.take(14) + "Z" else iso.replace("-", "").replace(":", "") + "Z"
}

private fun escape(s: String): String =
    s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
