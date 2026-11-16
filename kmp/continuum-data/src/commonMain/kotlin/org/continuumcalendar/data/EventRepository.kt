package org.continuumcalendar.data

import org.continuumcalendar.domain.model.CalendarEvent

interface EventRepository {
    suspend fun listAll(): List<CalendarEvent>
    suspend fun upsert(event: CalendarEvent)
    suspend fun delete(calendarId: String, id: String)
    suspend fun replaceAll(events: List<CalendarEvent>)
}

class InMemoryEventRepository : EventRepository {
    private val map = linkedMapOf<String, CalendarEvent>()

    private fun key(calendarId: String, id: String) = "$calendarId:$id"

    override suspend fun listAll(): List<CalendarEvent> =
        map.values.sortedWith(compareBy({ it.start }, { it.end }))

    override suspend fun upsert(event: CalendarEvent) {
        map[key(event.calendarId, event.id)] = event
    }

    override suspend fun delete(calendarId: String, id: String) {
        map.remove(key(calendarId, id))
    }

    override suspend fun replaceAll(events: List<CalendarEvent>) {
        map.clear()
        events.forEach { upsert(it) }
    }
}
