package org.fossify.calendar.continuum

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import org.fossify.calendar.R
import org.fossify.calendar.activities.SplashActivity
import org.fossify.calendar.extensions.config
import org.fossify.calendar.extensions.eventsHelper
import org.fossify.calendar.helpers.DAILY_VIEW
import org.fossify.calendar.helpers.DAY_CODE
import org.fossify.calendar.helpers.Formatter
import org.fossify.calendar.helpers.VIEW_TO_OPEN
import org.fossify.commons.extensions.getLaunchIntent
import org.joda.time.DateTime

class RollingWeekWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED ->
                performUpdate(context)
            else -> super.onReceive(context, intent)
        }
    }

    private fun performUpdate(context: Context) {
        val codes = RollingWeek.dayCodes(
            Formatter.getDayCodeFromDateTime(DateTime.now()),
            count = 7,
            rolling = context.config.rollingWeekWidget,
            isoWeekStart = context.config.firstDayOfWeek,
        )
        if (codes.isEmpty()) return
        val fromTs = Formatter.getDayStartTS(codes.first())
        val toTs = Formatter.getDayEndTS(codes.last())
        context.eventsHelper.getEvents(fromTs, toTs) { events ->
            val counts = IntArray(codes.size)
            for (event in events) {
                val code = Formatter.getDayCodeFromTS(event.startTS)
                val idx = codes.indexOf(code)
                if (idx >= 0) counts[idx]++
            }
            val mgr = AppWidgetManager.getInstance(context) ?: return@getEvents
            val color = context.config.widgetTextColor
            val todayCode = Formatter.getDayCodeFromDateTime(DateTime.now())
            val openLabel = context.getString(R.string.continuum_open_day)
            mgr.getAppWidgetIds(ComponentName(context, RollingWeekWidgetProvider::class.java)).forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_rolling_week)
                codes.forEachIndexed { i, code ->
                    val dt = Formatter.getLocalDateTimeFromCode(code)
                    views.setTextViewText(LABEL_IDS[i], dt.dayOfWeek().asShortText)
                    views.setTextColor(LABEL_IDS[i], color)
                    val countLabel = RollingWeek.cellLabel(counts[i], openLabel)
                    views.setTextViewText(COUNT_IDS[i], countLabel)
                    views.setTextViewTextSize(
                        COUNT_IDS[i],
                        TypedValue.COMPLEX_UNIT_SP,
                        RollingWeek.cellCountSizeSp(countLabel),
                    )
                    views.setTextColor(COUNT_IDS[i], color)
                    views.setInt(
                        COL_IDS[i],
                        "setBackgroundResource",
                        if (code == todayCode) R.drawable.widget_week_today else 0,
                    )
                    val open = (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
                        putExtra(DAY_CODE, code)
                        putExtra(VIEW_TO_OPEN, DAILY_VIEW)
                    }
                    views.setOnClickPendingIntent(
                        COL_IDS[i],
                        PendingIntent.getActivity(
                            context,
                            code.toInt(),
                            open,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
                mgr.updateAppWidget(id, views)
            }
        }
    }

    companion object {
        private val COL_IDS = intArrayOf(
            R.id.week_col_0, R.id.week_col_1, R.id.week_col_2, R.id.week_col_3,
            R.id.week_col_4, R.id.week_col_5, R.id.week_col_6,
        )
        private val LABEL_IDS = intArrayOf(
            R.id.week_label_0, R.id.week_label_1, R.id.week_label_2, R.id.week_label_3,
            R.id.week_label_4, R.id.week_label_5, R.id.week_label_6,
        )
        private val COUNT_IDS = intArrayOf(
            R.id.week_count_0, R.id.week_count_1, R.id.week_count_2, R.id.week_count_3,
            R.id.week_count_4, R.id.week_count_5, R.id.week_count_6,
        )
    }
}
