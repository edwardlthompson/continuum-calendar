package org.continuumcalendar.domain.sync

import org.continuumcalendar.domain.model.ContinuumSettings
import org.continuumcalendar.domain.model.WorkingHours

enum class PeerReconcileAction { SEED, PULL, PUSH_PENDING, NOOP }

fun decidePeerReconcile(
    hasRemote: Boolean,
    remoteRevision: Long,
    localRevision: Long,
    pendingLocalPush: Boolean,
): PeerReconcileAction {
    if (!hasRemote) return PeerReconcileAction.SEED
    if (pendingLocalPush || localRevision > remoteRevision) return PeerReconcileAction.PUSH_PENDING
    if (remoteRevision > localRevision) return PeerReconcileAction.PULL
    return PeerReconcileAction.NOOP
}

fun shouldApplyRemote(remoteRevision: Long, lastAppliedRevision: Long): Boolean =
    remoteRevision > lastAppliedRevision

fun mergeSettings(base: ContinuumSettings, patch: ContinuumSettingsPatch): ContinuumSettings =
    ContinuumSettings(
        themeMode = patch.themeMode ?: base.themeMode,
        showEmptyDaysInAgenda = patch.showEmptyDaysInAgenda ?: base.showEmptyDaysInAgenda,
        rollingWeekFromToday = patch.rollingWeekFromToday ?: base.rollingWeekFromToday,
        weeklyViewDays = patch.weeklyViewDays ?: base.weeklyViewDays,
        workingHours = patch.workingHours ?: base.workingHours,
        defaultReminderMinutes = patch.defaultReminderMinutes ?: base.defaultReminderMinutes,
        notificationEnabled = patch.notificationEnabled ?: base.notificationEnabled,
        defaultSnoozeMinutes = patch.defaultSnoozeMinutes ?: base.defaultSnoozeMinutes,
        visibleCalendarIds = patch.visibleCalendarIds ?: base.visibleCalendarIds,
        defaultWriteCalendarId = patch.defaultWriteCalendarId ?: base.defaultWriteCalendarId,
        agendaRangeDays = patch.agendaRangeDays ?: base.agendaRangeDays,
        slotMinMinutes = patch.slotMinMinutes ?: base.slotMinMinutes,
        redactTitlesInScreenshots = patch.redactTitlesInScreenshots ?: base.redactTitlesInScreenshots,
        travelBufferMinutes = patch.travelBufferMinutes ?: base.travelBufferMinutes,
        agendaDensity = patch.agendaDensity ?: base.agendaDensity,
        secondaryTimeZone = if (patch.clearSecondaryTimeZone) null
        else patch.secondaryTimeZone ?: base.secondaryTimeZone,
        showContactBirthdays = patch.showContactBirthdays ?: base.showContactBirthdays,
        use24HourFormat = patch.use24HourFormat ?: base.use24HourFormat,
        firstDayOfWeek = patch.firstDayOfWeek ?: base.firstDayOfWeek,
        useGoogleCalendar = patch.useGoogleCalendar ?: base.useGoogleCalendar,
        calendarNotifyPrefs = patch.calendarNotifyPrefs ?: base.calendarNotifyPrefs,
        weekHourDisplay = patch.weekHourDisplay ?: base.weekHourDisplay,
        holidayPack = patch.holidayPack ?: base.holidayPack,
        showGoogleTasks = patch.showGoogleTasks ?: base.showGoogleTasks,
    )

/** Sparse settings patch (null = leave unchanged). */
data class ContinuumSettingsPatch(
    val themeMode: String? = null,
    val showEmptyDaysInAgenda: Boolean? = null,
    val rollingWeekFromToday: Boolean? = null,
    val weeklyViewDays: Int? = null,
    val workingHours: WorkingHours? = null,
    val defaultReminderMinutes: Int? = null,
    val notificationEnabled: Boolean? = null,
    val defaultSnoozeMinutes: Int? = null,
    val visibleCalendarIds: List<String>? = null,
    val defaultWriteCalendarId: String? = null,
    val agendaRangeDays: Int? = null,
    val slotMinMinutes: Int? = null,
    val redactTitlesInScreenshots: Boolean? = null,
    val travelBufferMinutes: Int? = null,
    val agendaDensity: String? = null,
    val secondaryTimeZone: String? = null,
    val clearSecondaryTimeZone: Boolean = false,
    val showContactBirthdays: Boolean? = null,
    val use24HourFormat: Boolean? = null,
    val firstDayOfWeek: Int? = null,
    val useGoogleCalendar: Boolean? = null,
    val calendarNotifyPrefs: Map<String, org.continuumcalendar.domain.model.CalendarNotifyPrefs>? = null,
    val weekHourDisplay: String? = null,
    val holidayPack: String? = null,
    val showGoogleTasks: Boolean? = null,
)
