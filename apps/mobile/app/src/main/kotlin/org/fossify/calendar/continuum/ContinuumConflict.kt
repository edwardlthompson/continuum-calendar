package org.fossify.calendar.continuum

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.calendar.R
import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.models.Event
import org.fossify.calendar.models.ListEvent
import org.joda.time.DateTime

/** Detect overlaps and suggest a work-hours free slot (mirrors packages/shared conflicts). */
object ContinuumConflict {
    /** Warning emoji — avoids missing-glyph issues with the bare ⚠ character on some devices. */
    const val WARNING_EMOJI = "⚠️"

    fun titleWithConflictWarning(title: String, conflict: Boolean): String {
        if (!conflict) return title
        val trimmed = title.trimStart()
        return if (trimmed.startsWith(WARNING_EMOJI)) title else "$WARNING_EMOJI $title"
    }

    /** Treat multi-hour “special day” rows as non-busy even if the all-day flag was lost. */
    private fun isTimedBusyListEvent(event: ListEvent): Boolean {
        if (event.id <= 0L || event.isAllDay) return false
        // >= 20h ≈ all-day / anniversary blocks — never conflict with timed meetings.
        if (event.endTS - event.startTS >= 20L * 60L * 60L) return false
        return true
    }

    private fun isTimedBusyEvent(event: Event): Boolean {
        if (event.getIsAllDay()) return false
        if (event.endTS - event.startTS >= 20L * 60L * 60L) return false
        return true
    }

    /** Ids of timed agenda rows that overlap another timed row (skips Open placeholders). */
    fun conflictingListEventIds(events: List<ListEvent>): Set<Long> {
        val timed = events.filter { isTimedBusyListEvent(it) }.sortedBy { it.startTS }
        if (timed.size < 2) return emptySet()
        val ids = HashSet<Long>()
        for (i in timed.indices) {
            val a = timed[i]
            for (j in i + 1 until timed.size) {
                val b = timed[j]
                if (b.startTS >= a.endTS) break
                if (a.startTS < b.endTS && a.endTS > b.startTS) {
                    ids.add(a.id)
                    ids.add(b.id)
                }
            }
        }
        return ids
    }

    fun findOverlaps(
        events: List<Event>,
        startTS: Long,
        endTS: Long,
        excludeId: Long? = null,
    ): List<Event> {
        if (endTS <= startTS) return emptyList()
        return events.filter { event ->
            if (!isTimedBusyEvent(event)) return@filter false
            val id = event.id
            if (excludeId != null && id != null && id == excludeId) return@filter false
            startTS < event.endTS && endTS > event.startTS
        }
    }

    fun suggestFreeSlot(
        busyRanges: List<Pair<Long, Long>>,
        startTS: Long,
        endTS: Long,
        workingHours: ContinuumWorkingHours,
        travelBufferMinutes: Int = 0,
        days: Int = 14,
    ): FreeSlot? {
        val durationMin = ((endTS - startTS) / 60L).toInt().coerceAtLeast(15)
        val fromTs = minOf(startTS, DateTime.now().millis / 1000L)
        return ProposeTimes.proposeMeetingTimes(
            busyRanges = busyRanges,
            fromTs = fromTs,
            days = days,
            durationMinutes = durationMin,
            count = 1,
            workingHours = workingHours,
            travelBufferMinutes = travelBufferMinutes,
        ).firstOrNull()
    }

    fun formatSlot(slot: FreeSlot): String {
        val start = Formatter.getDateTimeFromTS(slot.startTs)
        val end = Formatter.getDateTimeFromTS(slot.endTs)
        return "${start.toString("EEE MMM d HH:mm")} – ${end.toString("HH:mm")}"
    }

    /**
     * Three-way dialog: use suggested time, save anyway, or cancel.
     * Callbacks run on the UI thread.
     */
    fun showSaveDialog(
        activity: Activity,
        overlaps: List<Event>,
        suggestion: FreeSlot?,
        onUseSuggestion: (FreeSlot) -> Unit,
        onSaveAnyway: () -> Unit,
        onCancel: () -> Unit = {},
    ) {
        val names = overlaps.take(3).joinToString(", ") { it.title.ifBlank { "(No title)" } }
        val more = if (overlaps.size > 3) " and ${overlaps.size - 3} more" else ""
        val message = buildString {
            append(activity.getString(R.string.continuum_conflict_message, names + more))
            append("\n\n")
            if (suggestion != null) {
                append(activity.getString(R.string.continuum_conflict_suggestion, formatSlot(suggestion)))
            } else {
                append(activity.getString(R.string.continuum_conflict_no_suggestion))
            }
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.continuum_conflict_title)
            .setMessage(message)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { d, _ ->
                d.dismiss()
                onCancel()
            }
            .setNeutralButton(R.string.continuum_conflict_save_anyway) { d, _ ->
                d.dismiss()
                onSaveAnyway()
            }
        if (suggestion != null) {
            builder.setPositiveButton(R.string.continuum_conflict_use_suggestion) { d, _ ->
                d.dismiss()
                onUseSuggestion(suggestion)
            }
        }
        builder.setCancelable(true).show()
    }
}
