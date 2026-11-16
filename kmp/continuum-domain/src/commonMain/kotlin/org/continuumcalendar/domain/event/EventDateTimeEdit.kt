package org.continuumcalendar.domain.event

/** Split an ISO start/end into date (YYYY-MM-DD) and time (HH:MM) for editor fields. */
fun isoDatePart(iso: String): String = iso.trim().take(10)

fun isoTimePart(iso: String): String {
    val t = iso.trim()
    val i = t.indexOf('T')
    if (i < 0) return "09:00"
    val clock = t.substring(i + 1).take(5)
    return if (clock.length == 5) clock else "09:00"
}

/** Join date + time into local ISO (or date-only when allDay). */
fun joinDateTime(date: String, time: String, allDay: Boolean): String {
    val d = date.trim().take(10)
    if (allDay) return d
    val hm = time.trim().take(5).ifBlank { "09:00" }
    val parts = hm.split(":")
    val h = parts.getOrNull(0)?.padStart(2, '0') ?: "09"
    val m = parts.getOrNull(1)?.padStart(2, '0') ?: "00"
    return "${d}T$h:$m:00"
}
