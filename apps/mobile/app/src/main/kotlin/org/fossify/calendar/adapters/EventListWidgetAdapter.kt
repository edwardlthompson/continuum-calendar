package org.fossify.calendar.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.fossify.calendar.R
import org.fossify.calendar.continuum.ContinuumConflict
import org.fossify.calendar.extensions.*
import org.fossify.calendar.helpers.*
import org.fossify.calendar.models.*
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.MEDIUM_ALPHA
import org.joda.time.DateTime
import kotlin.math.min

class EventListWidgetAdapter(val context: Context, val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val ITEM_EVENT = 0
    private val ITEM_SECTION_DAY = 1
    private val ITEM_SECTION_MONTH = 2
    private val ITEM_NOW_MARKER = 3

    private val allDayString = context.resources.getString(R.string.all_day)
    private var events = ArrayList<ListItem>()
    private var conflictIds: Set<String> = emptySet()
    private var textColor = context.getProperTextColor()
    private var weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
    private var replaceDescription = context.config.replaceDescription
    private var dimPastEvents = context.config.dimPastEvents
    private var dimCompletedTasks = context.config.dimCompletedTasks
    /** In-app list title minus ~2sp — use scaledDensity so RemoteViews SP isn't double-scaled. */
    private var titleFontSize = context.widgetSp(R.dimen.continuum_widget_title_text_size)
    private var timeFontSize = context.widgetSp(R.dimen.continuum_widget_meta_text_size)
    private var smallMargin = context.resources.getDimension(org.fossify.commons.R.dimen.small_margin).toInt()
    private var normalMargin = context.resources.getDimension(org.fossify.commons.R.dimen.normal_margin).toInt()

    init {
        initConfigValues()
    }

    private fun initConfigValues() {
        textColor = context.getProperTextColor()
        weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
        replaceDescription = context.config.replaceDescription
        dimPastEvents = context.config.dimPastEvents
        dimCompletedTasks = context.config.dimCompletedTasks
        titleFontSize = context.widgetSp(R.dimen.continuum_widget_title_text_size)
        timeFontSize = context.widgetSp(R.dimen.continuum_widget_meta_text_size)
    }

    private fun Context.widgetSp(dimenRes: Int): Float =
        resources.getDimension(dimenRes) / resources.displayMetrics.scaledDensity

    override fun getViewAt(position: Int): RemoteViews {
        val type = getItemViewType(position)
        val remoteView: RemoteViews

        if (type == ITEM_EVENT) {
            val event = events[position] as ListEvent
            val layout = R.layout.event_list_item_widget
            remoteView = RemoteViews(context.packageName, layout)
            setupListEvent(remoteView, event)
        } else if (type == ITEM_SECTION_DAY) {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_day_widget)
            val section = events.getOrNull(position) as? ListSectionDay
            if (section != null) {
                setupListSectionDay(remoteView, section)
            }
        } else if (type == ITEM_NOW_MARKER) {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_now_marker_widget)
        } else {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_month_widget)
            val section = events.getOrNull(position) as? ListSectionMonth
            if (section != null) {
                setupListSectionMonth(remoteView, section)
            }
        }

        return remoteView
    }

    private fun setupListEvent(remoteView: RemoteViews, item: ListEvent) {
        var curTextColor = textColor
        remoteView.apply {
            setBackgroundColor(R.id.event_item_color_bar, item.color)
            val conflict = conflictIds.contains(ContinuumConflict.occurrenceKey(item))
            setText(
                R.id.event_item_title,
                ContinuumConflict.titleWithConflictWarning(item.title, conflict),
            )

            var timeText = if (item.isAllDay) allDayString else Formatter.getTimeFromTS(context, item.startTS)
            val endText = Formatter.getTimeFromTS(context, item.endTS)
            if (item.startTS != item.endTS) {
                if (!item.isAllDay) {
                    timeText += " - $endText"
                }

                val startCode = Formatter.getDayCodeFromTS(item.startTS)
                val endCode = Formatter.getDayCodeFromTS(item.endTS)
                if (startCode != endCode) {
                    timeText += " (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            // Continuum widget rows are title + time only (no description / location).
            setText(R.id.event_item_time, timeText)

            if (item.isTask && item.isTaskCompleted && dimCompletedTasks || dimPastEvents && item.isPastEvent && !item.isTask) {
                curTextColor = weakTextColor
            }

            setTextColor(R.id.event_item_title, curTextColor)
            setTextColor(R.id.event_item_time, curTextColor)

            setTextSize(R.id.event_item_title, titleFontSize)
            setTextSize(R.id.event_item_time, timeFontSize)

            setVisibleIf(R.id.event_item_task_image, item.isTask)
            applyColorFilter(R.id.event_item_task_image, curTextColor)

            if (item.isTask) {
                setViewPadding(R.id.event_item_title, 0, 0, smallMargin, 0)
            } else {
                setViewPadding(R.id.event_item_title, normalMargin, 0, smallMargin, 0)
            }

            if (item.shouldStrikeThrough()) {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG)
            } else {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            }

            val openDay = Formatter.getDateDayTitle(Formatter.getDayCodeFromTS(item.startTS))
            setContentDescription(
                R.id.event_item_holder,
                if (item.id <= 0L) {
                    context.getString(R.string.accessibility_open_day, openDay)
                } else {
                    "${item.title}, $timeText"
                },
            )

            Intent().apply {
                putExtra(EVENT_ID, item.id)
                putExtra(EVENT_OCCURRENCE_TS, item.startTS)
                putExtra(IS_TASK, item.isTask)
                setOnClickFillInIntent(R.id.event_item_holder, this)
            }
        }
    }

    private fun setupListSectionDay(remoteView: RemoteViews, item: ListSectionDay) {
        var curTextColor = textColor
        if (dimPastEvents && item.isPastSection) {
            curTextColor = weakTextColor
        }

        remoteView.apply {
            setTextColor(R.id.event_section_title, curTextColor)
            setTextSize(R.id.event_section_title, timeFontSize)
            setText(R.id.event_section_title, item.title)

            Intent().apply {
                putExtra(DAY_CODE, item.code)
                putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
                setOnClickFillInIntent(R.id.event_section_title, this)
            }
        }
    }

    private fun setupListSectionMonth(remoteView: RemoteViews, item: ListSectionMonth) {
        val curTextColor = textColor
        remoteView.apply {
            setTextColor(R.id.event_section_title, curTextColor)
            setTextSize(R.id.event_section_title, timeFontSize)
            setText(R.id.event_section_title, item.title)
        }
    }

    private fun getItemViewType(position: Int) = when (events.getOrNull(position)) {
        is ListEvent -> ITEM_EVENT
        is ListSectionDay -> ITEM_SECTION_DAY
        is ListNowMarker -> ITEM_NOW_MARKER
        else -> ITEM_SECTION_MONTH
    }

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 4

    override fun onCreate() {}

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        initConfigValues()
        try {
            val period = intent.getIntExtra(EVENT_LIST_PERIOD, 0)
            val currentDate = DateTime()
            val dayStart = currentDate.withTimeAtStartOfDay()
            val fromTS = currentDate.seconds() - context.config.displayPastEvents * 60
            val configuredToTS = when (period) {
                0 -> currentDate.plusYears(1).seconds()
                EVENT_PERIOD_TODAY -> currentDate.withTime(23, 59, 59, 999).seconds()
                else -> currentDate.plusSeconds(period).seconds()
            }
            // Empty-day materialization over a full year blows up RemoteViews/ListView and blanks the widget.
            val showEmptyDays = context.config.showEmptyDaysInAgenda
            val toTS = if (showEmptyDays && period != EVENT_PERIOD_TODAY) {
                min(
                    configuredToTS,
                    dayStart.plusDays(WIDGET_EMPTY_DAYS_HORIZON).withTime(23, 59, 59, 999).seconds()
                )
            } else {
                configuredToTS
            }
            context.eventsHelper.getEventsSync(fromTS, toTS, applyTypeFilter = true) {
                val listItems = ArrayList<ListItem>(it.size)
                val replaceDescription = context.config.replaceDescription
                val sorted = it.sortedWith(compareBy<Event> { event ->
                    if (event.getIsAllDay()) {
                        Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) - 1
                    } else {
                        event.startTS
                    }
                }.thenBy { event ->
                    if (event.getIsAllDay()) {
                        Formatter.getDayEndTS(Formatter.getDayCodeFromTS(event.endTS))
                    } else {
                        event.endTS
                    }
                }.thenBy { event -> event.title }.thenBy { event -> if (replaceDescription) event.location else event.description })

                var prevCode = ""
                val now = getNowSeconds()
                val todayCode = Formatter.getDayCodeFromTS(now)

                sorted.forEach { event ->
                    val code = Formatter.getDayCodeFromTS(event.startTS)
                    // No month banners (e.g. "AUGUST") in the Continuum list widget.
                    if (code != prevCode) {
                        val isToday = code == todayCode
                        val day = Formatter.getAgendaSectionTitle(context, code, isToday)
                        val listSection = ListSectionDay(day, code, isToday, !isToday && event.startTS < now)
                        listItems.add(listSection)
                        prevCode = code
                    }

                    val listEvent = ListEvent(
                        id = event.id!!,
                        startTS = event.startTS,
                        endTS = event.endTS,
                        title = event.title,
                        description = event.description,
                        isAllDay = event.getIsAllDay(),
                        color = event.color,
                        location = event.location,
                        isPastEvent = event.isPastEvent,
                        isRepeatable = event.repeatInterval > 0,
                        isTask = event.isTask(),
                        isTaskCompleted = event.isTaskCompleted(),
                        isAttendeeInviteDeclined = event.isAttendeeInviteDeclined(),
                        isEventCanceled = event.isEventCanceled()
                    )
                    listItems.add(listEvent)
                }

                val filled = org.fossify.calendar.continuum.AgendaEmptyDays.fill(
                    context = context,
                    items = listItems,
                    showEmptyDays = showEmptyDays,
                    rangeStartCode = Formatter.getDayCodeFromTS(fromTS.coerceAtLeast(dayStart.seconds())),
                    rangeEndCode = Formatter.getDayCodeFromTS(toTS),
                    todayCode = Formatter.getDayCodeFromTS(getNowSeconds()),
                    nowTs = getNowSeconds(),
                    openTitle = context.getString(R.string.continuum_open_day),
                    redactTitles = context.config.redactTitlesInScreenshots,
                    includeMonthSections = false,
                )
                this@EventListWidgetAdapter.events = filled
                conflictIds = ContinuumConflict.conflictingListEventIds(filled.filterIsInstance<ListEvent>())
                Log.i(TAG, "widget list ready: raw=${it.size} rows=${events.size} emptyDays=$showEmptyDays conflicts=${conflictIds.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "widget onDataSetChanged failed", e)
            events = ArrayList()
        }
    }

    companion object {
        private const val TAG = "ContinuumWidgetList"
        /** Max days to expand when “show empty days” is on (homescreen binder budget). */
        private const val WIDGET_EMPTY_DAYS_HORIZON = 21
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {}
}
