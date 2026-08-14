package org.fossify.calendar.continuum

import android.content.Context
import android.content.SharedPreferences
import org.fossify.calendar.helpers.LOCAL_CALENDAR_ID
import org.fossify.calendar.helpers.SOURCE_IMPORTED_ICS
import org.fossify.calendar.helpers.SOURCE_SIMPLE_CALENDAR
import org.fossify.calendar.models.Event
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal fun isContinuumOwnedLocal(event: Event): Boolean =
    event.source == SOURCE_SIMPLE_CALENDAR || event.source == SOURCE_IMPORTED_ICS

internal fun peerCalendarId(event: Event): String =
    if (event.calendarId == LOCAL_CALENDAR_ID) "local-default" else event.calendarId.toString()

internal fun peerEventId(event: Event): String {
    val rowId = event.id ?: return event.importId
    return when {
        event.importId.startsWith("local-") -> event.importId
        event.importId.startsWith("continuum:") -> event.importId
        event.importId.isNotBlank() -> event.importId
        else -> "continuum:android:$rowId"
    }
}

/** Persists peer-sync tombstones so Android deletes are not resurrected from Drive. */
class ContinuumLocalEventTombstones(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
    )

    fun recordDeletes(events: List<Event>) {
        val now = Instant.now().toString()
        val tombs = loadMutable()
        var added = 0
        for (ev in events) {
            if (!isContinuumOwnedLocal(ev)) continue
            val id = peerEventId(ev)
            if (id.isBlank()) continue
            val calId = peerCalendarId(ev)
            tombs["$calId:$id"] = JSONObject()
                .put("id", id)
                .put("calendarId", calId)
                .put("deletedAt", now)
            added++
        }
        if (added == 0) return
        persist(tombs)
        ContinuumDiagnostics.i("Recorded $added local-event tombstone(s)")
    }

    fun toJsonArray(): JSONArray {
        val arr = JSONArray()
        for (o in prune(loadMutable()).values) arr.put(o)
        return arr
    }

    fun replaceAll(arr: JSONArray?) {
        val tombs = linkedMapOf<String, JSONObject>()
        ingest(tombs, arr)
        persist(prune(tombs))
    }

    private fun loadMutable(): LinkedHashMap<String, JSONObject> {
        val tombs = linkedMapOf<String, JSONObject>()
        val raw = prefs.getString(KEY_DELETED_IDS, null) ?: return tombs
        try {
            ingest(tombs, JSONArray(raw))
        } catch (e: Exception) {
            ContinuumDiagnostics.w("Failed reading local-event tombstones", e)
        }
        return tombs
    }

    private fun ingest(into: MutableMap<String, JSONObject>, arr: JSONArray?) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id")
            val calId = o.optString("calendarId")
            if (id.isBlank() || calId.isBlank()) continue
            val prev = into["$calId:$id"]
            if (prev == null || o.optString("deletedAt") >= prev.optString("deletedAt")) {
                into["$calId:$id"] = o
            }
        }
    }

    private fun prune(tombs: Map<String, JSONObject>): LinkedHashMap<String, JSONObject> {
        val cutoff = Instant.now().minusMillis(TTL_MS).toString()
        val out = linkedMapOf<String, JSONObject>()
        for ((k, o) in tombs) {
            val at = o.optString("deletedAt")
            if (at.isBlank() || at >= cutoff) out[k] = o
        }
        return out
    }

    private fun persist(tombs: Map<String, JSONObject>) {
        val arr = JSONArray()
        for (o in tombs.values) arr.put(o)
        // commit() so a following peer push on this thread reads the tombs.
        prefs.edit().putString(KEY_DELETED_IDS, arr.toString()).commit()
    }

    companion object {
        const val PREFS = "continuum_local_events"
        private const val KEY_DELETED_IDS = "deleted_ids"
        private const val TTL_MS = 90L * 24 * 60 * 60 * 1000
    }
}
