package org.fossify.calendar.continuum

import android.content.Context
import android.graphics.Color
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.extensions.config
import org.fossify.calendar.extensions.eventsDB
import org.fossify.calendar.helpers.FLAG_ALL_DAY
import org.fossify.calendar.helpers.LOCAL_CALENDAR_ID
import org.fossify.calendar.helpers.SOURCE_IMPORTED_ICS
import org.fossify.calendar.helpers.SOURCE_SIMPLE_CALENDAR
import org.fossify.calendar.helpers.getNowSeconds
import org.fossify.calendar.models.CalendarEntity
import org.fossify.calendar.models.Event
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Peer-sync Continuum-owned local calendars/events via Drive App Data
 * (`continuum-local-events.json`) — privacy path that does not use Google Calendar.
 *
 * Important: every reconcile path that merges remote data must [applyPayload] into Room
 * so desktop writes become visible on Android (push-pending used to upload-only).
 */
class ContinuumLocalEventsSync(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val auth = ContinuumGoogleAuth(context)
    private val settingsSync = ContinuumSettingsSync(context)

    fun markPending() {
        prefs.edit().putBoolean(KEY_PENDING, true).apply()
    }

    private fun clearPending() {
        prefs.edit().putBoolean(KEY_PENDING, false).apply()
    }

    private fun hasPending(): Boolean = prefs.getBoolean(KEY_PENDING, false)

    private fun lastRevision(): Long = prefs.getLong(KEY_REVISION, 0L)

    /** Mark dirty and push immediately when Continuum Google API is signed in. */
    fun notifyLocalChanged() {
        markPending()
        pushIfSignedIn()
    }

    /**
     * @return true when Room or Drive revision changed enough that the UI should refresh.
     */
    fun reconcilePeerRemote(): Boolean {
        val tokens = auth.ensureFreshTokens() ?: return false
        return try {
            val remote = pullEnvelope(tokens.accessToken)
            val remoteRev = remote?.optLong("revision", 0) ?: 0L
            val localRev = lastRevision()
            val action = when {
                remote == null -> "seed"
                hasPending() || localRev > remoteRev -> "push-pending"
                remoteRev > localRev -> "pull"
                else -> "noop"
            }
            when (action) {
                "seed", "push-pending" -> {
                    val merged = pushSnapshot(tokens.accessToken, remote)
                    // Always materialize the merge — otherwise desktop events never land in Room
                    // while Android has a pending local flag.
                    // Skip new-event heads-up for our own push path.
                    applyPayload(merged, notifyNewFromPeer = false)
                    true
                }
                "pull" -> {
                    applyRemote(remote!!)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            ContinuumDiagnostics.e("Local events reconcile failed", e)
            false
        }
    }

    fun pushIfSignedIn() {
        markPending()
        val tokens = auth.ensureFreshTokens() ?: return
        try {
            val remote = pullEnvelope(tokens.accessToken)
            val merged = pushSnapshot(tokens.accessToken, remote)
            applyPayload(merged, notifyNewFromPeer = false)
        } catch (e: Exception) {
            ContinuumDiagnostics.e("Local events push failed", e)
        }
    }

    /** @return merged payload (calendars/events/deletedIds) that was uploaded */
    private fun pushSnapshot(accessToken: String, remote: JSONObject?): JSONObject {
        val local = buildLocalPayload()
        val merged = mergePayloads(
            JSONObject()
                .put("calendars", remote?.optJSONArray("calendars") ?: JSONArray())
                .put("events", remote?.optJSONArray("events") ?: JSONArray())
                .put("deletedIds", remote?.optJSONArray("deletedIds") ?: JSONArray()),
            local,
        )
        val revision = (remote?.optLong("revision") ?: lastRevision()) + 1
        val envelope = JSONObject()
            .put("schemaVersion", ContinuumConsts.LOCAL_EVENTS_SCHEMA_VERSION)
            .put("revision", revision)
            .put("updatedAt", Instant.now().toString())
            .put(
                "updatedBy",
                JSONObject()
                    .put("platform", "android")
                    .put("deviceId", settingsSync.deviceId())
                    .put("appVersion", "0.1.0"),
            )
            .put("contentHash", sha256(merged.toString()))
            .put("calendars", merged.getJSONArray("calendars"))
            .put("events", merged.getJSONArray("events"))
            .put("deletedIds", merged.optJSONArray("deletedIds") ?: JSONArray())

        val body = envelope.toString()
        if (body.length > MAX_BYTES) {
            throw IllegalStateException("Local events payload too large")
        }

        val meta = findFile(accessToken)
        if (meta == null) {
            createFile(accessToken, body)
        } else {
            updateFile(accessToken, meta.first, body, meta.second)
        }
        prefs.edit().putLong(KEY_REVISION, revision).apply()
        clearPending()
        ContinuumDiagnostics.i(
            "Local events uploaded (revision=$revision, events=${merged.optJSONArray("events")?.length() ?: 0})",
        )
        return merged
    }

    private fun applyRemote(remote: JSONObject) {
        val fromSelf = remote.optJSONObject("updatedBy")?.optString("deviceId") == settingsSync.deviceId()
        applyPayload(
            JSONObject()
                .put("calendars", remote.optJSONArray("calendars") ?: JSONArray())
                .put("events", remote.optJSONArray("events") ?: JSONArray())
                .put("deletedIds", remote.optJSONArray("deletedIds") ?: JSONArray()),
            notifyNewFromPeer = !fromSelf,
        )
        prefs.edit().putLong(KEY_REVISION, remote.optLong("revision", 0)).apply()
        clearPending()
        ContinuumDiagnostics.i(
            "Local events pulled (revision=${remote.optLong("revision", 0)}, events=${remote.optJSONArray("events")?.length() ?: 0})",
        )
    }

    private fun applyPayload(payload: JSONObject, notifyNewFromPeer: Boolean) {
        val deleted = payload.optJSONArray("deletedIds") ?: JSONArray()
        for (i in 0 until deleted.length()) {
            val t = deleted.optJSONObject(i) ?: continue
            deleteByPeerId(t.optString("id"))
        }

        val calendars = payload.optJSONArray("calendars") ?: JSONArray()
        for (i in 0 until calendars.length()) {
            val c = calendars.optJSONObject(i) ?: continue
            upsertLocalCalendar(c)
        }

        // Ensure Continuum default local calendar exists and is visible.
        ensureLocalCalendarVisible(LOCAL_CALENDAR_ID)

        val events = payload.optJSONArray("events") ?: JSONArray()
        var applied = 0
        for (i in 0 until events.length()) {
            val e = events.optJSONObject(i) ?: continue
            val peerId = e.optString("id")
            val existedBefore = try {
                context.eventsDB.getEventWithImportId(peerId) != null ||
                    peerId.removePrefix("continuum:android:").toLongOrNull()
                        ?.let { context.eventsDB.getEventWithId(it) } != null
            } catch (_: Exception) {
                false
            }
            if (upsertLocalEvent(e)) {
                applied++
                if (notifyNewFromPeer && !existedBefore && peerId.isNotBlank()) {
                    val calendarId = when (val cid = e.optString("calendarId")) {
                        "local-default" -> LOCAL_CALENDAR_ID
                        else -> cid.toLongOrNull() ?: LOCAL_CALENDAR_ID
                    }
                    val logical = e.optString("logicalId").ifBlank {
                        ContinuumNotify.logicalIdForCalendarId(
                            calendarId,
                            e.optString("source"),
                        )
                    }
                    ContinuumNotify.notifyNewPeerEvent(
                        context,
                        peerId,
                        calendarId,
                        e.optString("title", "(No title)"),
                        logical,
                    )
                }
            }
        }
        ContinuumDiagnostics.i("Applied $applied peer local events into Room")
    }

    private fun deleteByPeerId(peerId: String) {
        if (peerId.isBlank()) return
        try {
            val byImport = context.eventsDB.getEventWithImportId(peerId)
            if (byImport?.id != null) {
                context.eventsDB.deleteEvents(listOf(byImport.id!!))
                return
            }
            val numeric = peerId.toLongOrNull() ?: return
            context.eventsDB.deleteEvents(listOf(numeric))
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed deleting peer local event $peerId", e)
        }
    }

    private fun buildLocalPayload(): JSONObject {
        val calArr = JSONArray()
        val evArr = JSONArray()
        try {
            context.calendarsDB.getCalendars()
                .filter { it.caldavCalendarId == 0 }
                .forEach { cal ->
                    val id = cal.id ?: return@forEach
                    calArr.put(
                        JSONObject()
                            .put("id", if (id == LOCAL_CALENDAR_ID) "local-default" else id.toString())
                            .put("accountId", "local")
                            .put("displayName", cal.title)
                            .put("color", String.format("#%06X", 0xFFFFFF and cal.color))
                            .put("visible", true)
                            .put("writable", true)
                            .put("source", "local")
                            .put(
                                "logicalId",
                                if (id == LOCAL_CALENDAR_ID) {
                                    ContinuumConsts.logicalId(CalendarSource.LOCAL, "local-default")
                                } else {
                                    ContinuumConsts.logicalId(CalendarSource.LOCAL, id.toString())
                                },
                            ),
                    )
                }
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed listing local calendars", e)
        }

        try {
            context.eventsDB.getAllEvents()
                .filter {
                    it.source == SOURCE_SIMPLE_CALENDAR || it.source == SOURCE_IMPORTED_ICS
                }
                .forEach { ev ->
                    val id = ev.id ?: return@forEach
                    val peerId = when {
                        ev.importId.startsWith("local-") -> ev.importId
                        ev.importId.startsWith("continuum:") -> ev.importId
                        ev.importId.isNotBlank() -> ev.importId
                        else -> "continuum:android:$id"
                    }
                    val allDay = ev.flags and FLAG_ALL_DAY != 0
                    val calId =
                        if (ev.calendarId == LOCAL_CALENDAR_ID) "local-default" else ev.calendarId.toString()
                    evArr.put(
                        JSONObject()
                            .put("id", peerId)
                            .put("calendarId", calId)
                            .put("title", ev.title)
                            .put("description", ev.description)
                            .put("location", ev.location)
                            .put("start", tsToIso(ev.startTS, allDay))
                            .put("end", tsToIso(ev.endTS, allDay))
                            .put("allDay", allDay)
                            .put("source", if (ev.source == SOURCE_IMPORTED_ICS) "ics_import" else "local")
                            .put(
                                "updated",
                                Instant.ofEpochMilli(
                                    ev.lastUpdated.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                ).toString(),
                            ),
                    )
                }
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed listing local events for peer sync", e)
        }

        return JSONObject()
            .put("calendars", calArr)
            .put("events", evArr)
            .put("deletedIds", JSONArray())
    }

    private fun mergePayloads(remote: JSONObject, local: JSONObject): JSONObject {
        val calMap = linkedMapOf<String, JSONObject>()
        fun ingestCals(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val key = o.optString("logicalId").ifBlank { o.optString("id") }
                calMap[key] = o
            }
        }
        ingestCals(remote.optJSONArray("calendars"))
        ingestCals(local.optJSONArray("calendars"))

        val tomb = linkedMapOf<String, JSONObject>()
        fun ingestTomb(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val key = "${o.optString("calendarId")}:${o.optString("id")}"
                val prev = tomb[key]
                if (prev == null || o.optString("deletedAt") >= prev.optString("deletedAt")) {
                    tomb[key] = o
                }
            }
        }
        ingestTomb(remote.optJSONArray("deletedIds"))
        ingestTomb(local.optJSONArray("deletedIds"))

        val evMap = linkedMapOf<String, JSONObject>()
        fun ingestEv(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val key = "${o.optString("calendarId")}:${o.optString("id")}"
                val del = tomb[key]
                if (del != null && del.optString("deletedAt") >= o.optString("updated")) continue
                val prev = evMap[key]
                if (prev == null || o.optString("updated") >= prev.optString("updated")) {
                    evMap[key] = o
                }
            }
        }
        ingestEv(remote.optJSONArray("events"))
        ingestEv(local.optJSONArray("events"))

        return JSONObject()
            .put("calendars", JSONArray(calMap.values.toList()))
            .put("events", JSONArray(evMap.values.toList()))
            .put("deletedIds", JSONArray(tomb.values.toList()))
    }

    private fun ensureLocalCalendarVisible(calendarId: Long) {
        try {
            if (context.calendarsDB.getCalendarWithId(calendarId) == null) {
                context.calendarsDB.insertOrUpdate(
                    CalendarEntity(
                        id = calendarId,
                        title = "Local",
                        color = 0xFF5B6B82.toInt(),
                        caldavCalendarId = 0,
                    ),
                )
            }
            context.config.addDisplayCalendar(calendarId.toString())
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed ensuring local calendar visible", e)
        }
    }

    private fun upsertLocalCalendar(c: JSONObject) {
        val peerId = c.optString("id")
        val id = when {
            peerId == "local-default" -> LOCAL_CALENDAR_ID
            else -> peerId.toLongOrNull()
        } ?: return
        val existing = try {
            context.calendarsDB.getCalendarWithId(id)
        } catch (_: Exception) {
            null
        }
        val color = try {
            Color.parseColor(c.optString("color", "#5b6b82"))
        } catch (_: Exception) {
            0xFF5B6B82.toInt()
        }
        val entity = CalendarEntity(
            id = existing?.id ?: id,
            title = c.optString("displayName", "Local"),
            color = color,
            caldavCalendarId = 0,
            caldavDisplayName = "",
            caldavEmail = "",
        )
        try {
            context.calendarsDB.insertOrUpdate(entity)
            ensureLocalCalendarVisible(entity.id ?: id)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed upserting local calendar $id", e)
        }
    }

    private fun upsertLocalEvent(e: JSONObject): Boolean {
        val peerId = e.optString("id")
        if (peerId.isBlank()) return false
        val calendarId = when (val cid = e.optString("calendarId")) {
            "local-default" -> LOCAL_CALENDAR_ID
            else -> cid.toLongOrNull() ?: LOCAL_CALENDAR_ID
        }
        ensureLocalCalendarVisible(calendarId)
        val allDay = e.optBoolean("allDay", false)
        val startTS = isoToTs(e.optString("start"), allDay)
        val endTS = isoToTs(e.optString("end"), allDay)
        val source = if (e.optString("source") == "ics_import") SOURCE_IMPORTED_ICS else SOURCE_SIMPLE_CALENDAR
        val updated = try {
            Instant.parse(normalizeIso(e.optString("updated"))).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val existing = try {
            context.eventsDB.getEventWithImportId(peerId)
                ?: peerId.removePrefix("continuum:android:").toLongOrNull()
                    ?.let { context.eventsDB.getEventWithId(it) }
        } catch (_: Exception) {
            null
        }
        val event = Event(
            id = existing?.id,
            startTS = startTS,
            endTS = endTS,
            title = e.optString("title", "(No title)"),
            location = e.optString("location", ""),
            description = e.optString("description", ""),
            calendarId = calendarId,
            source = source,
            flags = if (allDay) FLAG_ALL_DAY else 0,
            lastUpdated = updated,
            importId = peerId,
        )
        return try {
            context.eventsDB.insertOrUpdate(event)
            true
        } catch (ex: Exception) {
            ContinuumDiagnostics.w("Failed upserting local event $peerId", ex)
            false
        }
    }

    private fun tsToIso(ts: Long, allDay: Boolean): String {
        val millis = ts * 1000L
        return if (allDay) {
            Instant.ofEpochMilli(millis).toString().substring(0, 10)
        } else {
            Instant.ofEpochMilli(millis).toString()
        }
    }

    private fun normalizeIso(iso: String): String {
        if (iso.isBlank()) return iso
        if (iso.endsWith("Z") || iso.contains("+") || iso.matches(Regex(".+T.+-\\d{2}:\\d{2}$"))) {
            return iso
        }
        return when {
            iso.length == 16 -> "${iso}:00Z" // yyyy-MM-ddTHH:mm
            iso.length == 19 -> "${iso}Z" // yyyy-MM-ddTHH:mm:ss
            else -> iso
        }
    }

    private fun isoToTs(iso: String, allDay: Boolean): Long {
        return try {
            if (allDay || iso.length == 10) {
                Instant.parse("${iso.take(10)}T12:00:00Z").epochSecond
            } else {
                Instant.parse(normalizeIso(iso)).epochSecond
            }
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(iso.take(19)).toEpochSecond(ZoneOffset.UTC)
            } catch (_: Exception) {
                getNowSeconds()
            }
        }
    }

    private fun pullEnvelope(accessToken: String): JSONObject? {
        val meta = findFile(accessToken) ?: return null
        val conn = URL("https://www.googleapis.com/drive/v3/files/${meta.first}?alt=media")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        if (conn.responseCode !in 200..299) {
            ContinuumDiagnostics.w("Drive local-events download failed: ${conn.responseCode}")
            return null
        }
        return JSONObject(conn.inputStream.bufferedReader().readText())
    }

    private fun findFile(accessToken: String): Pair<String, String>? {
        val q = java.net.URLEncoder.encode(
            "name = '${ContinuumConsts.LOCAL_EVENTS_APP_DATA_FILENAME}' and trashed = false",
            "UTF-8",
        )
        val url =
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id)&pageSize=10"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        if (conn.responseCode !in 200..299) {
            ContinuumDiagnostics.w("Drive local-events list failed: ${conn.responseCode}")
            return null
        }
        val files = JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("files")
            ?: return null
        if (files.length() == 0) return null
        val f = files.getJSONObject(0)
        return f.getString("id") to ""
    }

    private fun createFile(accessToken: String, body: String) {
        val boundary = "continuum"
        val meta = JSONObject()
            .put("name", ContinuumConsts.LOCAL_EVENTS_APP_DATA_FILENAME)
            .put("parents", JSONArray().put("appDataFolder"))
        val payload = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta.toString())
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(body)
            append("\r\n--$boundary--")
        }
        val conn = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&spaces=appDataFolder")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.outputStream.use { it.write(payload.toByteArray()) }
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Drive local-events create failed: ${conn.responseCode}")
        }
        conn.inputStream.bufferedReader().readText()
    }

    private fun updateFile(accessToken: String, fileId: String, body: String, etag: String) {
        val conn = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "PATCH"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "application/json")
        if (etag.isNotBlank()) conn.setRequestProperty("If-Match", etag)
        conn.outputStream.use { it.write(body.toByteArray()) }
        if (conn.responseCode == 412) {
            ContinuumDiagnostics.w("Local events CAS conflict — will retry on next poll")
            markPending()
            return
        }
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Drive local-events update failed: ${conn.responseCode}")
        }
        conn.inputStream.bufferedReader().readText()
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS = "continuum_local_events"
        private const val KEY_REVISION = "revision"
        private const val KEY_PENDING = "pending_peer_push"
        private const val MAX_BYTES = 2_000_000
    }
}
