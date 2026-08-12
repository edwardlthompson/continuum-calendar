package org.fossify.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import org.fossify.calendar.R
import org.fossify.calendar.activities.SplashActivity
import org.fossify.calendar.extensions.launchNewEventOrTaskActivity
import org.fossify.calendar.extensions.widgetsDB
import org.fossify.calendar.services.WidgetService
import org.fossify.calendar.services.WidgetServiceEmpty
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.joda.time.DateTime

class MyWidgetListProvider : AppWidgetProvider() {
    private val NEW_EVENT = "new_event"
    private val LAUNCH_CAL = "launch_cal"
    private val GO_TO_TODAY = "go_to_today"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        // Match the in-app theme (not separate widget color prefs).
        val textColor = context.getProperTextColor()
        val bgColor = context.getProperBackgroundColor()
        val titleSizeSp =
            context.resources.getDimension(R.dimen.continuum_widget_title_text_size) /
                context.resources.displayMetrics.scaledDensity
        val metaSizeSp =
            context.resources.getDimension(R.dimen.continuum_widget_meta_text_size) /
                context.resources.displayMetrics.scaledDensity

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        ensureBackgroundThread {
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val widget = context.widgetsDB.getWidgetWithWidgetId(it)

                val headerVisibility = if (widget?.header == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                    applyColorFilter(R.id.widget_event_list_background, bgColor)
                    setTextColor(R.id.widget_event_list_empty, textColor)
                    setTextSize(R.id.widget_event_list_empty, metaSizeSp)

                    setImageViewResource(R.id.widget_event_list_logo, R.mipmap.ic_launcher)
                    setText(R.id.widget_event_list_today, context.getString(R.string.app_name))
                    setTextColor(R.id.widget_event_list_today, textColor)
                    setTextSize(R.id.widget_event_list_today, titleSizeSp)

                    setViewVisibility(R.id.widget_header_include, headerVisibility)
                }

                views.setImageViewBitmap(
                    R.id.widget_event_new_event, context.resources.getColoredBitmap(
                        resourceId = org.fossify.commons.R.drawable.ic_plus_vector,
                        newColor = textColor
                    )
                )
                setupIntent(context, views, NEW_EVENT, R.id.widget_event_new_event)
                setupIntent(context, views, LAUNCH_CAL, R.id.widget_event_list_today)

                views.setImageViewBitmap(R.id.widget_event_go_to_today, context.resources.getColoredBitmap(R.drawable.ic_today_vector, textColor))
                setupIntent(context, views, GO_TO_TODAY, R.id.widget_event_go_to_today)

                Intent(context, WidgetService::class.java).apply {
                    putExtra(EVENT_LIST_PERIOD, widget?.period)
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                    views.setRemoteAdapter(R.id.widget_event_list, this)
                }

                val startActivityIntent = context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)
                val startActivityPendingIntent =
                    PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                views.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)
                views.setEmptyView(R.id.widget_event_list, R.id.widget_event_list_empty)

                appWidgetManager.updateAppWidget(it, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_event_list)
            }
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetListProvider::class.java)

    private fun setupIntent(context: Context, views: RemoteViews, action: String, id: Int) {
        Intent(context, MyWidgetListProvider::class.java).apply {
            this.action = action
            val pendingIntent = PendingIntent.getBroadcast(context, 0, this, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NEW_EVENT -> context.launchNewEventOrTaskActivity()
            LAUNCH_CAL -> launchCalenderInDefaultView(context)
            GO_TO_TODAY -> goToToday(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        ensureBackgroundThread {
            appWidgetIds?.forEach {
                context?.widgetsDB?.deleteWidgetId(it)
            }
        }
    }

    private fun launchCalenderInDefaultView(context: Context) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            putExtra(DAY_CODE, Formatter.getDayCodeFromDateTime(DateTime()))
            // Continuum: widget title opens Agenda (event list), not Daily.
            putExtra(VIEW_TO_OPEN, EVENTS_LIST_VIEW)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }

    // hacky solution for reseting the events list
    private fun goToToday(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        ensureBackgroundThread {
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val widget = context.widgetsDB.getWidgetWithWidgetId(it)
                val headerVisibility = if (widget?.header == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                    setViewVisibility(R.id.widget_event_list_today, headerVisibility)
                    setViewVisibility(R.id.widget_event_go_to_today, headerVisibility)
                    setViewVisibility(R.id.widget_event_new_event, headerVisibility)
                }
                Intent(context, WidgetServiceEmpty::class.java).apply {
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                    views.setRemoteAdapter(R.id.widget_event_list, this)
                }

                appWidgetManager.updateAppWidget(it, views)
            }

            performUpdate(context)
        }
    }
}
