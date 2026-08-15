package org.fossify.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.fossify.calendar.R
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.continuum.ContinuumConflict
import org.fossify.calendar.continuum.enableOverflowMarquee
import org.fossify.calendar.databinding.EventListItemBinding
import org.fossify.calendar.databinding.ItemNowMarkerBinding
import org.fossify.calendar.dialogs.DeleteEventDialog
import org.fossify.calendar.extensions.*
import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.helpers.ITEM_EVENT
import org.fossify.calendar.helpers.ITEM_NOW_MARKER
import org.fossify.calendar.helpers.getNowSeconds
import org.fossify.calendar.models.Event
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.helpers.MEDIUM_ALPHA
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView

/**
 * Day list rows: [Event] or a [NOW_MARKER] sentinel (Int) for the red now bar.
 */
class DayEventsAdapter(
    activity: SimpleActivity,
    private val rows: ArrayList<Any>,
    recyclerView: MyRecyclerView,
    var dayCode: String,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    companion object {
        const val NOW_MARKER = 0
    }

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(org.fossify.commons.R.dimen.medium_margin).toInt()
    private val conflictKeys: Set<String> =
        ContinuumConflict.conflictingEventKeys(rows.filterIsInstance<Event>())

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = rows.count { it is Event }

    override fun getIsItemSelectable(position: Int) = rows.getOrNull(position) is Event

    override fun getItemSelectionKey(position: Int) = (rows.getOrNull(position) as? Event)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = rows.indexOfFirst { (it as? Event)?.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemViewType(position: Int) =
        if (rows.getOrNull(position) is Event) ITEM_EVENT else ITEM_NOW_MARKER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = if (viewType == ITEM_NOW_MARKER) {
            ItemNowMarkerBinding.inflate(activity.layoutInflater, parent, false).root
        } else {
            EventListItemBinding.inflate(activity.layoutInflater, parent, false).root
        }
        return createViewHolder(view = root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        if (row is Event) {
            holder.bindView(row, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
                setupView(itemView, row)
            }
        } else {
            holder.bindView(row, allowSingleClick = false, allowLongClick = false) { _, _ -> }
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = rows.size

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(org.fossify.commons.R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }

        notifyDataSetChanged()
    }

    private fun setupView(view: View, event: Event) {
        EventListItemBinding.bind(view).apply {
            eventItemHolder.isSelected = selectedKeys.contains(event.id?.toInt())
            eventItemHolder.background.applyColorFilter(textColor)
            eventItemTitle.text = ContinuumConflict.titleWithConflictWarning(
                event.title,
                conflictKeys.contains(ContinuumConflict.occurrenceKey(event)),
            )
            eventItemTitle.checkViewStrikeThrough(event.shouldStrikeThrough())
            eventItemTitle.enableOverflowMarquee()
            eventItemTime.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(activity, event.startTS)
            if (event.startTS != event.endTS) {
                val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
                val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                val startDate = Formatter.getDayTitle(activity, startDayCode, false)
                val endDate = Formatter.getDayTitle(activity, endDayCode, false)
                val startDayString = if (startDayCode != dayCode) " ($startDate)" else ""
                if (!event.getIsAllDay()) {
                    val endTimeString = Formatter.getTimeFromTS(activity, event.endTS)
                    val endDayString = if (endDayCode != dayCode) " ($endDate)" else ""
                    eventItemTime.text = "${eventItemTime.text}$startDayString - $endTimeString$endDayString"
                } else {
                    val endDayString = if (endDayCode != dayCode) " - ($endDate)" else ""
                    eventItemTime.text = "${eventItemTime.text}$startDayString$endDayString"
                }
            }

            eventItemDescription.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
            eventItemDescription.beVisibleIf(displayDescription && eventItemDescription.text.isNotEmpty())
            eventItemColorBar.background.applyColorFilter(event.color)

            var newTextColor = textColor

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && event.isTaskCompleted()
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }
            if (adjustAlpha) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            } else if (!event.getIsAllDay() && event.startTS <= getNowSeconds() && event.endTS >= getNowSeconds() && !isPrintVersion) {
                newTextColor = properPrimaryColor
            }

            eventItemTime.setTextColor(newTextColor)
            eventItemTitle.setTextColor(newTextColor)
            eventItemDescription.setTextColor(newTextColor)
            eventItemTaskImage.beVisibleIf(event.isTask())
            eventItemTaskImage.applyColorFilter(newTextColor)

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }
            (eventItemTitle.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun shareEvents() = activity.shareEvents(getSelectedEventIds())

    private fun getSelectedEventIds() =
        rows.filterIsInstance<Event>().filter { selectedKeys.contains(it.id?.toInt()) }
            .map { it.id!! }.toMutableList() as ArrayList<Long>

    private fun askConfirmDelete() {
        val eventIds = getSelectedEventIds()
        val eventsToDelete = rows.filterIsInstance<Event>().filter { selectedKeys.contains(it.id?.toInt()) }
        val timestamps = eventsToDelete.map { it.startTS }
        val hasRepeatableEvent = eventsToDelete.any { it.repeatInterval > 0 }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) {
            ensureBackgroundThread {
                val nonRepeatingEventIDs = eventsToDelete.filter { it.repeatInterval == 0 }.map { it.id!! }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)
                val repeatingEventIDs = eventsToDelete.filter { it.repeatInterval > 0 }.map { it.id!! }
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    finishActMode()
                }
            }
        }
    }
}
