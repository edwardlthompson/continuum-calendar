package org.continuumcalendar.data.google

import org.continuumcalendar.data.settings.continuumSettingsToJson
import org.continuumcalendar.data.settings.parseContinuumSettingsJson
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.ContinuumSettings

internal object PeerEnvelopeJson {
    fun parseRevision(body: String?): Long? {
        if (body.isNullOrBlank()) return null
        return Regex(""""revision"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toLongOrNull()
    }

    fun extractSettings(body: String?): ContinuumSettings? {
        if (body.isNullOrBlank()) return null
        val settingsBlock = objectField(body, "settings") ?: return parseContinuumSettingsJson(body)
        return parseContinuumSettingsJson(settingsBlock)
    }

    fun settingsEnvelopeJson(settings: ContinuumSettings, revision: Long, updatedAt: String): String {
        val settingsJson = continuumSettingsToJson(settings)
        return """{"schemaVersion":1,"revision":$revision,"updatedAt":${jsonString(updatedAt)},"updatedBy":{"platform":"desktop","deviceId":"cmp","appVersion":"0.1.0"},"contentHash":"","settings":$settingsJson}"""
    }

    fun parseOwnedEvents(body: String): List<CalendarEvent> {
        return objectArray(body, "events").mapNotNull { raw ->
            val id = stringField(raw, "id") ?: return@mapNotNull null
            val calendarId = stringField(raw, "calendarId") ?: return@mapNotNull null
            val start = stringField(raw, "start") ?: return@mapNotNull null
            val end = stringField(raw, "end") ?: start
            CalendarEvent(
                id = id,
                calendarId = calendarId,
                title = stringField(raw, "title") ?: "(No title)",
                description = stringField(raw, "description"),
                location = stringField(raw, "location"),
                start = start,
                end = end,
                allDay = boolField(raw, "allDay") ?: false,
                source = CalendarSource.LOCAL,
                updated = stringField(raw, "updated"),
                busy = boolField(raw, "busy") ?: true,
            )
        }
    }

    private fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    private fun boolField(json: String, key: String): Boolean? {
        val re = Regex(""""$key"\s*:\s*(true|false)""")
        return re.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun objectField(json: String, key: String): String? {
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

    private fun objectArray(json: String, key: String): List<String> {
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
}
