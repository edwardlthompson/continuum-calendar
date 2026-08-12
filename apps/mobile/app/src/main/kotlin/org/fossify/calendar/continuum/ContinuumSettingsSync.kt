package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.helpers.Config
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

/** Revisioned Continuum settings via Drive App Data (CAS + poll). */
class ContinuumSettingsSync(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val auth = ContinuumGoogleAuth(context)

    fun deviceId(): String {
        var id = prefs.getString(KEY_DEVICE, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE, id).apply()
        }
        return id
    }

    fun loadLocal(): ContinuumSettings {
        val raw = prefs.getString(KEY_SETTINGS_JSON, null)
        if (raw == null) {
            // Seed peer fields from Fossify prefs so first Continuum push carries 24h / week start.
            val cfg = Config.newInstance(context)
            return ContinuumSettings(
                use24HourFormat = cfg.use24HourFormat,
                firstDayOfWeek = ContinuumConsts.jodaToJsFirstDay(cfg.firstDayOfWeek),
                showEmptyDaysInAgenda = cfg.showEmptyDaysInAgenda,
                rollingWeekFromToday = cfg.startWeekWithCurrentDay,
                redactTitlesInScreenshots = cfg.redactTitlesInScreenshots,
                showContactBirthdays = cfg.showContactBirthdays,
                weeklyViewDays = cfg.weeklyViewDays.coerceIn(1, 14),
                defaultSnoozeMinutes = cfg.snoozeTime.coerceAtLeast(1),
            )
        }
        return parseSettings(JSONObject(raw))
    }

    fun saveLocal(settings: ContinuumSettings, revision: Long, etag: String?) {
        prefs.edit()
            .putString(KEY_SETTINGS_JSON, settingsToJson(settings).toString())
            .putLong(KEY_REVISION, revision)
            .putString(KEY_ETAG, etag)
            .apply()
        applyToConfig(settings)
    }

    fun lastRevision(): Long = prefs.getLong(KEY_REVISION, 0L)

    fun applyToConfig(settings: ContinuumSettings) {
        val cfg = Config.newInstance(context)
        cfg.showEmptyDaysInAgenda = settings.showEmptyDaysInAgenda
        cfg.startWeekWithCurrentDay = settings.rollingWeekFromToday
        cfg.redactTitlesInScreenshots = settings.redactTitlesInScreenshots
        cfg.showContactBirthdays = settings.showContactBirthdays
        cfg.use24HourFormat = settings.use24HourFormat
        cfg.firstDayOfWeek = ContinuumConsts.jsToJodaFirstDay(settings.firstDayOfWeek.coerceIn(0, 6))
        cfg.weeklyViewDays = settings.weeklyViewDays.coerceIn(1, 14)
        cfg.snoozeTime = settings.defaultSnoozeMinutes.coerceAtLeast(1)
        applyThemeMode(settings.themeMode)
        if (!settings.useGoogleCalendar && settings.defaultWriteCalendarId.startsWith("google:")) {
            scheduleDefaultWriteCalendar("local:${org.fossify.calendar.helpers.LOCAL_CALENDAR_ID}")
        } else {
            scheduleDefaultWriteCalendar(settings.defaultWriteCalendarId)
        }
        // Birthday / Google calendar display lists need Room — never touch DB on caller thread.
        scheduleBirthdayCalendarVisibility(settings.showContactBirthdays)
        scheduleGoogleCalendarVisibility(settings.useGoogleCalendar)
        // travelBuffer / workingHours / slotMin / agendaRange / notificationEnabled
        // live in Continuum prefs JSON and are read via loadLocal().
    }

    private fun scheduleGoogleCalendarVisibility(useGoogle: Boolean) {
        org.fossify.commons.helpers.ensureBackgroundThread {
            try {
                ContinuumPrivacy.applyGoogleCalendarVisibility(context, useGoogle)
            } catch (e: Exception) {
                ContinuumDiagnostics.e("Failed updating Google calendar visibility", e)
            }
        }
    }

    private fun applyThemeMode(themeMode: String) {
        val mode = when (themeMode) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        try {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed applying themeMode=$themeMode", e)
        }
    }

    private fun scheduleDefaultWriteCalendar(logicalId: String) {
        org.fossify.commons.helpers.ensureBackgroundThread {
            try {
                DefaultWriteCalendar.applyLogicalIdToConfig(context, logicalId)
            } catch (e: Exception) {
                ContinuumDiagnostics.e("Failed applying default write calendar", e)
            }
        }
    }

    private fun scheduleBirthdayCalendarVisibility(show: Boolean) {
        org.fossify.commons.helpers.ensureBackgroundThread {
            try {
                ContinuumBirthdayFilter.applyDisplayCalendarVisibility(context, show)
            } catch (e: Exception) {
                ContinuumDiagnostics.e("Failed updating birthday calendar visibility", e)
            }
        }
    }

    fun hasDriveAuth(): Boolean = auth.isSignedIn()

    fun hasPendingPeerPush(): Boolean = prefs.getBoolean(KEY_PENDING_PEER, false)

    private fun markPendingPeerPush() {
        prefs.edit().putBoolean(KEY_PENDING_PEER, true).apply()
    }

    private fun clearPendingPeerPush() {
        prefs.edit().putBoolean(KEY_PENDING_PEER, false).apply()
    }

    fun pullAndApply(): ContinuumSettings? {
        val tokens = auth.ensureFreshTokens() ?: return null
        val meta = findFile(tokens.accessToken) ?: return loadLocal()
        val conn = URL("https://www.googleapis.com/drive/v3/files/${meta.first}?alt=media")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${tokens.accessToken}")
        val text = conn.inputStream.bufferedReader().readText()
        val envelope = JSONObject(text)
        val revision = envelope.optLong("revision", 0)
        if (revision <= lastRevision()) return loadLocal()
        val settings = parseSettings(envelope.getJSONObject("settings"))
        val etag = conn.getHeaderField("ETag") ?: meta.second
        saveLocal(settings, revision, etag)
        return settings
    }

    /**
     * Peer reconcile with desktop via Drive App Data.
     * Seeds remote if missing, pushes pending local edits, otherwise pulls newer revision.
     */
    fun reconcilePeerRemote(): ContinuumSettings {
        val tokens = auth.ensureFreshTokens() ?: return loadLocal()
        val remote = pullEnvelope(tokens.accessToken)
        val remoteRev = remote?.optLong("revision", 0) ?: 0L
        val localRev = lastRevision()
        val pending = hasPendingPeerPush()

        // Mirror packages/shared decidePeerReconcile
        val action = when {
            remote == null -> "seed"
            pending || localRev > remoteRev -> "push-pending"
            remoteRev > localRev -> "pull"
            else -> "noop"
        }

        return when (action) {
            "seed", "push-pending" -> {
                val result = pushPatch(loadLocal())
                ContinuumDiagnostics.i("Peer settings reconcile action=$action uploaded=${result.uploadedToDrive}")
                result.settings
            }
            "pull" -> {
                pullAndApply() ?: loadLocal()
            }
            else -> {
                // Refresh etag / ensure config matches remote hash without lowering revision.
                if (remote != null) {
                    val settings = parseSettings(remote.getJSONObject("settings"))
                    applyToConfig(settings)
                    settings
                } else {
                    loadLocal()
                }
            }
        }
    }

    /** After Continuum Google API sign-in — join the peer remote (seed/push/pull). */
    fun pushLocalAfterAuth(): ContinuumSettings = reconcilePeerRemote()

    /**
     * Merge [patch] onto the shared Drive peer remote (or mark pending when not signed in).
     * Desktop and Android are peers: either may write; the other polls and applies.
     */
    fun pushPatch(patch: ContinuumSettings): SettingsPushResult {
        val tokens = auth.ensureFreshTokens() ?: run {
            saveLocal(patch, lastRevision() + 1, null)
            markPendingPeerPush()
            ContinuumDiagnostics.w(
                "Continuum settings pending peer push — sign in to Continuum Google API to sync with desktop",
            )
            return SettingsPushResult(settings = patch, uploadedToDrive = false)
        }
        val remote = pullEnvelope(tokens.accessToken)
        val baseSettings = remote?.let { parseSettings(it.getJSONObject("settings")) } ?: loadLocal()
        val merged = baseSettings.copy(
            themeMode = patch.themeMode,
            showEmptyDaysInAgenda = patch.showEmptyDaysInAgenda,
            rollingWeekFromToday = patch.rollingWeekFromToday,
            weeklyViewDays = patch.weeklyViewDays.coerceIn(1, 14),
            workingHours = patch.workingHours,
            defaultReminderMinutes = patch.defaultReminderMinutes.coerceAtLeast(0),
            defaultSnoozeMinutes = patch.defaultSnoozeMinutes.coerceAtLeast(0),
            redactTitlesInScreenshots = patch.redactTitlesInScreenshots,
            notificationEnabled = patch.notificationEnabled,
            travelBufferMinutes = patch.travelBufferMinutes.coerceAtLeast(0),
            slotMinMinutes = patch.slotMinMinutes.coerceAtLeast(1),
            agendaRangeDays = patch.agendaRangeDays.coerceIn(7, 90),
            agendaDensity = patch.agendaDensity,
            showContactBirthdays = patch.showContactBirthdays,
            use24HourFormat = patch.use24HourFormat,
            firstDayOfWeek = patch.firstDayOfWeek.coerceIn(0, 6),
            useGoogleCalendar = patch.useGoogleCalendar,
            defaultWriteCalendarId = patch.defaultWriteCalendarId.ifBlank { baseSettings.defaultWriteCalendarId },
            visibleCalendarIds = if (patch.visibleCalendarIds.isNotEmpty()) {
                patch.visibleCalendarIds
            } else {
                baseSettings.visibleCalendarIds
            },
            secondaryTimeZone = patch.secondaryTimeZone ?: baseSettings.secondaryTimeZone,
            calendarNotifyPrefs = if (patch.calendarNotifyPrefs.isNotEmpty()) {
                patch.calendarNotifyPrefs
            } else {
                baseSettings.calendarNotifyPrefs
            },
        )
        val revision = (remote?.optLong("revision") ?: lastRevision()) + 1
        val hash = sha256(settingsToJson(merged).toString())
        val envelope = JSONObject()
            .put("schemaVersion", ContinuumConsts.SCHEMA_VERSION)
            .put("revision", revision)
            .put("updatedAt", java.time.Instant.now().toString())
            .put(
                "updatedBy",
                JSONObject()
                    .put("platform", "android")
                    .put("deviceId", deviceId())
                    .put("appVersion", "0.1.0"),
            )
            .put("contentHash", hash)
            .put("settings", settingsToJson(merged))

        val meta = findFile(tokens.accessToken)
        if (meta == null) {
            createFile(tokens.accessToken, envelope.toString())
        } else {
            updateFile(tokens.accessToken, meta.first, envelope.toString(), meta.second)
        }
        saveLocal(merged, revision, null)
        clearPendingPeerPush()
        ContinuumDiagnostics.i("Continuum settings uploaded to peer remote (revision=$revision)")
        return SettingsPushResult(settings = merged, uploadedToDrive = true)
    }

    private fun pullEnvelope(accessToken: String): JSONObject? {
        val meta = findFile(accessToken) ?: return null
        val conn = URL("https://www.googleapis.com/drive/v3/files/${meta.first}?alt=media")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        return JSONObject(conn.inputStream.bufferedReader().readText())
    }

    private fun findFile(accessToken: String): Pair<String, String>? {
        val q = java.net.URLEncoder.encode(
            "name = '${ContinuumConsts.APP_DATA_FILENAME}' and trashed = false",
            "UTF-8",
        )
        val url =
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id)&pageSize=10"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            ContinuumDiagnostics.w("Drive list failed: $code $err")
            return null
        }
        val files = JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("files")
            ?: return null
        if (files.length() == 0) return null
        val f = files.getJSONObject(0)
        // ETag comes from media download / update responses, not list fields.
        return f.getString("id") to ""
    }

    private fun createFile(accessToken: String, body: String) {
        val boundary = "continuum"
        val meta = JSONObject()
            .put("name", ContinuumConsts.APP_DATA_FILENAME)
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
            pullAndApply()
            pushPatch(loadLocal())
            return
        }
        conn.inputStream.bufferedReader().readText()
    }

    private fun parseSettings(json: JSONObject): ContinuumSettings {
        val hours = json.optJSONObject("workingHours")
        return ContinuumSettings(
            themeMode = json.optString("themeMode", "system"),
            showEmptyDaysInAgenda = json.optBoolean("showEmptyDaysInAgenda", true),
            rollingWeekFromToday = json.optBoolean("rollingWeekFromToday", true),
            weeklyViewDays = json.optInt("weeklyViewDays", 7),
            workingHours = ContinuumWorkingHours(
                start = hours?.optString("start", "09:00") ?: "09:00",
                end = hours?.optString("end", "17:00") ?: "17:00",
            ),
            defaultReminderMinutes = json.optInt("defaultReminderMinutes", 10),
            notificationEnabled = json.optBoolean("notificationEnabled", true),
            defaultSnoozeMinutes = json.optInt("defaultSnoozeMinutes", 10),
            redactTitlesInScreenshots = json.optBoolean("redactTitlesInScreenshots", false),
            travelBufferMinutes = json.optInt("travelBufferMinutes", 0).coerceAtLeast(0),
            slotMinMinutes = json.optInt("slotMinMinutes", 30).coerceAtLeast(1),
            agendaRangeDays = json.optInt("agendaRangeDays", 30).coerceIn(7, 90),
            agendaDensity = json.optString("agendaDensity", "comfortable"),
            showContactBirthdays = json.optBoolean("showContactBirthdays", true),
            use24HourFormat = json.optBoolean("use24HourFormat", false),
            firstDayOfWeek = json.optInt("firstDayOfWeek", 0).coerceIn(0, 6),
            useGoogleCalendar = json.optBoolean("useGoogleCalendar", true),
            secondaryTimeZone = if (json.has("secondaryTimeZone") && !json.isNull("secondaryTimeZone")) {
                json.optString("secondaryTimeZone").takeIf { it.isNotBlank() }
            } else {
                null
            },
            defaultWriteCalendarId = json.optString("defaultWriteCalendarId", "google:primary"),
            visibleCalendarIds = json.optJSONArray("visibleCalendarIds")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) add(arr.optString(i))
                }
            } ?: listOf("google:primary"),
            calendarNotifyPrefs = parseNotifyPrefs(json.optJSONObject("calendarNotifyPrefs")),
        )
    }

    private fun parseNotifyPrefs(obj: JSONObject?): Map<String, CalendarNotifyPrefs> {
        if (obj == null) return emptyMap()
        val out = linkedMapOf<String, CalendarNotifyPrefs>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.isBlank()) continue
            val entry = obj.optJSONObject(key)
            val isHoliday = key.startsWith("holidays:")
            out[key] = CalendarNotifyPrefs(
                newEvent = entry?.optBoolean("newEvent", !isHoliday) ?: !isHoliday,
                reminder = entry?.optBoolean("reminder", true) ?: true,
            )
        }
        return out
    }

    private fun settingsToJson(s: ContinuumSettings): JSONObject =
        JSONObject()
            .put("themeMode", s.themeMode)
            .put("showEmptyDaysInAgenda", s.showEmptyDaysInAgenda)
            .put("rollingWeekFromToday", s.rollingWeekFromToday)
            .put("weeklyViewDays", s.weeklyViewDays)
            .put(
                "workingHours",
                JSONObject().put("start", s.workingHours.start).put("end", s.workingHours.end),
            )
            .put("defaultReminderMinutes", s.defaultReminderMinutes)
            .put("notificationEnabled", s.notificationEnabled)
            .put("defaultSnoozeMinutes", s.defaultSnoozeMinutes)
            .put("redactTitlesInScreenshots", s.redactTitlesInScreenshots)
            .put("travelBufferMinutes", s.travelBufferMinutes)
            .put("slotMinMinutes", s.slotMinMinutes)
            .put("agendaRangeDays", s.agendaRangeDays)
            .put("agendaDensity", s.agendaDensity)
            .put("showContactBirthdays", s.showContactBirthdays)
            .put("use24HourFormat", s.use24HourFormat)
            .put("firstDayOfWeek", s.firstDayOfWeek)
            .put("useGoogleCalendar", s.useGoogleCalendar)
            .put("defaultWriteCalendarId", s.defaultWriteCalendarId)
            .put("visibleCalendarIds", JSONArray(s.visibleCalendarIds))
            .put(
                "calendarNotifyPrefs",
                JSONObject().also { map ->
                    s.calendarNotifyPrefs.forEach { (k, v) ->
                        map.put(
                            k,
                            JSONObject().put("newEvent", v.newEvent).put("reminder", v.reminder),
                        )
                    }
                },
            )
            .also { json ->
                if (!s.secondaryTimeZone.isNullOrBlank()) {
                    json.put("secondaryTimeZone", s.secondaryTimeZone)
                }
            }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS = "continuum_settings"
        private const val KEY_SETTINGS_JSON = "settings_json"
        private const val KEY_REVISION = "revision"
        private const val KEY_ETAG = "etag"
        private const val KEY_DEVICE = "device_id"
        private const val KEY_PENDING_PEER = "pending_peer_push"
    }
}

data class SettingsPushResult(
    val settings: ContinuumSettings,
    val uploadedToDrive: Boolean,
)
