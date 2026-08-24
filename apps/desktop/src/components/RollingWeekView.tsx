import { useMemo, useRef } from 'react'
import { useEffect } from 'react'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import dayGridPlugin from '@fullcalendar/daygrid'
import multiMonthPlugin from '@fullcalendar/multimonth'
import interactionPlugin from '@fullcalendar/interaction'
import type { DateSelectArg, EventClickArg, EventContentArg, EventInput } from '@fullcalendar/core'
import { eventOccurrenceKey, type CalendarEvent, type CalendarListEntry } from '@continuum/shared'
import { fullCalendarTimeFormats } from '../utils/timeFormat'

function startOfDay(d: Date): Date {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  x.setMilliseconds(0)
  return x
}

/** Week start from [firstDayOfWeek] when rollingWeekFromToday is false. */
function weekAnchor(fromToday: boolean, firstDayOfWeek = 0, base = new Date()): Date {
  const d = startOfDay(base)
  if (fromToday) return d
  const day = d.getDay()
  const offset = (day - firstDayOfWeek + 7) % 7
  d.setDate(d.getDate() - offset)
  return d
}

interface RollingWeekViewProps {
  events: CalendarEvent[]
  calendars?: CalendarListEntry[]
  rollingWeekFromToday?: boolean
  redactTitles?: boolean
  use24HourFormat?: boolean
  firstDayOfWeek?: number
  weeklyViewDays?: number
  conflictIds?: Set<string>
  onSelectEvent?: (event: CalendarEvent) => void
  onSelectSlot?: (start: Date, end: Date) => void
  calendarView?: 'rollingWeek' | 'dayGridMonth' | 'multiMonthYear'
  focusDate?: string
  focusSeq?: number
  focusScrollTime?: string
}

export function RollingWeekView({
  events,
  calendars = [],
  rollingWeekFromToday = true,
  redactTitles = false,
  // Weekly grid always uses 24h labels for denser horizontal slot columns.
  use24HourFormat = true,
  firstDayOfWeek = 0,
  weeklyViewDays = 7,
  conflictIds,
  onSelectEvent,
  onSelectSlot,
  calendarView = 'rollingWeek',
  focusDate,
  focusSeq = 0,
  focusScrollTime,
}: RollingWeekViewProps) {
  const calendarRef = useRef<FullCalendar>(null)
  const today = useMemo(
    () => weekAnchor(rollingWeekFromToday, firstDayOfWeek),
    [rollingWeekFromToday, firstDayOfWeek],
  )
  const byId = useMemo(() => new Map(events.map((e) => [e.id, e])), [events])
  const colorByCal = useMemo(() => {
    const m = new Map<string, string>()
    for (const c of calendars) m.set(c.id, c.color)
    return m
  }, [calendars])
  const fcEvents = useMemo<EventInput[]>(
    () =>
      events.map((e) => {
        const calColor = colorByCal.get(e.calendarId)
        const conflict = conflictIds?.has(eventOccurrenceKey(e))
        return {
          id: e.id,
          title: redactTitles ? '••••••••' : e.title,
          start: e.start,
          end: e.end,
          allDay: e.allDay,
          backgroundColor: conflict ? '#ca8a04' : calColor,
          borderColor: conflict ? '#eab308' : calColor,
          classNames: conflict ? ['cc-conflict-event'] : undefined,
          extendedProps: { conflict: Boolean(conflict) },
        }
      }),
    [events, redactTitles, conflictIds, colorByCal],
  )

  function handleEventClick(arg: EventClickArg) {
    const ev = byId.get(arg.event.id)
    if (ev) onSelectEvent?.(ev)
  }

  function handleSelect(arg: DateSelectArg) {
    onSelectSlot?.(arg.start, arg.end)
  }

  function renderEventContent(arg: EventContentArg) {
    const conflict = Boolean(arg.event.extendedProps.conflict)
    return (
      <div className="fc-event-main-frame flex h-full min-h-0 items-start gap-0.5 overflow-hidden px-0.5">
        {conflict ? (
          <span
            className="cc-conflict-badge shrink-0 text-[11px] leading-none"
            title="Scheduling conflict"
            aria-label="Scheduling conflict"
          >
            ⚠️
          </span>
        ) : null}
        <div className="fc-event-title-container min-w-0 flex-1">
          <div className="fc-event-title fc-sticky-title truncate text-xs font-medium">
            {arg.event.title}
          </div>
          {arg.timeText ? (
            <div className="fc-event-time truncate text-[10px] opacity-90">{arg.timeText}</div>
          ) : null}
        </div>
      </div>
    )
  }

  const timeFormats = fullCalendarTimeFormats(use24HourFormat)

  useEffect(() => {
    const api = calendarRef.current?.getApi()
    if (!api) return
    if (calendarView && api.view.type !== calendarView) api.changeView(calendarView)
    if (focusDate) api.gotoDate(focusDate)
    if (focusScrollTime && calendarView === 'rollingWeek') api.scrollToTime(focusScrollTime)
  }, [calendarView, focusDate, focusScrollTime, focusSeq])

  return (
    <div className="h-full min-h-[28rem] rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-2 shadow-sm">
      <FullCalendar
        key={`${today.toISOString()}-${use24HourFormat}-${firstDayOfWeek}-${weeklyViewDays}`}
        ref={calendarRef}
        plugins={[timeGridPlugin, dayGridPlugin, multiMonthPlugin, interactionPlugin]}
        initialView={calendarView}
        headerToolbar={{
          left: 'prev,next today',
          center: 'title',
          right: '',
        }}
        buttonText={{
          today: 'Today',
          month: 'Month',
        }}
        views={{
          rollingWeek: {
            type: 'timeGrid',
            duration: { days: Math.min(14, Math.max(1, weeklyViewDays)) },
            buttonText: 'Rolling week',
            dayHeaders: true,
          },
          dayGridMonth: {
            buttonText: 'Month',
          },
          multiMonthYear: {
            type: 'multiMonthYear',
            buttonText: 'Year',
          },
        }}
        initialDate={today}
        firstDay={firstDayOfWeek}
        nowIndicator
        weekends
        allDaySlot
        slotMinTime="06:00:00"
        slotMaxTime="22:00:00"
        expandRows
        height="100%"
        events={fcEvents}
        editable
        selectable
        selectMirror
        dayMaxEvents={false}
        eventDisplay="block"
        eventTimeFormat={timeFormats.eventTimeFormat}
        slotLabelFormat={timeFormats.slotLabelFormat}
        eventClick={handleEventClick}
        select={handleSelect}
        eventContent={renderEventContent}
      />
    </div>
  )
}
