package org.fossify.calendar.activities

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.fossify.calendar.R
import org.fossify.calendar.databinding.ActivitySettingsBinding
import org.fossify.calendar.dialogs.ExportEventsDialog
import org.fossify.calendar.dialogs.ManageAutomaticBackupsDialog
import org.fossify.calendar.dialogs.ManageSyncedCalendarsDialog
import org.fossify.calendar.dialogs.SelectCalendarDialog
import org.fossify.calendar.dialogs.SelectCalendarsDialog
import org.fossify.calendar.continuum.ContinuumAuthActivity
import org.fossify.calendar.continuum.ContinuumGoogleAuth
import org.fossify.calendar.continuum.ContinuumLocalEventsSync
import org.fossify.calendar.continuum.ContinuumSettingsOAuth
import org.fossify.calendar.continuum.ContinuumSettingsSync
import org.fossify.calendar.extensions.calDAVHelper
import org.fossify.calendar.extensions.calendarsDB
import org.fossify.calendar.extensions.cancelScheduledAutomaticBackup
import org.fossify.calendar.extensions.config
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.extensions.getSyncedCalDAVCalendars
import org.fossify.calendar.extensions.scheduleNextAutomaticBackup
import org.fossify.calendar.extensions.showImportEventsDialog
import org.fossify.calendar.extensions.tryImportEventsFromFile
import org.fossify.calendar.extensions.updateWidgets
import org.fossify.calendar.helpers.ALLOW_CHANGING_TIME_ZONES
import org.fossify.calendar.helpers.ALLOW_CREATING_TASKS
import org.fossify.calendar.helpers.ALLOW_CUSTOMIZE_DAY_COUNT
import org.fossify.calendar.helpers.DAILY_VIEW
import org.fossify.calendar.helpers.DEFAULT_DURATION
import org.fossify.calendar.helpers.DEFAULT_REMINDER_1
import org.fossify.calendar.helpers.DEFAULT_REMINDER_2
import org.fossify.calendar.helpers.DEFAULT_REMINDER_3
import org.fossify.calendar.helpers.DEFAULT_START_TIME
import org.fossify.calendar.helpers.DEFAULT_START_TIME_CURRENT_TIME
import org.fossify.calendar.helpers.DEFAULT_START_TIME_NEXT_FULL_HOUR
import org.fossify.calendar.helpers.DIM_COMPLETED_TASKS
import org.fossify.calendar.helpers.DIM_PAST_EVENTS
import org.fossify.calendar.helpers.DISPLAY_DESCRIPTION
import org.fossify.calendar.helpers.DISPLAY_PAST_EVENTS
import org.fossify.calendar.helpers.EVENTS_LIST_VIEW
import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.helpers.HIGHLIGHT_WEEKENDS
import org.fossify.calendar.helpers.HIGHLIGHT_WEEKENDS_COLOR
import org.fossify.calendar.helpers.IcsExporter
import org.fossify.calendar.helpers.LAST_EVENT_REMINDER_MINUTES
import org.fossify.calendar.helpers.LAST_EVENT_REMINDER_MINUTES_2
import org.fossify.calendar.helpers.LAST_EVENT_REMINDER_MINUTES_3
import org.fossify.calendar.helpers.LAST_VIEW
import org.fossify.calendar.helpers.LIST_WIDGET_VIEW_TO_OPEN
import org.fossify.calendar.helpers.LOOP_REMINDERS
import org.fossify.calendar.helpers.MONTHLY_DAILY_VIEW
import org.fossify.calendar.helpers.MONTHLY_VIEW
import org.fossify.calendar.helpers.PULL_TO_REFRESH
import org.fossify.calendar.helpers.REMINDER_AUDIO_STREAM
import org.fossify.calendar.helpers.REMINDER_OFF
import org.fossify.calendar.helpers.REPLACE_DESCRIPTION
import org.fossify.calendar.helpers.SHOW_GRID
import org.fossify.calendar.helpers.SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP
import org.fossify.calendar.helpers.START_WEEKLY_AT
import org.fossify.calendar.helpers.START_WEEK_WITH_CURRENT_DAY
import org.fossify.calendar.helpers.USE_PREVIOUS_EVENT_REMINDERS
import org.fossify.calendar.helpers.VIBRATE
import org.fossify.calendar.helpers.WEEKLY_VIEW
import org.fossify.calendar.helpers.WEEK_NUMBERS
import org.fossify.calendar.helpers.YEARLY_VIEW
import org.fossify.calendar.models.CalendarEntity
import org.fossify.commons.dialogs.ColorPickerDialog
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.CustomIntervalPickerDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.dialogs.SelectAlarmSoundDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.checkAppIconColor
import org.fossify.commons.extensions.formatMinutesToTimeString
import org.fossify.commons.extensions.getAppIconColors
import org.fossify.commons.extensions.getDayOfWeekString
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getFileOutputStream
import org.fossify.commons.extensions.getFontSizeText
import org.fossify.commons.extensions.getFormattedMinutes
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getTimePickerDialogTheme
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.extensions.setFillWithStroke
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.showPickSecondsDialogHelper
import org.fossify.commons.extensions.storeNewYourAlarmSound
import org.fossify.commons.extensions.toBoolean
import org.fossify.commons.extensions.toFileDirItem
import org.fossify.commons.extensions.toInt
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ACCENT_COLOR
import org.fossify.commons.helpers.APP_ICON_COLOR
import org.fossify.commons.helpers.BACKGROUND_COLOR
import org.fossify.commons.helpers.FIRST_DAY_OF_WEEK
import org.fossify.commons.helpers.FONT_SIZE
import org.fossify.commons.helpers.FONT_SIZE_EXTRA_LARGE
import org.fossify.commons.helpers.FONT_SIZE_LARGE
import org.fossify.commons.helpers.FONT_SIZE_MEDIUM
import org.fossify.commons.helpers.FONT_SIZE_SMALL
import org.fossify.commons.helpers.IS_CUSTOMIZING_COLORS
import org.fossify.commons.helpers.IS_GLOBAL_THEME_ENABLED
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.PERMISSION_READ_CALENDAR
import org.fossify.commons.helpers.PERMISSION_READ_STORAGE
import org.fossify.commons.helpers.PERMISSION_WRITE_CALENDAR
import org.fossify.commons.helpers.PERMISSION_WRITE_STORAGE
import org.fossify.commons.helpers.PRIMARY_COLOR
import org.fossify.commons.helpers.SNOOZE_TIME
import org.fossify.commons.helpers.SUNDAY_FIRST
import org.fossify.commons.helpers.TEXT_COLOR
import org.fossify.commons.helpers.USE_24_HOUR_FORMAT
import org.fossify.commons.helpers.USE_ENGLISH
import org.fossify.commons.helpers.USE_SAME_SNOOZE
import org.fossify.commons.helpers.WAS_USE_ENGLISH_TOGGLED
import org.fossify.commons.helpers.WIDGET_BG_COLOR
import org.fossify.commons.helpers.WIDGET_TEXT_COLOR
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isOreoPlus
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.AlarmSound
import org.fossify.commons.models.RadioItem
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    companion object {
        private const val GET_RINGTONE_URI = 1
        private const val PICK_SETTINGS_IMPORT_SOURCE_INTENT = 2
        private const val PICK_EVENTS_IMPORT_SOURCE_INTENT = 3
        private const val PICK_EVENTS_EXPORT_FILE_INTENT = 4
    }

    private var mStoredPrimaryColor = 0

    private var calendarsToExport = listOf<Long>()

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mStoredPrimaryColor = getProperPrimaryColor()

        setupEdgeToEdge(padBottomSystem = listOf(binding.settingsNestedScrollview))
        setupMaterialScrollListener(
            scrollingView = binding.settingsNestedScrollview,
            topAppBar = binding.settingsAppbar
        )
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(topAppBar = binding.settingsAppbar, navigationIcon = NavigationIcon.Arrow)
        setupSettingItems()
        setupSettingsSearch()
    }

    private fun setupSettingsSearch() {
        binding.settingsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterSettings(s?.toString().orEmpty())
            }
        })
        filterSettings(binding.settingsSearch.text?.toString().orEmpty())
    }

    private fun filterSettings(query: String) {
        val q = query.trim().lowercase()
        val holder = binding.settingsHolder
        val sectionLabels = ArrayList<TextView>()
        val sectionChildren = linkedMapOf<TextView, MutableList<View>>()
        var currentSection: TextView? = null
        for (i in 0 until holder.childCount) {
            val child = holder.getChildAt(i)
            if (child.id == R.id.settings_search) continue
            val isSection = child is TextView && child.id != View.NO_ID &&
                resources.getResourceEntryName(child.id).endsWith("_section_label")
            if (isSection) {
                currentSection = child as TextView
                sectionLabels.add(currentSection)
                sectionChildren[currentSection] = mutableListOf()
            } else if (currentSection != null) {
                sectionChildren[currentSection]?.add(child)
            } else {
                child.visibility = if (q.isEmpty() || viewMatchesQuery(child, q)) View.VISIBLE else View.GONE
            }
        }
        for (section in sectionLabels) {
            val kids = sectionChildren[section].orEmpty()
            var anyVisible = false
            for (kid in kids) {
                val show = q.isEmpty() || viewMatchesQuery(kid, q)
                kid.visibility = if (show) View.VISIBLE else View.GONE
                if (show) anyVisible = true
            }
            val sectionMatch = q.isEmpty() || viewMatchesQuery(section, q)
            section.visibility = if (anyVisible || sectionMatch) View.VISIBLE else View.GONE
            if (sectionMatch && q.isNotEmpty()) {
                kids.forEach { it.visibility = View.VISIBLE }
                section.visibility = View.VISIBLE
            }
        }
    }

    private fun viewMatchesQuery(view: View, q: String): Boolean {
        val texts = ArrayList<String>()
        fun collect(v: View) {
            if (v is TextView) {
                val t = v.text?.toString().orEmpty()
                if (t.isNotBlank()) texts.add(t)
                val h = v.hint?.toString().orEmpty()
                if (h.isNotBlank()) texts.add(h)
            }
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) collect(v.getChildAt(i))
            }
        }
        collect(view)
        return texts.any { it.lowercase().contains(q) }
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupCustomizeNotifications()
        setupUseEnglish()
        setupLanguage()
        setupManageCalendars()
        setupManageQuickFilterCalendars()
        setupHourFormat()
        setupAllowCreatingTasks()
        setupStartWeekOn()
        setupHighlightWeekends()
        setupHighlightWeekendsColor()
        setupDeleteAllEvents()
        setupDisplayDescription()
        setupReplaceDescription()
        setupWeekNumbers()
        setupShowGrid()
        setupWeeklyStart()
        setupMidnightSpanEvents()
        setupAllowCustomizeDayCount()
        setupStartWeekWithCurrentDay()
        setupShowEmptyDaysInAgenda()
        setupRedactTitlesInScreenshots()
        setupContinuumGoogleSignIn()
        setupContinuumPeerSync()
        setupUseGoogleCalendar()
        setupShowContactBirthdays()
        setupContinuumThemeMode()
        setupContinuumErrorLog()
        setupContinuumTravelBuffer()
        setupContinuumWorkingHours()
        setupContinuumSlotMin()
        setupContinuumWeeklyViewDays()
        setupContinuumNotifications()
        setupVibrate()
        setupReminderSound()
        setupReminderAudioStream()
        setupUseSameSnooze()
        setupLoopReminders()
        setupSnoozeTime()
        setupCaldavSync()
        setupManageSyncedCalendars()
        setupDefaultStartTime()
        setupDefaultDuration()
        setupDefaultCalendar()
        setupPullToRefresh()
        setupDefaultReminder()
        setupDefaultReminder1()
        setupDefaultReminder2()
        setupDefaultReminder3()
        setupDisplayPastEvents()
        setupFontSize()
        setupCustomizeWidgetColors()
        setupViewToOpenFromListWidget()
        setupDimEvents()
        setupDimCompletedTasks()
        setupAllowChangingTimeZones()
        updateTextColors(binding.settingsHolder)
        checkPrimaryColor()
        setupEnableAutomaticBackups()
        setupManageAutomaticBackups()
        setupExportEvents()
        setupImportEvents()
        setupExportSettings()
        setupImportSettings()

        arrayOf(
            binding.settingsContinuumSectionLabel,
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsRemindersLabel,
            binding.settingsCaldavLabel,
            binding.settingsNewEventsLabel,
            binding.settingsWeeklyViewLabel,
            binding.settingsMonthlyViewLabel,
            binding.settingsEventListsLabel,
            binding.settingsWidgetsLabel,
            binding.settingsEventsLabel,
            binding.settingsTasksLabel,
            binding.settingsBackupsLabel,
            binding.settingsMigratingLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    override fun onPause() {
        super.onPause()
        mStoredPrimaryColor = getProperPrimaryColor()
    }

    override fun onStop() {
        super.onStop()
        val reminders = sortedSetOf(
            config.defaultReminder1,
            config.defaultReminder2,
            config.defaultReminder3
        ).filter { it != REMINDER_OFF }
        config.defaultReminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        config.defaultReminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        config.defaultReminder3 = reminders.getOrElse(2) { REMINDER_OFF }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == GET_RINGTONE_URI && resultCode == RESULT_OK && resultData != null) {
            val newAlarmSound = storeNewYourAlarmSound(resultData)
            updateReminderSound(newAlarmSound)
        } else if (requestCode == PICK_SETTINGS_IMPORT_SOURCE_INTENT && resultCode == RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        } else if (requestCode == PICK_EVENTS_IMPORT_SOURCE_INTENT && resultCode == RESULT_OK && resultData != null && resultData.data != null) {
            tryImportEventsFromFile(resultData.data!!)
        } else if (requestCode == PICK_EVENTS_EXPORT_FILE_INTENT && resultCode == RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportEventsTo(calendarsToExport, outputStream)
        }
    }

    private fun checkPrimaryColor() {
        if (getProperPrimaryColor() != mStoredPrimaryColor) {
            ensureBackgroundThread {
                val calendars = eventsHelper.getCalendarsSync()
                if (calendars.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val calendar = calendars.first { it.caldavCalendarId == 0 }
                    calendar.color = getProperPrimaryColor()
                    eventsHelper.insertOrUpdateCalendarSync(calendar)
                }
            }
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomizeNotifications() {
        binding.settingsCustomizeNotificationsHolder.beVisibleIf(isOreoPlus())
        binding.settingsCustomizeNotificationsHolder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageCalendars() {
        binding.settingsManageCalendarsHolder.setOnClickListener {
            startActivity(Intent(this, ManageCalendarsActivity::class.java))
        }
    }

    private fun setupManageQuickFilterCalendars() = binding.apply {
        settingsManageQuickFilterCalendarsHolder.setOnClickListener {
            showQuickFilterPicker()
        }

        eventsHelper.getCalendars(this@SettingsActivity, false) {
            settingsManageQuickFilterCalendarsHolder.beGoneIf(it.size < 2)
        }
    }

    private fun setupHourFormat() = binding.apply {
        settingsHourFormat.isChecked = config.use24HourFormat
        settingsHourFormatHolder.setOnClickListener {
            settingsHourFormat.toggle()
            val use24 = settingsHourFormat.isChecked
            config.use24HourFormat = use24
            ensureBackgroundThread {
                try {
                    val sync = ContinuumSettingsSync(this@SettingsActivity)
                    sync.pushPatch(sync.loadLocal().copy(use24HourFormat = use24))
                } catch (e: Exception) {
                    org.fossify.calendar.continuum.ContinuumDiagnostics.e("use24HourFormat sync failed", e)
                }
            }
        }
    }

    private fun setupAllowCreatingTasks() = binding.apply {
        settingsAllowCreatingTasks.isChecked = config.allowCreatingTasks
        settingsAllowCreatingTasksHolder.setOnClickListener {
            settingsAllowCreatingTasks.toggle()
            config.allowCreatingTasks = settingsAllowCreatingTasks.isChecked
        }
    }

    private fun setupCaldavSync() = binding.apply {
        settingsCaldavSync.isChecked = config.caldavSync
        settingsCaldavSyncHolder.setOnClickListener {
            if (config.caldavSync) {
                toggleCaldavSync(false)
            } else {
                handlePermission(PERMISSION_WRITE_CALENDAR) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CALENDAR) {
                            if (it) {
                                handleNotificationPermission { granted ->
                                    if (granted) {
                                        toggleCaldavSync(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupPullToRefresh() = binding.apply {
        settingsCaldavPullToRefreshHolder.beVisibleIf(config.caldavSync)
        settingsCaldavPullToRefresh.isChecked = config.pullToRefresh
        settingsCaldavPullToRefreshHolder.setOnClickListener {
            settingsCaldavPullToRefresh.toggle()
            config.pullToRefresh = settingsCaldavPullToRefresh.isChecked
        }
    }

    private fun setupManageSyncedCalendars() = binding.apply {
        settingsManageSyncedCalendarsHolder.beVisibleIf(config.caldavSync)
        settingsManageSyncedCalendarsHolder.setOnClickListener {
            showCalendarPicker()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) = binding.apply {
        if (enable) {
            showCalendarPicker()
        } else {
            settingsCaldavSync.isChecked = false
            config.caldavSync = false
            settingsManageSyncedCalendarsHolder.beGone()
            settingsCaldavPullToRefreshHolder.beGone()

            ensureBackgroundThread {
                config.getSyncedCalendarIdsAsList().forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                calendarsDB.deleteCalendarsWithCalendarIds(config.getSyncedCalendarIdsAsList())
                updateDefaultCalendarText()
            }
        }
    }

    private fun showCalendarPicker() = binding.apply {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        ManageSyncedCalendarsDialog(this@SettingsActivity) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            if (newCalendarIds.isEmpty() && !config.caldavSync) {
                return@ManageSyncedCalendarsDialog
            }

            settingsManageSyncedCalendarsHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            settingsCaldavPullToRefreshHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            settingsCaldavSync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            if (settingsCaldavSync.isChecked) {
                toast(R.string.syncing)
            }

            ensureBackgroundThread {
                if (newCalendarIds.isNotEmpty()) {
                    val existingCalendarNames = eventsHelper.getCalendarsSync().map {
                        it.getDisplayTitle().lowercase(Locale.getDefault())
                    } as ArrayList<String>

                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingCalendarNames.contains(calendarTitle.lowercase(Locale.getDefault()))) {
                            val calendar = CalendarEntity(
                                id = null,
                                title = it.displayName,
                                color = it.color,
                                caldavCalendarId = it.id,
                                caldavDisplayName = it.displayName,
                                caldavEmail = it.accountName
                            )
                            existingCalendarNames.add(calendarTitle.lowercase(Locale.getDefault()))
                            eventsHelper.insertOrUpdateCalendar(this@SettingsActivity, calendar)
                        }
                    }

                    syncCalDAVCalendars {
                        calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                            if (settingsCaldavSync.isChecked) {
                                toast(R.string.synchronization_completed)
                            }
                        }
                    }
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                    eventsHelper.getCalendarWithCalDAVCalendarId(it)?.apply {
                        eventsHelper.deleteCalendars(arrayListOf(this), true)
                    }
                }

                calendarsDB.deleteCalendarsWithCalendarIds(removedCalendarIds)
                updateDefaultCalendarText()
            }
        }
    }

    private fun showQuickFilterPicker() {
        SelectCalendarsDialog(this, config.quickFilterCalendars) {
            config.quickFilterCalendars = it
        }
    }

    private fun setupStartWeekOn() = binding.apply {
        val items = arrayListOf(
            RadioItem(DateTimeConstants.SUNDAY, getString(org.fossify.commons.R.string.sunday)),
            RadioItem(DateTimeConstants.MONDAY, getString(org.fossify.commons.R.string.monday)),
            RadioItem(DateTimeConstants.TUESDAY, getString(org.fossify.commons.R.string.tuesday)),
            RadioItem(
                DateTimeConstants.WEDNESDAY,
                getString(org.fossify.commons.R.string.wednesday)
            ),
            RadioItem(DateTimeConstants.THURSDAY, getString(org.fossify.commons.R.string.thursday)),
            RadioItem(DateTimeConstants.FRIDAY, getString(org.fossify.commons.R.string.friday)),
            RadioItem(DateTimeConstants.SATURDAY, getString(org.fossify.commons.R.string.saturday)),
        )

        settingsStartWeekOn.text = getDayOfWeekString(config.firstDayOfWeek)
        settingsStartWeekOnHolder.setOnClickListener {
            RadioGroupDialog(this@SettingsActivity, items, config.firstDayOfWeek) { any ->
                val firstDayOfWeek = any as Int
                config.firstDayOfWeek = firstDayOfWeek
                settingsStartWeekOn.text = getDayOfWeekString(firstDayOfWeek)
                ensureBackgroundThread {
                    try {
                        val sync = ContinuumSettingsSync(this@SettingsActivity)
                        sync.pushPatch(
                            sync.loadLocal().copy(
                                firstDayOfWeek = org.fossify.calendar.continuum.ContinuumConsts
                                    .jodaToJsFirstDay(firstDayOfWeek),
                            ),
                        )
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "firstDayOfWeek sync failed",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun setupHighlightWeekends() = binding.apply {
        settingsHighlightWeekends.isChecked = config.highlightWeekends
        settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
        settingsHighlightWeekendsHolder.setOnClickListener {
            settingsHighlightWeekends.toggle()
            config.highlightWeekends = settingsHighlightWeekends.isChecked
            settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
        }
    }

    private fun setupHighlightWeekendsColor() = binding.apply {
        settingsHighlightWeekendsColor.setFillWithStroke(
            config.highlightWeekendsColor,
            getProperBackgroundColor()
        )
        settingsHighlightWeekendsColorHolder.setOnClickListener {
            ColorPickerDialog(
                activity = this@SettingsActivity,
                color = config.highlightWeekendsColor
            ) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    config.highlightWeekendsColor = color
                    settingsHighlightWeekendsColor.setFillWithStroke(
                        color,
                        getProperBackgroundColor()
                    )
                }
            }
        }
    }

    private fun setupDeleteAllEvents() = binding.apply {
        settingsDeleteAllEventsHolder.setOnClickListener {
            ConfirmationDialog(
                activity = this@SettingsActivity,
                messageId = R.string.delete_all_events_confirmation
            ) {
                eventsHelper.deleteAllEvents()
            }
        }
    }

    private fun setupDisplayDescription() = binding.apply {
        settingsDisplayDescription.isChecked = config.displayDescription
        settingsReplaceDescriptionHolder.beVisibleIf(config.displayDescription)
        settingsDisplayDescriptionHolder.setOnClickListener {
            settingsDisplayDescription.toggle()
            config.displayDescription = settingsDisplayDescription.isChecked
            settingsReplaceDescriptionHolder.beVisibleIf(config.displayDescription)
        }
    }

    private fun setupReplaceDescription() = binding.apply {
        settingsReplaceDescription.isChecked = config.replaceDescription
        settingsReplaceDescriptionHolder.setOnClickListener {
            settingsReplaceDescription.toggle()
            config.replaceDescription = settingsReplaceDescription.isChecked
        }
    }

    private fun setupWeeklyStart() = binding.apply {
        settingsStartWeeklyAt.text = getHoursString(config.startWeeklyAt)
        settingsStartWeeklyAtHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..16).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = config.startWeeklyAt
            ) {
                config.startWeeklyAt = it as Int
                settingsStartWeeklyAt.text = getHoursString(it)
            }
        }
    }

    private fun setupMidnightSpanEvents() = binding.apply {
        settingsMidnightSpanEvent.isChecked = config.showMidnightSpanningEventsAtTop
        settingsMidnightSpanEventsHolder.setOnClickListener {
            settingsMidnightSpanEvent.toggle()
            config.showMidnightSpanningEventsAtTop = settingsMidnightSpanEvent.isChecked
        }
    }

    private fun setupAllowCustomizeDayCount() = binding.apply {
        settingsAllowCustomizeDayCount.isChecked = config.allowCustomizeDayCount
        settingsAllowCustomizeDayCountHolder.setOnClickListener {
            settingsAllowCustomizeDayCount.toggle()
            config.allowCustomizeDayCount = settingsAllowCustomizeDayCount.isChecked
        }
    }

    private fun setupStartWeekWithCurrentDay() = binding.apply {
        settingsStartWeekWithCurrentDay.isChecked = config.startWeekWithCurrentDay
        settingsStartWeekWithCurrentDayHolder.setOnClickListener {
            settingsStartWeekWithCurrentDay.toggle()
            config.startWeekWithCurrentDay = settingsStartWeekWithCurrentDay.isChecked
            ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                    rollingWeekFromToday = settingsStartWeekWithCurrentDay.isChecked,
                ),
            )
        }
    }

    private fun setupShowEmptyDaysInAgenda() = binding.apply {
        settingsShowEmptyDaysInAgenda.isChecked = config.showEmptyDaysInAgenda
        settingsShowEmptyDaysInAgendaHolder.setOnClickListener {
            settingsShowEmptyDaysInAgenda.toggle()
            config.showEmptyDaysInAgenda = settingsShowEmptyDaysInAgenda.isChecked
            ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                    showEmptyDaysInAgenda = settingsShowEmptyDaysInAgenda.isChecked,
                ),
            )
        }
    }

    private fun setupRedactTitlesInScreenshots() = binding.apply {
        settingsRedactTitlesInScreenshots.isChecked = config.redactTitlesInScreenshots
        settingsRedactTitlesInScreenshotsHolder.setOnClickListener {
            settingsRedactTitlesInScreenshots.toggle()
            config.redactTitlesInScreenshots = settingsRedactTitlesInScreenshots.isChecked
            ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                    redactTitlesInScreenshots = settingsRedactTitlesInScreenshots.isChecked,
                ),
            )
        }
    }

    private fun setupUseGoogleCalendar() = binding.apply {
        val sync = ContinuumSettingsSync(this@SettingsActivity)
        settingsUseGoogleCalendar.isChecked = sync.loadLocal().useGoogleCalendar
        settingsUseGoogleCalendarHolder.setOnClickListener {
            settingsUseGoogleCalendar.toggle()
            val enabled = settingsUseGoogleCalendar.isChecked
            ensureBackgroundThread {
                try {
                    sync.pushPatch(sync.loadLocal().copy(useGoogleCalendar = enabled))
                    if (!enabled) {
                        runOnUiThread { toast(R.string.continuum_privacy_mode_on) }
                    }
                } catch (e: Exception) {
                    org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                        "useGoogleCalendar sync failed",
                        e,
                    )
                }
            }
        }
    }

    private fun setupContinuumThemeMode() = binding.apply {
        val sync = ContinuumSettingsSync(this@SettingsActivity)
        fun labelFor(mode: String) = when (mode) {
            "light" -> getString(R.string.continuum_theme_light)
            "dark" -> getString(R.string.continuum_theme_dark)
            else -> getString(R.string.continuum_theme_system)
        }
        fun refresh() {
            settingsContinuumThemeMode.text = labelFor(sync.loadLocal().themeMode)
        }
        refresh()
        settingsContinuumThemeModeHolder.setOnClickListener {
            val current = sync.loadLocal().themeMode
            val items = arrayListOf(
                RadioItem(0, getString(R.string.continuum_theme_system)),
                RadioItem(1, getString(R.string.continuum_theme_light)),
                RadioItem(2, getString(R.string.continuum_theme_dark)),
            )
            val checked = when (current) {
                "light" -> 1
                "dark" -> 2
                else -> 0
            }
            RadioGroupDialog(this@SettingsActivity, items, checked) { any ->
                val mode = when (any as Int) {
                    1 -> "light"
                    2 -> "dark"
                    else -> "system"
                }
                ensureBackgroundThread {
                    try {
                        sync.pushPatch(sync.loadLocal().copy(themeMode = mode))
                        runOnUiThread { refresh() }
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "themeMode sync failed",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun setupContinuumWeeklyViewDays() = binding.apply {
        val sync = ContinuumSettingsSync(this@SettingsActivity)
        fun refresh() {
            settingsContinuumWeeklyViewDays.text =
                getString(R.string.continuum_days_short, sync.loadLocal().weeklyViewDays)
        }
        refresh()
        settingsContinuumWeeklyViewDaysHolder.setOnClickListener {
            val current = sync.loadLocal().weeklyViewDays
            val items = arrayListOf(
                RadioItem(1, getString(R.string.continuum_days_short, 1)),
                RadioItem(3, getString(R.string.continuum_days_short, 3)),
                RadioItem(5, getString(R.string.continuum_days_short, 5)),
                RadioItem(7, getString(R.string.continuum_days_short, 7)),
                RadioItem(10, getString(R.string.continuum_days_short, 10)),
                RadioItem(14, getString(R.string.continuum_days_short, 14)),
            )
            RadioGroupDialog(this@SettingsActivity, items, current) { any ->
                val days = any as Int
                config.weeklyViewDays = days
                ensureBackgroundThread {
                    try {
                        sync.pushPatch(sync.loadLocal().copy(weeklyViewDays = days))
                        runOnUiThread { refresh() }
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "weeklyViewDays sync failed",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun setupContinuumNotifications() = binding.apply {
        val sync = ContinuumSettingsSync(this@SettingsActivity)
        settingsContinuumNotifications.isChecked = sync.loadLocal().notificationEnabled
        settingsContinuumNotificationsHolder.setOnClickListener {
            val enabling = !settingsContinuumNotifications.isChecked
            if (enabling) {
                handleNotificationPermission { granted ->
                    if (!granted) {
                        toast(org.fossify.commons.R.string.notifications_disabled)
                        return@handleNotificationPermission
                    }
                    settingsContinuumNotifications.isChecked = true
                    ensureBackgroundThread {
                        try {
                            sync.pushPatch(sync.loadLocal().copy(notificationEnabled = true))
                        } catch (e: Exception) {
                            org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                                "notificationEnabled sync failed",
                                e,
                            )
                        }
                    }
                }
            } else {
                settingsContinuumNotifications.isChecked = false
                ensureBackgroundThread {
                    try {
                        sync.pushPatch(sync.loadLocal().copy(notificationEnabled = false))
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "notificationEnabled sync failed",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun setupContinuumGoogleSignIn() = binding.apply {
        settingsContinuumGoogleSignIn.text = getString(R.string.continuum_connect_google_calendars)
        settingsContinuumGoogleSignInHolder.setOnClickListener {
            // Calendars only: device Google account via CalendarContract / CalDAV.
            handlePermission(PERMISSION_WRITE_CALENDAR) { writeGranted ->
                if (!writeGranted) return@handlePermission
                handlePermission(PERMISSION_READ_CALENDAR) { readGranted ->
                    if (!readGranted) return@handlePermission
                    handleNotificationPermission { granted ->
                        if (granted) {
                            toast(R.string.continuum_connect_google_calendars_summary)
                            toggleCaldavSync(true)
                        }
                    }
                }
            }
        }
    }

    private fun setupContinuumPeerSync() = binding.apply {
        fun refresh() {
            val signedIn = ContinuumGoogleAuth(this@SettingsActivity).isSignedIn()
            settingsContinuumPeerSync.text = getString(
                if (signedIn) R.string.continuum_peer_sync_connected
                else R.string.continuum_settings_sync_sign_in_for_desktop,
            )
            if (!ContinuumSettingsOAuth.usesAndroidClient()) {
                settingsContinuumPeerSync.text =
                    "${settingsContinuumPeerSync.text}\n${getString(R.string.continuum_peer_sync_need_android_client)}"
            }
        }
        refresh()
        settingsContinuumPeerSyncHolder.setOnClickListener {
            if (!ContinuumSettingsOAuth.isConfigured()) {
                toast(R.string.continuum_google_client_id_missing)
                return@setOnClickListener
            }
            if (ContinuumGoogleAuth(this@SettingsActivity).isSignedIn()) {
                ensureBackgroundThread {
                    try {
                        ContinuumSettingsSync(this@SettingsActivity).pushLocalAfterAuth()
                        ContinuumLocalEventsSync(this@SettingsActivity).reconcilePeerRemote()
                        runOnUiThread {
                            refresh()
                            toast(R.string.continuum_peer_sync_connected_toast)
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showErrorToast(e) }
                    }
                }
            } else {
                toast(R.string.continuum_settings_sync_sign_in_for_desktop)
                ContinuumSettingsOAuth.startIfConfigured(this@SettingsActivity)
            }
        }
        settingsContinuumPasteOauthHolder.setOnClickListener {
            val input = android.widget.EditText(this@SettingsActivity).apply {
                hint = getString(R.string.continuum_peer_sync_paste_url_hint)
                minLines = 3
                setTextIsSelectable(true)
            }
            androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                .setTitle(R.string.continuum_peer_sync_paste_url)
                .setMessage(R.string.continuum_peer_sync_paste_url_hint)
                .setView(input)
                .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
                    val url = input.text?.toString()?.trim().orEmpty()
                    if (url.isBlank()) {
                        toast(R.string.continuum_peer_sync_paste_invalid)
                    } else {
                        ContinuumAuthActivity.startWithPasteUrl(this@SettingsActivity, url)
                    }
                }
                .setNegativeButton(org.fossify.commons.R.string.cancel, null)
                .show()
        }
    }

    private fun setupShowContactBirthdays() = binding.apply {
        settingsShowContactBirthdays.isChecked = config.showContactBirthdays
        settingsShowContactBirthdaysHolder.setOnClickListener {
            settingsShowContactBirthdays.toggle()
            val show = settingsShowContactBirthdays.isChecked
            config.showContactBirthdays = show
            ensureBackgroundThread {
                try {
                    org.fossify.calendar.continuum.ContinuumBirthdayFilter.invalidateCache()
                    val sync = ContinuumSettingsSync(this@SettingsActivity)
                    val result = sync.pushPatch(
                        sync.loadLocal().copy(showContactBirthdays = show),
                    )
                    runOnUiThread {
                        updateWidgets()
                        toast(
                            if (show) R.string.continuum_show_contact_birthdays
                            else R.string.continuum_birthdays_hidden,
                        )
                        if (!result.uploadedToDrive) {
                            if (ContinuumSettingsOAuth.startIfConfigured(this@SettingsActivity)) {
                                toast(R.string.continuum_settings_sync_sign_in_for_desktop)
                            } else {
                                toast(R.string.continuum_settings_sync_local_only)
                            }
                        } else {
                            toast(R.string.continuum_settings_uploaded)
                        }
                    }
                } catch (e: Exception) {
                    org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                        "showContactBirthdays toggle failed",
                        e,
                    )
                    runOnUiThread {
                        settingsShowContactBirthdays.isChecked = !show
                        config.showContactBirthdays = !show
                        showErrorToast(e)
                    }
                }
            }
        }
    }

    private fun setupContinuumErrorLog() = binding.apply {
        settingsContinuumErrorLogHolder.setOnClickListener {
            ensureBackgroundThread {
                try {
                    val file = org.fossify.calendar.continuum.ContinuumDiagnostics.logFile(this@SettingsActivity)
                    val text = if (file.exists()) file.readText() else ""
                    runOnUiThread {
                        if (text.isBlank()) {
                            toast(R.string.continuum_error_log_empty)
                            return@runOnUiThread
                        }
                        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(
                            android.content.ClipData.newPlainText("continuum_error_log", text.takeLast(50_000)),
                        )
                        toast(R.string.continuum_error_log_copied)
                        org.fossify.commons.dialogs.ConfirmationDialog(
                            activity = this@SettingsActivity,
                            message = text.takeLast(3500),
                            positive = org.fossify.commons.R.string.ok,
                            negative = 0,
                        ) {}
                    }
                } catch (e: Exception) {
                    org.fossify.calendar.continuum.ContinuumDiagnostics.e("Failed reading error log", e)
                    runOnUiThread { showErrorToast(e) }
                }
            }
        }
    }

    private fun setupContinuumTravelBuffer() = binding.apply {
        fun refresh() {
            val minutes = ContinuumSettingsSync(this@SettingsActivity).loadLocal().travelBufferMinutes
            settingsContinuumTravelBuffer.text = getString(R.string.continuum_minutes_short, minutes)
        }
        refresh()
        settingsContinuumTravelBufferHolder.setOnClickListener {
            val current = ContinuumSettingsSync(this@SettingsActivity).loadLocal().travelBufferMinutes
            val items = arrayListOf(
                RadioItem(0, getString(R.string.continuum_minutes_short, 0)),
                RadioItem(5, getString(R.string.continuum_minutes_short, 5)),
                RadioItem(10, getString(R.string.continuum_minutes_short, 10)),
                RadioItem(15, getString(R.string.continuum_minutes_short, 15)),
                RadioItem(30, getString(R.string.continuum_minutes_short, 30)),
                RadioItem(45, getString(R.string.continuum_minutes_short, 45)),
                RadioItem(60, getString(R.string.continuum_minutes_short, 60)),
            )
            RadioGroupDialog(this@SettingsActivity, items, current) {
                val minutes = (it as Int).coerceAtLeast(0)
                ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                    ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                        travelBufferMinutes = minutes,
                    ),
                )
                refresh()
            }
        }
    }

    private fun setupContinuumWorkingHours() = binding.apply {
        fun refresh() {
            val hours = ContinuumSettingsSync(this@SettingsActivity).loadLocal().workingHours
            settingsContinuumWorkingHours.text = "${hours.start} – ${hours.end}"
        }
        refresh()
        settingsContinuumWorkingHoursHolder.setOnClickListener {
            val current = ContinuumSettingsSync(this@SettingsActivity).loadLocal().workingHours
            pickContinuumClockTime(
                title = getString(R.string.continuum_working_hours_start),
                hm = current.start,
            ) { startHm ->
                pickContinuumClockTime(
                    title = getString(R.string.continuum_working_hours_end),
                    hm = current.end,
                ) { endHm ->
                    val startMin = hmToMinutes(startHm)
                    val endMin = hmToMinutes(endHm)
                    if (endMin <= startMin) {
                        toast(R.string.continuum_working_hours_invalid)
                        return@pickContinuumClockTime
                    }
                    ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                        ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                            workingHours = org.fossify.calendar.continuum.ContinuumWorkingHours(
                                start = startHm,
                                end = endHm,
                            ),
                        ),
                    )
                    refresh()
                }
            }
        }
    }

    private fun hmToMinutes(hm: String): Int {
        val parts = hm.split(':')
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun pickContinuumClockTime(title: String, hm: String, onPicked: (String) -> Unit) {
        val parts = hm.split(':')
        val hour = (parts.getOrNull(0)?.toIntOrNull() ?: 9).coerceIn(0, 23)
        val minute = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
        if (isDynamicTheme()) {
            val timeFormat = if (config.use24HourFormat) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(title)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                onPicked("%02d:%02d".format(timePicker.hour, timePicker.minute))
            }
            timePicker.show(supportFragmentManager, "continuum_hours_$title")
        } else {
            TimePickerDialog(
                this,
                getTimePickerDialogTheme(),
                { _, hourOfDay, minuteOfHour ->
                    onPicked("%02d:%02d".format(hourOfDay, minuteOfHour))
                },
                hour,
                minute,
                config.use24HourFormat,
            ).apply { setTitle(title) }.show()
        }
    }

    private fun setupContinuumSlotMin() = binding.apply {
        fun refresh() {
            val minutes = ContinuumSettingsSync(this@SettingsActivity).loadLocal().slotMinMinutes
            settingsContinuumSlotMin.text = getString(R.string.continuum_minutes_short, minutes)
        }
        refresh()
        settingsContinuumSlotMinHolder.setOnClickListener {
            val current = ContinuumSettingsSync(this@SettingsActivity).loadLocal().slotMinMinutes
            val items = arrayListOf(
                RadioItem(15, getString(R.string.continuum_minutes_short, 15)),
                RadioItem(30, getString(R.string.continuum_minutes_short, 30)),
                RadioItem(45, getString(R.string.continuum_minutes_short, 45)),
                RadioItem(60, getString(R.string.continuum_minutes_short, 60)),
                RadioItem(90, getString(R.string.continuum_minutes_short, 90)),
                RadioItem(120, getString(R.string.continuum_minutes_short, 120)),
            )
            RadioGroupDialog(this@SettingsActivity, items, current) {
                val minutes = (it as Int).coerceAtLeast(1)
                ContinuumSettingsSync(this@SettingsActivity).pushPatch(
                    ContinuumSettingsSync(this@SettingsActivity).loadLocal().copy(
                        slotMinMinutes = minutes,
                    ),
                )
                refresh()
            }
        }
    }

    private fun setupWeekNumbers() = binding.apply {
        settingsWeekNumbers.isChecked = config.showWeekNumbers
        settingsWeekNumbersHolder.setOnClickListener {
            settingsWeekNumbers.toggle()
            config.showWeekNumbers = settingsWeekNumbers.isChecked
        }
    }

    private fun setupShowGrid() = binding.apply {
        settingsShowGrid.isChecked = config.showGrid
        settingsShowGridHolder.setOnClickListener {
            settingsShowGrid.toggle()
            config.showGrid = settingsShowGrid.isChecked
        }
    }

    @Deprecated("Not used on Oreo+ devices")
    private fun setupReminderSound() = binding.apply {
        settingsReminderSoundHolder.beGoneIf(isOreoPlus())
        settingsReminderSound.text = config.reminderSoundTitle

        settingsReminderSoundHolder.setOnClickListener {
            SelectAlarmSoundDialog(
                activity = this@SettingsActivity,
                currentUri = config.reminderSoundUri,
                audioStream = config.reminderAudioStream,
                pickAudioIntentId = GET_RINGTONE_URI,
                type = RingtoneManager.TYPE_NOTIFICATION,
                loopAudio = false,
                onAlarmPicked = {
                    if (it != null) {
                        updateReminderSound(it)
                    }
                },
                onAlarmSoundDeleted = {
                    if (it.uri == config.reminderSoundUri) {
                        val defaultAlarm = getDefaultAlarmSound(RingtoneManager.TYPE_NOTIFICATION)
                        updateReminderSound(defaultAlarm)
                    }
                })
        }
    }

    @Deprecated("Not used on Oreo+ devices")
    private fun updateReminderSound(alarmSound: AlarmSound) {
        config.reminderSoundTitle = alarmSound.title
        config.reminderSoundUri = alarmSound.uri
        binding.settingsReminderSound.text = alarmSound.title
    }

    private fun setupReminderAudioStream() = binding.apply {
        settingsReminderAudioStream.text = getAudioStreamText()
        settingsReminderAudioStreamHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(
                    AudioManager.STREAM_NOTIFICATION,
                    getString(R.string.notification_stream)
                ),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = config.reminderAudioStream
            ) {
                config.reminderAudioStream = it as Int
                settingsReminderAudioStream.text = getAudioStreamText()
            }
        }
    }

    private fun getAudioStreamText() = getString(
        when (config.reminderAudioStream) {
            AudioManager.STREAM_ALARM -> R.string.alarm_stream
            AudioManager.STREAM_SYSTEM -> R.string.system_stream
            AudioManager.STREAM_NOTIFICATION -> R.string.notification_stream
            else -> R.string.ring_stream
        }
    )

    private fun setupVibrate() = binding.apply {
        settingsVibrate.isChecked = config.vibrateOnReminder
        settingsVibrateHolder.setOnClickListener {
            settingsVibrate.toggle()
            config.vibrateOnReminder = settingsVibrate.isChecked
        }
    }

    private fun setupLoopReminders() = binding.apply {
        settingsLoopReminders.isChecked = config.loopReminders
        settingsLoopRemindersHolder.setOnClickListener {
            settingsLoopReminders.toggle()
            config.loopReminders = settingsLoopReminders.isChecked
        }
    }

    private fun setupUseSameSnooze() = binding.apply {
        settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        settingsUseSameSnooze.isChecked = config.useSameSnooze
        settingsUseSameSnoozeHolder.setOnClickListener {
            settingsUseSameSnooze.toggle()
            config.useSameSnooze = settingsUseSameSnooze.isChecked
            settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeTime()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                updateSnoozeTime()
                ensureBackgroundThread {
                    try {
                        val sync = ContinuumSettingsSync(this@SettingsActivity)
                        sync.pushPatch(
                            sync.loadLocal().copy(defaultSnoozeMinutes = config.snoozeTime),
                        )
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "defaultSnoozeMinutes sync failed",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun updateSnoozeTime() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun setupDefaultReminder() = binding.apply {
        settingsUseLastEventReminders.isChecked = config.usePreviousEventReminders
        toggleDefaultRemindersVisibility(!config.usePreviousEventReminders)
        settingsUseLastEventRemindersHolder.setOnClickListener {
            settingsUseLastEventReminders.toggle()
            config.usePreviousEventReminders = settingsUseLastEventReminders.isChecked
            toggleDefaultRemindersVisibility(!settingsUseLastEventReminders.isChecked)
        }
    }

    private fun setupDefaultReminder1() = binding.apply {
        settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
        settingsDefaultReminder1Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder1) {
                config.defaultReminder1 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
            }
        }
    }

    private fun setupDefaultReminder2() = binding.apply {
        settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
        settingsDefaultReminder2Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder2) {
                config.defaultReminder2 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
            }
        }
    }

    private fun setupDefaultReminder3() = binding.apply {
        settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
        settingsDefaultReminder3Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder3) {
                config.defaultReminder3 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
            }
        }
    }

    private fun toggleDefaultRemindersVisibility(show: Boolean) = binding.apply {
        arrayOf(
            settingsDefaultReminder1Holder,
            settingsDefaultReminder2Holder,
            settingsDefaultReminder3Holder
        ).forEach {
            it.beVisibleIf(show)
        }
    }

    private fun getHoursString(hours: Int): String {
        return if (config.use24HourFormat) {
            String.format("%02d:00", hours)
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hours)
            calendar.set(Calendar.MINUTE, 0)
            val format = SimpleDateFormat("hh.mm aa")
            format.format(calendar.time)
        }
    }

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        binding.settingsDisplayPastEventsHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, displayPastEvents * 60) {
                val result = it / 60
                displayPastEvents = result
                config.displayPastEvents = result
                updatePastEventsText(result)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        binding.settingsDisplayPastEvents.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0) {
            getString(org.fossify.commons.R.string.never)
        } else {
            getFormattedMinutes(displayPastEvents, false)
        }
    }

    private fun setupFontSize() = binding.apply {
        settingsFontSize.text = getFontSizeText()
        settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(org.fossify.commons.R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(org.fossify.commons.R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(org.fossify.commons.R.string.large)),
                RadioItem(
                    FONT_SIZE_EXTRA_LARGE,
                    getString(org.fossify.commons.R.string.extra_large)
                )
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settingsFontSize.text = getFontSizeText()
                updateWidgets()
            }
        }
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupViewToOpenFromListWidget() = binding.apply {
        settingsListWidgetViewToOpen.text = getDefaultViewText()
        settingsListWidgetViewToOpenHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
                RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
                RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
                RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
                RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
                RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list)),
                RadioItem(LAST_VIEW, getString(R.string.last_view))
            )

            RadioGroupDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = config.listWidgetViewToOpen
            ) {
                config.listWidgetViewToOpen = it as Int
                settingsListWidgetViewToOpen.text = getDefaultViewText()
                updateWidgets()
            }
        }
    }

    private fun getDefaultViewText() = getString(
        when (config.listWidgetViewToOpen) {
            DAILY_VIEW -> R.string.daily_view
            WEEKLY_VIEW -> R.string.weekly_view
            MONTHLY_VIEW -> R.string.monthly_view
            MONTHLY_DAILY_VIEW -> R.string.monthly_daily_view
            YEARLY_VIEW -> R.string.yearly_view
            EVENTS_LIST_VIEW -> R.string.simple_event_list
            else -> R.string.last_view
        }
    )

    private fun setupDimEvents() = binding.apply {
        settingsDimPastEvents.isChecked = config.dimPastEvents
        settingsDimPastEventsHolder.setOnClickListener {
            settingsDimPastEvents.toggle()
            config.dimPastEvents = settingsDimPastEvents.isChecked
        }
    }

    private fun setupDimCompletedTasks() = binding.apply {
        settingsDimCompletedTasks.isChecked = config.dimCompletedTasks
        settingsDimCompletedTasksHolder.setOnClickListener {
            settingsDimCompletedTasks.toggle()
            config.dimCompletedTasks = settingsDimCompletedTasks.isChecked
        }
    }

    private fun setupAllowChangingTimeZones() = binding.apply {
        settingsAllowChangingTimeZones.isChecked = config.allowChangingTimeZones
        settingsAllowChangingTimeZonesHolder.setOnClickListener {
            settingsAllowChangingTimeZones.toggle()
            config.allowChangingTimeZones = settingsAllowChangingTimeZones.isChecked
        }
    }

    private fun setupDefaultStartTime() {
        updateDefaultStartTimeText()
        binding.settingsDefaultStartTimeHolder.setOnClickListener {
            val currentDefaultTime = when (config.defaultStartTime) {
                DEFAULT_START_TIME_NEXT_FULL_HOUR -> DEFAULT_START_TIME_NEXT_FULL_HOUR
                DEFAULT_START_TIME_CURRENT_TIME -> DEFAULT_START_TIME_CURRENT_TIME
                else -> 0
            }

            val items = ArrayList<RadioItem>()
            items.add(RadioItem(DEFAULT_START_TIME_CURRENT_TIME, getString(R.string.current_time)))
            items.add(
                RadioItem(
                    DEFAULT_START_TIME_NEXT_FULL_HOUR,
                    getString(R.string.next_full_hour)
                )
            )
            items.add(RadioItem(0, getString(R.string.other_time)))

            RadioGroupDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = currentDefaultTime
            ) {
                if (it as Int == DEFAULT_START_TIME_NEXT_FULL_HOUR || it == DEFAULT_START_TIME_CURRENT_TIME) {
                    config.defaultStartTime = it
                    updateDefaultStartTimeText()
                } else {
                    val timeListener =
                        TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                            config.defaultStartTime = hourOfDay * 60 + minute
                            updateDefaultStartTimeText()
                        }

                    val currentDateTime = DateTime.now()

                    if (isDynamicTheme()) {
                        val timeFormat = if (config.use24HourFormat) {
                            TimeFormat.CLOCK_24H
                        } else {
                            TimeFormat.CLOCK_12H
                        }

                        val timePicker = MaterialTimePicker.Builder()
                            .setTimeFormat(timeFormat)
                            .setHour(currentDateTime.hourOfDay)
                            .setMinute(currentDateTime.minuteOfHour)
                            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                            .build()

                        timePicker.addOnPositiveButtonClickListener {
                            config.defaultStartTime = timePicker.hour * 60 + timePicker.minute
                            updateDefaultStartTimeText()
                        }

                        timePicker.show(supportFragmentManager, "")
                    } else {
                        TimePickerDialog(
                            this,
                            getTimePickerDialogTheme(),
                            timeListener,
                            currentDateTime.hourOfDay,
                            currentDateTime.minuteOfHour,
                            config.use24HourFormat
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateDefaultStartTimeText() {
        binding.settingsDefaultStartTime.text = when (config.defaultStartTime) {
            DEFAULT_START_TIME_CURRENT_TIME -> getString(R.string.current_time)
            DEFAULT_START_TIME_NEXT_FULL_HOUR -> getString(R.string.next_full_hour)
            else -> {
                val hours = config.defaultStartTime / 60
                val minutes = config.defaultStartTime % 60
                val dateTime = DateTime.now().withHourOfDay(hours).withMinuteOfHour(minutes)
                Formatter.getTime(this, dateTime)
            }
        }
    }

    private fun setupDefaultDuration() {
        updateDefaultDurationText()
        binding.settingsDefaultDurationHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, config.defaultDuration * 60) {
                val result = it / 60
                config.defaultDuration = result
                updateDefaultDurationText()
            }
        }
    }

    private fun updateDefaultDurationText() {
        val duration = config.defaultDuration
        binding.settingsDefaultDuration.text = if (duration == 0) {
            "0 ${getString(org.fossify.commons.R.string.minutes_raw)}"
        } else {
            getFormattedMinutes(duration, false)
        }
    }

    private fun setupDefaultCalendar() = binding.apply {
        updateDefaultCalendarText()
        settingsDefaultCalendar.text = getString(R.string.last_used_one)
        settingsDefaultCalendarHolder.setOnClickListener {
            SelectCalendarDialog(
                activity = this@SettingsActivity,
                currCalendar = config.defaultCalendarId,
                showCalDAVCalendars = true,
                showNewCalendarOption = false,
                addLastUsedOneAsFirstOption = true,
                showOnlyWritable = true,
                showManageCalendars = false
            ) {
                config.defaultCalendarId = it.id!!
                updateDefaultCalendarText()
                // Keep Continuum settings in sync for desktop parity / Drive App Data.
                ensureBackgroundThread {
                    try {
                        val sync = ContinuumSettingsSync(this@SettingsActivity)
                        val logical = if (it.id == null || it.id == -1L) {
                            "last_used"
                        } else {
                            org.fossify.calendar.continuum.DefaultWriteCalendar.logicalIdFor(it)
                        }
                        sync.pushPatch(sync.loadLocal().copy(defaultWriteCalendarId = logical))
                    } catch (e: Exception) {
                        org.fossify.calendar.continuum.ContinuumDiagnostics.e(
                            "Failed syncing default write calendar",
                            e,
                        )
                    }
                }
            }
        }
    }

    private fun updateDefaultCalendarText() {
        if (config.defaultCalendarId == -1L) {
            runOnUiThread {
                binding.settingsDefaultCalendar.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val calendar = calendarsDB.getCalendarWithId(config.defaultCalendarId)
                if (calendar != null) {
                    config.lastUsedCaldavCalendarId = calendar.caldavCalendarId
                    runOnUiThread {
                        binding.settingsDefaultCalendar.text = calendar.title
                    }
                } else {
                    config.defaultCalendarId = -1
                    updateDefaultCalendarText()
                }
            }
        }
    }

    private fun setupEnableAutomaticBackups() = binding.apply {
        settingsBackupsLabel.beVisibleIf(isRPlus())
        settingsBackupsDivider.beVisibleIf(isRPlus())
        settingsEnableAutomaticBackupsHolder.beVisibleIf(isRPlus())
        settingsEnableAutomaticBackups.isChecked = config.autoBackup
        settingsEnableAutomaticBackupsHolder.setOnClickListener {
            val wasBackupDisabled = !config.autoBackup
            if (wasBackupDisabled) {
                maybeRequestExactAlarmPermission {
                    ManageAutomaticBackupsDialog(
                        activity = this@SettingsActivity,
                        onSuccess = {
                            enableOrDisableAutomaticBackups(true)
                            scheduleNextAutomaticBackup()
                        }
                    )
                }
            } else {
                cancelScheduledAutomaticBackup()
                enableOrDisableAutomaticBackups(false)
            }
        }
    }

    private fun setupManageAutomaticBackups() = binding.apply {
        settingsManageAutomaticBackupsHolder.beVisibleIf(isRPlus() && config.autoBackup)
        settingsManageAutomaticBackupsHolder.setOnClickListener {
            maybeRequestExactAlarmPermission {
                ManageAutomaticBackupsDialog(
                    activity = this@SettingsActivity,
                    onSuccess = {
                        scheduleNextAutomaticBackup()
                    }
                )
            }
        }
    }

    private fun enableOrDisableAutomaticBackups(enable: Boolean) = binding.apply {
        config.autoBackup = enable
        settingsEnableAutomaticBackups.isChecked = enable
        settingsManageAutomaticBackupsHolder.beVisibleIf(enable)
    }

    private fun setupExportEvents() {
        binding.eventsExportHolder.setOnClickListener {
            tryExportEvents()
        }
    }

    private fun setupImportEvents() {
        binding.eventsImportHolder.setOnClickListener {
            tryImportEvents()
        }
    }

    private fun setupExportSettings() {
        binding.settingsExportHolder.setOnClickListener {
            val configItems = LinkedHashMap<String, Any>().apply {
                put(IS_GLOBAL_THEME_ENABLED, config.isGlobalThemeEnabled)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(APP_ICON_COLOR, config.appIconColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(WEEK_NUMBERS, config.showWeekNumbers)
                put(START_WEEKLY_AT, config.startWeeklyAt)
                put(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, config.showMidnightSpanningEventsAtTop)
                put(ALLOW_CUSTOMIZE_DAY_COUNT, config.allowCustomizeDayCount)
                put(START_WEEK_WITH_CURRENT_DAY, config.startWeekWithCurrentDay)
                put(VIBRATE, config.vibrateOnReminder)
                put(LAST_EVENT_REMINDER_MINUTES, config.lastEventReminderMinutes1)
                put(LAST_EVENT_REMINDER_MINUTES_2, config.lastEventReminderMinutes2)
                put(LAST_EVENT_REMINDER_MINUTES_3, config.lastEventReminderMinutes3)
                put(DISPLAY_PAST_EVENTS, config.displayPastEvents)
                put(FONT_SIZE, config.fontSize)
                put(LIST_WIDGET_VIEW_TO_OPEN, config.listWidgetViewToOpen)
                put(REMINDER_AUDIO_STREAM, config.reminderAudioStream)
                put(DISPLAY_DESCRIPTION, config.displayDescription)
                put(REPLACE_DESCRIPTION, config.replaceDescription)
                put(SHOW_GRID, config.showGrid)
                put(LOOP_REMINDERS, config.loopReminders)
                put(DIM_PAST_EVENTS, config.dimPastEvents)
                put(DIM_COMPLETED_TASKS, config.dimCompletedTasks)
                put(ALLOW_CHANGING_TIME_ZONES, config.allowChangingTimeZones)
                put(USE_PREVIOUS_EVENT_REMINDERS, config.usePreviousEventReminders)
                put(DEFAULT_REMINDER_1, config.defaultReminder1)
                put(DEFAULT_REMINDER_2, config.defaultReminder2)
                put(DEFAULT_REMINDER_3, config.defaultReminder3)
                put(PULL_TO_REFRESH, config.pullToRefresh)
                put(DEFAULT_START_TIME, config.defaultStartTime)
                put(DEFAULT_DURATION, config.defaultDuration)
                put(USE_SAME_SNOOZE, config.useSameSnooze)
                put(SNOOZE_TIME, config.snoozeTime)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(FIRST_DAY_OF_WEEK, config.firstDayOfWeek)
                put(HIGHLIGHT_WEEKENDS, config.highlightWeekends)
                put(HIGHLIGHT_WEEKENDS_COLOR, config.highlightWeekendsColor)
                put(ALLOW_CREATING_TASKS, config.allowCreatingTasks)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        binding.settingsImportHolder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"

                    try {
                        startActivityForResult(this, PICK_SETTINGS_IMPORT_SOURCE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(
                            org.fossify.commons.R.string.system_service_disabled,
                            Toast.LENGTH_LONG
                        )
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                parseFile(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                IS_GLOBAL_THEME_ENABLED -> config.isGlobalThemeEnabled = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                APP_ICON_COLOR -> {
                    if (getAppIconColors().contains(value.toInt())) {
                        config.appIconColor = value.toInt()
                        checkAppIconColor()
                    }
                }

                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                WEEK_NUMBERS -> config.showWeekNumbers = value.toBoolean()
                START_WEEKLY_AT -> config.startWeeklyAt = value.toInt()
                SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP -> config.showMidnightSpanningEventsAtTop =
                    value.toBoolean()

                ALLOW_CUSTOMIZE_DAY_COUNT -> config.allowCustomizeDayCount = value.toBoolean()
                START_WEEK_WITH_CURRENT_DAY -> config.startWeekWithCurrentDay = value.toBoolean()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
                LIST_WIDGET_VIEW_TO_OPEN -> config.listWidgetViewToOpen = value.toInt()
                REMINDER_AUDIO_STREAM -> config.reminderAudioStream = value.toInt()
                DISPLAY_DESCRIPTION -> config.displayDescription = value.toBoolean()
                REPLACE_DESCRIPTION -> config.replaceDescription = value.toBoolean()
                SHOW_GRID -> config.showGrid = value.toBoolean()
                LOOP_REMINDERS -> config.loopReminders = value.toBoolean()
                DIM_PAST_EVENTS -> config.dimPastEvents = value.toBoolean()
                DIM_COMPLETED_TASKS -> config.dimCompletedTasks = value.toBoolean()
                ALLOW_CHANGING_TIME_ZONES -> config.allowChangingTimeZones = value.toBoolean()
                USE_PREVIOUS_EVENT_REMINDERS -> config.usePreviousEventReminders = value.toBoolean()
                DEFAULT_REMINDER_1 -> config.defaultReminder1 = value.toInt()
                DEFAULT_REMINDER_2 -> config.defaultReminder2 = value.toInt()
                DEFAULT_REMINDER_3 -> config.defaultReminder3 = value.toInt()
                PULL_TO_REFRESH -> config.pullToRefresh = value.toBoolean()
                DEFAULT_START_TIME -> config.defaultStartTime = value.toInt()
                DEFAULT_DURATION -> config.defaultDuration = value.toInt()
                USE_SAME_SNOOZE -> config.useSameSnooze = value.toBoolean()
                SNOOZE_TIME -> config.snoozeTime = value.toInt()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                SUNDAY_FIRST -> config.firstDayOfWeek = DateTimeConstants.SUNDAY
                FIRST_DAY_OF_WEEK -> config.firstDayOfWeek = value.toInt()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
            }
        }

        runOnUiThread {
            val msg = if (configValues.isNotEmpty()) {
                org.fossify.commons.R.string.settings_imported_successfully
            } else {
                org.fossify.commons.R.string.no_entries_for_importing
            }

            toast(msg)

            setupSettingItems()
            updateWidgets()
        }
    }

    private fun tryImportEvents() {
        if (isQPlus()) {
            handleNotificationPermission { granted ->
                if (granted) {
                    hideKeyboard()
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/calendar"

                        try {
                            startActivityForResult(this, PICK_EVENTS_IMPORT_SOURCE_INTENT)
                        } catch (e: ActivityNotFoundException) {
                            toast(
                                org.fossify.commons.R.string.system_service_disabled,
                                Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }
                } else {
                    PermissionRequiredDialog(
                        activity = this,
                        textId = org.fossify.commons.R.string.allow_notifications_reminders,
                        positiveActionCallback = { openNotificationSettings() }
                    )
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    importEvents()
                }
            }
        }
    }

    private fun importEvents() {
        FilePickerDialog(this) {
            showImportEventsDialog(it) {}
        }
    }


    private fun tryExportEvents() {
        if (isQPlus()) {
            ExportEventsDialog(
                activity = this,
                path = config.lastExportPath,
                hidePath = true
            ) { file, calendars ->
                calendarsToExport = calendars
                hideKeyboard()

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, PICK_EVENTS_EXPORT_FILE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(
                            org.fossify.commons.R.string.system_service_disabled,
                            Toast.LENGTH_LONG
                        )
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) { granted ->
                if (granted) {
                    ExportEventsDialog(
                        activity = this,
                        path = config.lastExportPath,
                        hidePath = false
                    ) { file, calendars ->
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportEventsTo(calendars, it)
                        }
                    }
                }
            }
        }
    }

    private fun exportEventsTo(calendars: List<Long>, outputStream: OutputStream?) {
        ensureBackgroundThread {
            val events = eventsHelper.getEventsToExport(
                calendars = calendars,
                exportEvents = config.exportEvents,
                exportTasks = config.exportTasks,
                exportPastEntries = config.exportPastEntries
            )
            if (events.isEmpty()) {
                toast(org.fossify.commons.R.string.no_entries_for_exporting)
            } else {
                IcsExporter(this).exportEvents(outputStream, events, true) { result ->
                    toast(
                        when (result) {
                            IcsExporter.ExportResult.EXPORT_OK -> org.fossify.commons.R.string.exporting_successful
                            IcsExporter.ExportResult.EXPORT_PARTIAL -> org.fossify.commons.R.string.exporting_some_entries_failed
                            else -> org.fossify.commons.R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }
}
