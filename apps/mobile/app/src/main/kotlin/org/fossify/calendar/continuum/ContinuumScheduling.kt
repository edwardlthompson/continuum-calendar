package org.fossify.calendar.continuum

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.fossify.calendar.activities.EventActivity
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.helpers.NEW_EVENT_START_TS
import org.fossify.calendar.models.Event
import org.joda.time.DateTime

/** Shared scheduling actions for MainActivity Continuum menu items. */
object ContinuumScheduling {
    fun loadBusyRanges(
        context: Context,
        days: Int,
        callback: (List<Pair<Long, Long>>, ContinuumSettings) -> Unit,
    ) {
        val settings = ContinuumSettingsSync(context).loadLocal()
        val from = DateTime.now().withTimeAtStartOfDay().millis / 1000L
        val to = from + days.coerceAtLeast(1) * 24L * 3600L
        context.eventsHelper.getEvents(from, to) { events ->
            callback(toBusyRanges(events), settings)
        }
    }

    fun toBusyRanges(events: List<Event>): List<Pair<Long, Long>> =
        events
            .filter { !it.getIsAllDay() }
            .map { it.startTS to it.endTS }

    fun copyFreeSlots(context: Context, onDone: (String) -> Unit) {
        loadBusyRanges(context, days = 7) { busy, settings ->
            val (sh, _) = parseHour(settings.workingHours.start)
            val (eh, _) = parseHour(settings.workingHours.end)
            val slots = mutableListOf<FreeSlot>()
            var day = DateTime.now().withTimeAtStartOfDay()
            repeat(7) {
                val dayStart = day.withTime(sh, 0, 0, 0).millis / 1000L
                val dayEnd = day.withTime(eh, 0, 0, 0).millis / 1000L
                slots += FreeSlots.compute(
                    busyRanges = busy,
                    dayStartTs = dayStart,
                    dayEndTs = dayEnd,
                    minMinutes = settings.slotMinMinutes,
                    travelBufferMinutes = settings.travelBufferMinutes,
                )
                day = day.plusDays(1)
            }
            val text = FreeSlots.formatPlainText(slots)
            copyText(context, text)
            onDone(text)
        }
    }

    fun copyProposedTimes(context: Context, onDone: (String) -> Unit) {
        loadBusyRanges(context, days = 7) { busy, settings ->
            val slots = ProposeTimes.proposeMeetingTimes(
                busyRanges = busy,
                durationMinutes = settings.slotMinMinutes,
                count = 5,
                workingHours = settings.workingHours,
                travelBufferMinutes = settings.travelBufferMinutes,
            )
            val text = FreeSlots.formatPlainText(slots, heading = "Proposed times")
            copyText(context, text)
            onDone(text)
        }
    }

    fun jumpToNextFree(context: Context, onEmpty: () -> Unit, onLaunch: (Intent) -> Unit) {
        loadBusyRanges(context, days = 7) { busy, settings ->
            val slots = ProposeTimes.proposeMeetingTimes(
                busyRanges = busy,
                durationMinutes = settings.slotMinMinutes,
                count = 1,
                workingHours = settings.workingHours,
                travelBufferMinutes = settings.travelBufferMinutes,
            )
            val slot = slots.firstOrNull()
            if (slot == null) {
                onEmpty()
                return@loadBusyRanges
            }
            onLaunch(
                Intent(context, EventActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, slot.startTs)
                },
            )
        }
    }

    private fun parseHour(hm: String): Pair<Int, Int> {
        val parts = hm.split(':')
        return (parts.getOrNull(0)?.toIntOrNull() ?: 9).coerceIn(0, 23) to
            (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
    }

    private fun copyText(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("continuum_slots", text))
    }
}
