package org.fossify.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.fossify.calendar.R
import org.fossify.calendar.activities.MainActivity
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.adapters.DayEventsAdapter
import org.fossify.calendar.continuum.ContinuumSettingsSync
import org.fossify.calendar.continuum.TodayAgendaLogic
import org.fossify.calendar.continuum.TodayAgendaPhase
import org.fossify.calendar.databinding.FragmentDayBinding
import org.fossify.calendar.databinding.TopNavigationBinding
import org.fossify.calendar.extensions.config
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.extensions.getViewBitmap
import org.fossify.calendar.extensions.launchNewEventIntent
import org.fossify.calendar.extensions.printBitmap
import org.fossify.calendar.helpers.*
import org.fossify.calendar.interfaces.NavigationListener
import org.fossify.calendar.models.Event
import org.fossify.commons.extensions.*

class DayFragment : Fragment() {
    var mListener: NavigationListener? = null
    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0
    private var cachedEvents = ArrayList<Event>()

    private lateinit var binding: FragmentDayBinding
    private lateinit var topNavigationBinding: TopNavigationBinding

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isAdded) return
            cachedEvents.forEach { it.updateIsPastEvent() }
            lastHash = 0
            receivedEvents(cachedEvents)
            refreshHandler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDayBinding.inflate(inflater, container, false)
        topNavigationBinding = TopNavigationBinding.bind(binding.root)
        mDayCode = requireArguments().getString(DAY_CODE)!!
        setupButtons()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, 60_000L)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        topNavigationBinding.topLeftArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(org.fossify.commons.R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
            contentDescription = getString(R.string.accessibility_previous_day)
        }

        topNavigationBinding.topRightArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(org.fossify.commons.R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
            contentDescription = getString(R.string.accessibility_next_day)
        }

        val day = Formatter.getDayTitle(requireContext(), mDayCode)
        topNavigationBinding.topValue.apply {
            text = day
            contentDescription = text
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            setTextColor(context.getProperTextColor())
        }
    }

    fun updateCalendar() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        context?.eventsHelper?.getEvents(startTS, endTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val nowBucket = (getNowSeconds() / 60).toInt()
        val newHash = events.hashCode() xor nowBucket
        if (newHash == lastHash || !isAdded) {
            return
        }
        lastHash = newHash
        cachedEvents = ArrayList(events)

        val replaceDescription = requireContext().config.replaceDescription
        val sorted = ArrayList(events.sortedWith(compareBy({ !it.getIsAllDay() }, { it.startTS }, { it.endTS }, { it.title }, {
            if (replaceDescription) it.location else it.description
        })))
        sorted.forEach { it.updateIsPastEvent() }

        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (activity == null) return

        val rows = buildRows(events)
        DayEventsAdapter(activity as SimpleActivity, rows, binding.dayEvents, mDayCode) {
            if (it !is Event) return@DayEventsAdapter
            if ((it.id ?: 0) > 0) {
                editEvent(it)
            } else {
                // Continuum “Open” placeholder (null id) — schedule in this day.
                requireContext().launchNewEventIntent(mDayCode)
            }
        }.apply {
            binding.dayEvents.adapter = this
        }

        if (requireContext().areSystemAnimationsEnabled) {
            binding.dayEvents.scheduleLayoutAnimation()
        }
    }

    private fun buildRows(events: ArrayList<Event>): ArrayList<Any> {
        val rows = ArrayList<Any>()
        val todayCode = Formatter.getTodayCode()
        if (mDayCode != todayCode) {
            rows.addAll(events)
            return rows
        }
        val nowTs = getNowSeconds()
        val workEnd = ContinuumSettingsSync(requireContext()).loadLocal().workingHours.end
        when (TodayAgendaLogic.phaseForEvents(events, nowTs, mDayCode, workEnd)) {
            TodayAgendaPhase.EMPTY -> Unit
            TodayAgendaPhase.OPEN -> {
                val start = Formatter.getDayStartTS(mDayCode)
                rows.add(
                    Event(null, start, Formatter.getDayEndTS(mDayCode)).apply {
                        title = getString(R.string.continuum_open_day)
                        color = 0xFF0F6E8C.toInt()
                        flags = FLAG_ALL_DAY
                    }
                )
            }
            TodayAgendaPhase.ACTIVE -> {
                val past = events.filter { TodayAgendaLogic.eventEnded(it, nowTs) }
                val future = events.filter { !TodayAgendaLogic.eventEnded(it, nowTs) }
                rows.addAll(past)
                rows.add(DayEventsAdapter.NOW_MARKER)
                rows.addAll(future)
            }
        }
        return rows
    }

    private fun editEvent(event: Event) {
        Intent(context, getActivityToOpen(event.isTask())).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
            startActivity(this)
        }
    }

    fun printCurrentView() {
        topNavigationBinding.apply {
            topLeftArrow.beGone()
            topRightArrow.beGone()
            topValue.setTextColor(resources.getColor(org.fossify.commons.R.color.theme_light_text_color))
            (binding.dayEvents.adapter as? DayEventsAdapter)?.togglePrintMode()

            Handler().postDelayed({
                requireContext().printBitmap(binding.dayHolder.getViewBitmap())

                Handler().postDelayed({
                    topLeftArrow.beVisible()
                    topRightArrow.beVisible()
                    topValue.setTextColor(requireContext().getProperTextColor())
                    (binding.dayEvents.adapter as? DayEventsAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }
}
