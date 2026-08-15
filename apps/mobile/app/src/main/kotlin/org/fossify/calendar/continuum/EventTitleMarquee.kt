package org.fossify.calendar.continuum

import android.widget.TextView

/**
 * In-app overflow ticker. Widgets stay on end-ellipsis (RemoteViews cannot tick).
 */
fun TextView.enableOverflowMarquee() {
    if (this is ContinuumTickerTextView) {
        restartTicker()
    }
}
