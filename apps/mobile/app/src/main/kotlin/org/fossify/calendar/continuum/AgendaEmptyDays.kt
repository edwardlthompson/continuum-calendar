package org.fossify.calendar.continuum

import android.content.Context
import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.models.ListEvent
import org.fossify.calendar.models.ListItem
import org.fossify.calendar.models.ListNowMarker
import org.fossify.calendar.models.ListSectionDay
import org.fossify.calendar.models.ListSectionMonth
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * When [showEmptyDays] is true, ensure every day from [rangeStartCode]..[rangeEndCode]
 * appears as a section; empty days get an "Open" placeholder event.
 *
 * Today uses [TodayAgendaLogic] phases: active (events + now bar), open, or empty.
 */
object AgendaEmptyDays {
    fun fill(
        context: Context,
        items: ArrayList<ListItem>,
        showEmptyDays: Boolean,
        rangeStartCode: String,
        rangeEndCode: String,
        todayCode: String,
        nowTs: Long,
        openTitle: String = "Open",
        openColor: Int = 0xFF0F6E8C.toInt(),
        redactTitles: Boolean = false,
        includeMonthSections: Boolean = true,
        workingHoursEnd: String = "17:00",
    ): ArrayList<ListItem> {
        if (!showEmptyDays) {
            val withToday = applyTodayPhase(
                items = if (redactTitles) redact(items) else items,
                todayCode = todayCode,
                nowTs = nowTs,
                openTitle = openTitle,
                openColor = openColor,
                workingHoursEnd = workingHoursEnd,
            )
            return if (includeMonthSections) withToday else stripMonthSections(withToday)
        }

        val eventsByDay = linkedMapOf<String, MutableList<ListItem>>()
        var currentDay: String? = null
        for (item in items) {
            when (item) {
                is ListSectionDay -> {
                    currentDay = item.code
                    eventsByDay.getOrPut(item.code) { mutableListOf() }
                }
                is ListSectionMonth -> Unit
                else -> {
                    val day = currentDay ?: continue
                    eventsByDay.getOrPut(day) { mutableListOf() }.add(item)
                }
            }
        }
        items.filterIsInstance<ListSectionDay>().forEach {
            eventsByDay.getOrPut(it.code) { mutableListOf() }
        }

        val out = ArrayList<ListItem>()
        var cursor = DateTime.parse(rangeStartCode, DateTimeFormat.forPattern("yyyyMMdd"))
        val end = DateTime.parse(rangeEndCode, DateTimeFormat.forPattern("yyyyMMdd"))
        var prevMonth = ""
        while (!cursor.isAfter(end)) {
            val code = Formatter.getDayCodeFromDateTime(cursor)
            val monthLabel = Formatter.getLongMonthYear(context, code)
            if (includeMonthSections && monthLabel != prevMonth) {
                out.add(ListSectionMonth(monthLabel))
                prevMonth = monthLabel
            }
            val isToday = code == todayCode
            val dayStartTs = Formatter.getDayStartTS(code)
            out.add(
                ListSectionDay(
                    title = Formatter.getAgendaSectionTitle(context, code, isToday),
                    code = code,
                    isToday = isToday,
                    isPastSection = !isToday && dayStartTs < nowTs,
                )
            )
            val dayItems = eventsByDay[code].orEmpty()
            if (isToday) {
                appendTodayBody(
                    out = out,
                    dayItems = dayItems,
                    dayCode = code,
                    nowTs = nowTs,
                    openTitle = openTitle,
                    openColor = openColor,
                    dayStartTs = dayStartTs,
                    redactTitles = redactTitles,
                    workingHoursEnd = workingHoursEnd,
                )
            } else {
                appendFutureDayBody(
                    out = out,
                    dayItems = dayItems,
                    dayCode = code,
                    dayStartTs = dayStartTs,
                    openTitle = openTitle,
                    openColor = openColor,
                    redactTitles = redactTitles,
                )
            }
            cursor = cursor.plusDays(1)
        }
        return out
    }

    private fun applyTodayPhase(
        items: ArrayList<ListItem>,
        todayCode: String,
        nowTs: Long,
        openTitle: String,
        openColor: Int,
        workingHoursEnd: String,
    ): ArrayList<ListItem> {
        val out = ArrayList<ListItem>()
        var i = 0
        while (i < items.size) {
            val item = items[i]
            if (item is ListSectionDay && item.code == todayCode) {
                out.add(item)
                i++
                val dayItems = ArrayList<ListItem>()
                while (i < items.size && items[i] !is ListSectionDay && items[i] !is ListSectionMonth) {
                    dayItems.add(items[i])
                    i++
                }
                appendTodayBody(
                    out = out,
                    dayItems = dayItems,
                    dayCode = todayCode,
                    nowTs = nowTs,
                    openTitle = openTitle,
                    openColor = openColor,
                    dayStartTs = Formatter.getDayStartTS(todayCode),
                    redactTitles = false,
                    workingHoursEnd = workingHoursEnd,
                )
            } else {
                out.add(item)
                i++
            }
        }
        return out
    }

    private fun appendFutureDayBody(
        out: ArrayList<ListItem>,
        dayItems: List<ListItem>,
        dayCode: String,
        dayStartTs: Long,
        openTitle: String,
        openColor: Int,
        redactTitles: Boolean,
    ) {
        val events = dayItems.filterIsInstance<ListEvent>().filter { it.id > 0L }
        if (events.isEmpty()) {
            out.add(openEvent(dayStartTs, dayCode, openTitle, openColor, withAllDay = false))
            return
        }
        dayItems.forEach { item ->
            if (item is ListEvent && redactTitles) {
                out.add(item.copy(title = "••••••••"))
            } else {
                out.add(item)
            }
        }
        // All-day / special days do not fill the schedule — still show Open.
        if (events.none { !it.isAllDay }) {
            out.add(openEvent(dayStartTs, dayCode, openTitle, openColor, withAllDay = true))
        }
    }

    private fun appendTodayBody(
        out: ArrayList<ListItem>,
        dayItems: List<ListItem>,
        dayCode: String,
        nowTs: Long,
        openTitle: String,
        openColor: Int,
        dayStartTs: Long,
        redactTitles: Boolean,
        workingHoursEnd: String,
    ) {
        val events = dayItems.filterIsInstance<ListEvent>().filter { it.id > 0 }
        when (TodayAgendaLogic.phase(events, nowTs, dayCode, workingHoursEnd)) {
            TodayAgendaPhase.EMPTY -> Unit
            TodayAgendaPhase.OPEN -> out.add(openEvent(dayStartTs, dayCode, openTitle, openColor, withAllDay = false))
            TodayAgendaPhase.ACTIVE -> {
                val past = events.filter { TodayAgendaLogic.eventEnded(it, nowTs) }
                val future = events.filter { !TodayAgendaLogic.eventEnded(it, nowTs) }
                past.forEach { ev ->
                    out.add(if (redactTitles) ev.copy(title = "••••••••") else ev)
                }
                out.add(ListNowMarker())
                future.forEach { ev ->
                    out.add(if (redactTitles) ev.copy(title = "••••••••") else ev)
                }
                if (events.none { !it.isAllDay }) {
                    out.add(openEvent(dayStartTs, dayCode, openTitle, openColor, withAllDay = true))
                }
            }
        }
    }

    private fun openEvent(
        dayStartTs: Long,
        code: String,
        openTitle: String,
        openColor: Int,
        withAllDay: Boolean,
    ): ListEvent {
        return ListEvent.empty.copy(
            id = -dayStartTs,
            startTS = dayStartTs,
            endTS = Formatter.getDayEndTS(code),
            title = openTitle,
            description = if (withAllDay) "No timed events" else "No events",
            isAllDay = true,
            color = openColor,
        )
    }

    private fun stripMonthSections(items: ArrayList<ListItem>): ArrayList<ListItem> {
        return ArrayList(items.filter { it !is ListSectionMonth })
    }

    private fun redact(items: ArrayList<ListItem>): ArrayList<ListItem> {
        val out = ArrayList<ListItem>(items.size)
        for (item in items) {
            if (item is ListEvent && item.id > 0) out.add(item.copy(title = "••••••••"))
            else out.add(item)
        }
        return out
    }
}
