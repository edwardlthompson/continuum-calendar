package org.continuumcalendar.data

import org.continuumcalendar.domain.model.CalendarEvent
import org.continuumcalendar.domain.model.CalendarSource

class SqlDelightEventRepository(
    private val db: ContinuumDatabase,
) : EventRepository {
    override suspend fun listAll(): List<CalendarEvent> =
        db.continuumQueries.selectAll().executeAsList().map { it.toDomain() }

    override suspend fun upsert(event: CalendarEvent) {
        db.continuumQueries.upsert(
            id = event.id,
            calendarId = event.calendarId,
            title = event.title,
            description = event.description,
            location = event.location,
            startIso = event.start,
            endIso = event.end,
            allDay = if (event.allDay) 1L else 0L,
            source = event.source?.wire(),
            updatedIso = event.updated,
        )
    }

    override suspend fun delete(calendarId: String, id: String) {
        db.continuumQueries.deleteByKey(calendarId, id)
    }

    override suspend fun replaceAll(events: List<CalendarEvent>) {
        db.transaction {
            db.continuumQueries.deleteAll()
            events.forEach { event ->
                db.continuumQueries.upsert(
                    id = event.id,
                    calendarId = event.calendarId,
                    title = event.title,
                    description = event.description,
                    location = event.location,
                    startIso = event.start,
                    endIso = event.end,
                    allDay = if (event.allDay) 1L else 0L,
                    source = event.source?.wire(),
                    updatedIso = event.updated,
                )
            }
        }
    }
}

fun CalendarEventEntity.toDomain(): CalendarEvent =
    CalendarEvent(
        id = id,
        calendarId = calendarId,
        title = title,
        description = description,
        location = location,
        start = startIso,
        end = endIso,
        allDay = allDay != 0L,
        source = source?.let { CalendarSource.fromWire(it) },
        updated = updatedIso,
    )
