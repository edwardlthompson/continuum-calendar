package org.continuumcalendar.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.continuumcalendar.domain.event.convertAllDayBounds
import org.continuumcalendar.domain.event.isoDatePart
import org.continuumcalendar.domain.event.isoTimePart
import org.continuumcalendar.domain.event.joinDateTime
import org.continuumcalendar.domain.model.CalendarAttendee
import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarListEntry
import org.continuumcalendar.domain.model.CalendarSource
import org.continuumcalendar.domain.model.EventReminder
import org.continuumcalendar.ui.theme.LocalContinuumPalette

data class EventDraft(
    val id: String? = null,
    val calendarId: String,
    val title: String = "",
    val start: String,
    val end: String,
    val allDay: Boolean = false,
    val location: String = "",
    val description: String = "",
    val etag: String? = null,
    val attendees: List<CalendarAttendee> = emptyList(),
    val reminders: List<EventReminder> = emptyList(),
    val recurrence: List<String> = emptyList(),
    val recurringEventId: String? = null,
    val timeZone: String? = null,
    val busy: Boolean = true,
    val visibility: String? = null,
    val readOnly: Boolean = false,
)

fun CalendarEvent.toDraft(): EventDraft = EventDraft(
    id = id,
    calendarId = calendarId,
    title = title,
    start = start,
    end = end,
    allDay = allDay,
    location = location.orEmpty(),
    description = description.orEmpty(),
    etag = etag,
    attendees = attendees,
    reminders = reminders,
    recurrence = recurrence,
    recurringEventId = recurringEventId,
    timeZone = timeZone,
    busy = busy,
    visibility = visibility,
    readOnly = readOnly,
)

fun EventDraft.toEvent(): CalendarEvent = CalendarEvent(
    id = id ?: "local-${start.hashCode()}-${title.hashCode()}",
    calendarId = calendarId,
    title = title.ifBlank { "(No title)" },
    description = description.ifBlank { null },
    location = location.ifBlank { null },
    start = start,
    end = end,
    allDay = allDay,
    attendees = attendees.filter { it.email.isNotBlank() },
    reminders = reminders.filter { it.minutes >= 0 },
    source = CalendarSource.GOOGLE,
    etag = etag,
    recurrence = recurrence,
    recurringEventId = recurringEventId,
    timeZone = timeZone,
    busy = busy,
    visibility = visibility,
    readOnly = readOnly,
)

@Composable
fun EventEditorSheet(
    draft: EventDraft,
    calendars: List<CalendarListEntry>,
    defaultWriteCalendarId: String = "",
    defaultReminderMinutes: Int = 10,
    onSave: (EventDraft) -> Unit,
    onDelete: ((EventDraft) -> Unit)?,
    onClose: () -> Unit,
) {
    val palette = LocalContinuumPalette.current
    var state by remember(draft.id, draft.start) {
        mutableStateOf(
            if (draft.id == null && draft.reminders.isEmpty()) {
                draft.copy(reminders = listOf(EventReminder(defaultReminderMinutes)))
            } else {
                draft
            },
        )
    }
    var guestDraft by remember { mutableStateOf("") }
    val writable = calendars.filter {
        it.writable && it.source != CalendarSource.HOLIDAYS
    }.ifEmpty { calendars.filter { it.writable } }.ifEmpty { calendars }

    Column(
        Modifier
            .width(360.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.id == null) "New event" else "Edit event",
                style = MaterialTheme.typography.titleMedium,
                color = palette.accent,
            )
            Spacer(Modifier.weight(1f))
            Text("Close", color = palette.muted, modifier = Modifier.clickable(onClick = onClose))
        }
        FieldLabel("Title")
        SimpleField(state.title) { state = state.copy(title = it) }
        FieldLabel("Calendar")
        writable.forEach { cal ->
            val on = state.calendarId == cal.id || state.calendarId == cal.logicalId
            val isDefault = cal.logicalId == defaultWriteCalendarId || cal.id == defaultWriteCalendarId
            Text(
                text = buildString {
                    append(cal.displayName)
                    if (isDefault) append(" ★")
                },
                color = if (on) palette.accent else palette.text,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { state = state.copy(calendarId = cal.id) }
                    .padding(8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (state.allDay) "☑ All day" else "☐ All day",
                color = palette.text,
                modifier = Modifier.clickable {
                    val next = !state.allDay
                    val bounds = convertAllDayBounds(state.start, state.end, state.allDay, next)
                    state = state.copy(start = bounds.start, end = bounds.end, allDay = bounds.allDay)
                },
            )
        }
        FieldLabel("Start date")
        SimpleField(isoDatePart(state.start)) { d ->
            state = state.copy(start = joinDateTime(d, isoTimePart(state.start), state.allDay))
        }
        if (!state.allDay) {
            FieldLabel("Start time (HH:MM)")
            SimpleField(isoTimePart(state.start)) { t ->
                state = state.copy(start = joinDateTime(isoDatePart(state.start), t, false))
            }
        }
        FieldLabel("End date")
        SimpleField(isoDatePart(state.end)) { d ->
            state = state.copy(end = joinDateTime(d, isoTimePart(state.end), state.allDay))
        }
        if (!state.allDay) {
            FieldLabel("End time (HH:MM)")
            SimpleField(isoTimePart(state.end)) { t ->
                state = state.copy(end = joinDateTime(isoDatePart(state.end), t, false))
            }
        }
        FieldLabel("Location")
        SimpleField(state.location) { state = state.copy(location = it) }
        FieldLabel("Description")
        SimpleField(state.description) { state = state.copy(description = it) }
        FieldLabel("Reminders (minutes before)")
        ReminderEditors(
            reminders = state.reminders,
            onChange = { state = state.copy(reminders = it) },
        )
        FieldLabel("Guests")
        state.attendees.forEachIndexed { idx, att ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    att.displayName?.takeIf { it.isNotBlank() }?.let { "$it <${att.email}>" } ?: att.email,
                    color = palette.text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Remove",
                    color = palette.now,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        state = state.copy(attendees = state.attendees.filterIndexed { i, _ -> i != idx })
                    },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleField(guestDraft, modifier = Modifier.weight(1f)) { guestDraft = it }
            Text(
                "Add",
                color = palette.accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    val email = guestDraft.trim()
                    if (email.contains('@') && state.attendees.none { it.email.equals(email, true) }) {
                        state = state.copy(attendees = state.attendees + CalendarAttendee(email = email))
                        guestDraft = ""
                    }
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onSave(state) }) {
                Text("Save", color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
            if (onDelete != null && state.id != null) {
                TextButton(onClick = { onDelete(state) }) {
                    Text("Delete", color = palette.now)
                }
            }
        }
    }
}

@Composable
private fun ReminderEditors(
    reminders: List<EventReminder>,
    onChange: (List<EventReminder>) -> Unit,
) {
    val palette = LocalContinuumPalette.current
    reminders.forEachIndexed { i, rem ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alert ${i + 1}", color = palette.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(56.dp))
            Text("−", color = palette.accent, modifier = Modifier.clickable {
                val next = reminders.toMutableList()
                next[i] = rem.copy(minutes = (rem.minutes - 5).coerceAtLeast(0))
                onChange(next)
            }.padding(4.dp))
            Text("${rem.minutes}", color = palette.text, fontWeight = FontWeight.SemiBold)
            Text("+", color = palette.accent, modifier = Modifier.clickable {
                val next = reminders.toMutableList()
                next[i] = rem.copy(minutes = (rem.minutes + 5).coerceAtMost(10080))
                onChange(next)
            }.padding(4.dp))
            Text(
                "Remove",
                color = palette.now,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    onChange(reminders.filterIndexed { idx, _ -> idx != i })
                },
            )
        }
    }
    if (reminders.size < 3) {
        Text(
            "Add reminder",
            color = palette.accent,
            modifier = Modifier.clickable { onChange(reminders + EventReminder(10)) },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalContinuumPalette.current.muted)
}

@Composable
private fun SimpleField(
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    val palette = LocalContinuumPalette.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = false,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.background)
            .border(1.dp, palette.border, RoundedCornerShape(6.dp))
            .padding(8.dp),
    )
}
