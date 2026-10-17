import { useMemo, useRef } from 'react'
import { useEffect } from 'react'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import dayGridPlugin from '@fullcalendar/daygrid'
import multiMonthPlugin from '@fullcalendar/multimonth'
import interactionPlugin from '@fullcalendar/interaction'
import type { DateSelectArg, EventClickArg, EventContentArg, EventInput } from '@fullcalendar/core'
import {
  eventOccurrenceKey,
  type CalendarEvent,
  type CalendarListEntry,
  type WeekHourDisplay,
} from '@continuum/shared'
import { fullCalendarTimeFormats } from '../utils/timeFormat'
import {
  fullDaySlotWindow,
  isGridEventEditable,
  weekScrollFocusTime,
  type GridMoveArg,
} from '../grid/gridMove'
import { weekAnchor } from './weekNav'

interface RollingWeekViewProps {
  events: CalendarEvent[]
  calendars?: CalendarListEntry[]
  rollingWeekFromToday?: boolean
  redactTitles?: boolean
  use24HourFormat?: boolean
  firstDayOfWeek?: number
  weeklyViewDays?: number
  conflictIds?: Set<string>
  /** Used only for initial scroll focus in scroll mode (not slot clipping). */
  workingHours?: { start?: string; end?: string }
  weekHourDisplay?: WeekHourDisplay
  moveBusy?: boolean
  onSelectEvent?: (event: CalendarEvent) => void
  onSelectSlot?: (start: Date, end: Date) => void
  onMoveEvent?: (event: CalendarEvent, arg: GridMoveArg, revert: () => void) => void
  calendarView?: 'rollingWeek' | 'dayGridMonth' | 'multiMonthYear'
  focusDate?: string
  focusSeq?: number
  focusScrollTime?: string
  /** Shift the rolling week by ±1 day (arrows beside the date-range title). */
  onShiftDays?: (deltaDays: number) => void
}

/** Hourly slots in fit24 so 24 rows fill the pane without a forced CSS height. */
const FIT_SLOT_DURATION = '01:00:00'
const SCROLL_SLOT_DURATION = '00:30:00'

export function RollingWeekView({
  events,
  calendars = [],
  rollingWeekFromToday = true,
  redactTitles = false,
  use24HourFormat = false,
  firstDayOfWeek = 0,
  weeklyViewDays = 7,
  conflictIds,
  workingHours,
  weekHourDisplay = 'scroll',
  moveBusy = false,
  onSelectEvent,
  onSelectSlot,
  onMoveEvent,
  calendarView = 'rollingWeek',
  focusDate,
  focusSeq = 0,
  focusScrollTime,
  onShiftDays,
}: RollingWeekViewProps) {
  const calendarRef = useRef<FullCalendar>(null)
  const onShiftDaysRef = useRef(onShiftDays)
  onShiftDaysRef.current = onShiftDays
  const today = useMemo(
    () => weekAnchor(rollingWeekFromToday, firstDayOfWeek),
    [rollingWeekFromToday, firstDayOfWeek],
  )
  const weekNavButtons = useMemo(
    () => ({
      prevDay: {
        text: '‹',
        hint: 'Previous day',
        click: () => onShiftDaysRef.current?.(-1),
      },
      nextDay: {
        text: '›',
        hint: 'Next day',
        click: () => onShiftDaysRef.current?.(1),
      },
    }),
    [],
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
        const editable = !moveBusy && isGridEventEditable(e, calendars)
        return {
          id: e.id,
          title: redactTitles ? '••••••••' : e.title,
          start: e.start,
          end: e.end,
          allDay: e.allDay,
          backgroundColor: conflict ? '#ca8a04' : calColor,
          borderColor: conflict ? '#eab308' : calColor,
          classNames: conflict ? ['cc-conflict-event'] : undefined,
          editable,
          startEditable: editable,
          durationEditable: editable,
          extendedProps: { conflict: Boolean(conflict) },
        }
      }),
    [events, redactTitles, conflictIds, colorByCal, calendars, moveBusy],
  )
  const slotWindow = useMemo(() => fullDaySlotWindow(), [])
  const scrollMode = weekHourDisplay === 'scroll'
  const defaultScrollTime = useMemo(
    () => weekScrollFocusTime(workingHours),
    [workingHours],
  )

  function handleEventClick(arg: EventClickArg) {
    const ev = byId.get(arg.event.id)
    if (ev) onSelectEvent?.(ev)
  }

  function handleSelect(arg: DateSelectArg) {
    onSelectSlot?.(arg.start, arg.end)
  }

  function handleMove(info: {
    event: {
      id: string
      start: Date | null
      end: Date | null
      allDay: boolean
      startStr: string
      endStr: string
    }
    revert: () => void
  }) {
    try {
      const ev = byId.get(info.event.id)
      if (!ev) {
        info.revert()
        return
      }
      onMoveEvent?.(
        ev,
        {
          eventId: info.event.id,
          start: info.event.start,
          end: info.event.end,
          allDay: info.event.allDay,
          startStr: info.event.startStr,
          endStr: info.event.endStr,
        },
        info.revert,
      )
    } catch {
      info.revert()
    }
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
  const slotDuration = scrollMode ? SCROLL_SLOT_DURATION : FIT_SLOT_DURATION
  // Fit 24h: expandRows sizes the slots table to the liquid pane on every layout/resize.
  // Scroll: fixed CSS row height; expandRows off so the grid scrolls.
  const expandRows = !scrollMode

  useEffect(() => {
    const api = calendarRef.current?.getApi()
    if (!api) return
    if (calendarView && api.view.type !== calendarView) api.changeView(calendarView)
    if (focusDate) api.gotoDate(focusDate)
    if (calendarView === 'rollingWeek' && scrollMode) {
      api.scrollToTime(focusScrollTime || defaultScrollTime)
    }
  }, [calendarView, focusDate, focusScrollTime, focusSeq, scrollMode, defaultScrollTime])

  useEffect(() => {
    const api = calendarRef.current?.getApi()
    if (!api || calendarView !== 'rollingWeek') return
    api.setOption('slotDuration', slotDuration)
    api.setOption('expandRows', expandRows)
  }, [slotDuration, expandRows, calendarView])

  return (
    <div
      className={`h-full min-h-0 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-2 shadow-sm ${
        scrollMode ? 'cc-week-scroll' : 'cc-week-fit24'
      }`}
    >
      <FullCalendar
        key={`${today.toISOString()}-${use24HourFormat}-${firstDayOfWeek}-${weeklyViewDays}-${weekHourDisplay}-${slotWindow.slotMinTime}-${slotWindow.slotMaxTime}-${slotDuration}`}
        ref={calendarRef}
        plugins={[timeGridPlugin, dayGridPlugin, multiMonthPlugin, interactionPlugin]}
        initialView={calendarView}
        headerToolbar={
          calendarView === 'rollingWeek'
            ? { left: 'prevDay', center: 'title', right: 'nextDay' }
            : { left: '', center: 'title', right: '' }
        }
        customButtons={calendarView === 'rollingWeek' ? weekNavButtons : undefined}
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
        initialDate={focusDate || today}
        firstDay={firstDayOfWeek}
        nowIndicator
        weekends
        allDaySlot
        slotMinTime={slotWindow.slotMinTime}
        slotMaxTime={slotWindow.slotMaxTime}
        slotDuration={slotDuration}
        expandRows={expandRows}
        height="100%"
        events={fcEvents}
        editable={!moveBusy}
        selectable
        selectMirror
        dayMaxEvents={false}
        eventDisplay="block"
        eventTimeFormat={timeFormats.eventTimeFormat}
        slotLabelFormat={timeFormats.slotLabelFormat}
        eventClick={handleEventClick}
        select={handleSelect}
        eventDrop={handleMove}
        eventResize={handleMove}
        eventContent={renderEventContent}
      />
    </div>
  )
}
