package org.continuumcalendar.data.google

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.logicalCalendarId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object GoogleTasksFetch {
    const val TASKS_CALENDAR_ID = "google-tasks"
    const val TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"
    private const val BASE = "https://www.googleapis.com/tasks/v1"
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun hasTasksScope(scope: String): Boolean = scope.contains(TASKS_SCOPE)

    fun tasksCalendarEntry(): CalendarListEntry =
        CalendarListEntry(
            id = TASKS_CALENDAR_ID,
            accountId = "google",
            displayName = "Google Tasks",
            color = "#5F6368",
            visible = true,
            writable = false,
            source = CalendarSource.GOOGLE,
            logicalId = logicalCalendarId(CalendarSource.GOOGLE, TASKS_CALENDAR_ID),
        )

    fun listDueTasks(accessToken: String): List<CalendarEvent> {
        val listsBody = get(accessToken, "$BASE/users/@me/lists?maxResults=100")
        val listIds = Regex(""""id"\s*:\s*"([^"]+)"""").findAll(listsBody).map { it.groupValues[1] }.toList()
        val out = mutableListOf<CalendarEvent>()
        for (listId in listIds.take(20)) {
            val body = runCatching {
                get(
                    accessToken,
                    "$BASE/lists/${java.net.URLEncoder.encode(listId, Charsets.UTF_8)}/tasks?showCompleted=false&maxResults=100",
                )
            }.getOrNull() ?: continue
            out += parseTasks(body, listId)
        }
        return out
    }

    private fun parseTasks(body: String, listId: String): List<CalendarEvent> {
        val items = objectArray(body, "items")
        return items.mapNotNull { raw ->
            val id = stringField(raw, "id")?.trim() ?: return@mapNotNull null
            val due = stringField(raw, "due")?.trim()?.take(10) ?: return@mapNotNull null
            if (!due.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return@mapNotNull null
            CalendarEvent(
                id = "$listId:$id",
                calendarId = TASKS_CALENDAR_ID,
                title = stringField(raw, "title")?.trim()?.ifBlank { null } ?: "Task",
                description = stringField(raw, "notes"),
                start = due,
                end = due,
                allDay = true,
                source = CalendarSource.GOOGLE,
                busy = false,
                readOnly = true,
            )
        }
    }

    private fun get(accessToken: String, url: String): String {
        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() !in 200..299) error("Tasks API HTTP ${res.statusCode()}")
        return res.body()
    }

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return re.find(json)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
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
