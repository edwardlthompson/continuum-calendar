package org.fossify.calendar.continuum

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager

/**
 * Request the display's fastest same-resolution mode and mark fling surfaces
 * as high-refresh so adaptive (LTPO/VRR) panels can ramp during scrolls.
 */
object HighRefreshDisplay {
    data class ModeSpec(val modeId: Int, val width: Int, val height: Int, val refreshRate: Float)

    fun fastestSameResolutionModeId(current: ModeSpec, supported: List<ModeSpec>): Int {
        return supported
            .filter { it.width == current.width && it.height == current.height }
            .maxByOrNull { it.refreshRate }
            ?.modeId
            ?: current.modeId
    }

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(LifecycleHooks)
    }

    fun applyToWindow(activity: Activity) {
        val display = activityDisplay(activity) ?: return
        val current = display.mode
        val fastestId = fastestSameResolutionModeId(
            ModeSpec(current.modeId, current.physicalWidth, current.physicalHeight, current.refreshRate),
            display.supportedModes.map {
                ModeSpec(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate)
            },
        )
        val params = activity.window.attributes
        if (params.preferredDisplayModeId != fastestId) {
            params.preferredDisplayModeId = fastestId
            activity.window.attributes = params
        }
    }

    fun markHighRefresh(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
        }
    }

    fun markScrollSurfaces(root: View) {
        if (isScrollSurface(root)) {
            markHighRefresh(root)
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                markScrollSurfaces(root.getChildAt(i))
            }
        }
    }

    internal fun isScrollSurface(view: View): Boolean {
        return view is ScrollView ||
            view is HorizontalScrollView ||
            view is NestedScrollView ||
            view is AbsListView ||
            view is RecyclerView ||
            view is ViewPager ||
            view is WebView
    }

    private fun activityDisplay(activity: Activity) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay
    }

    private val fragmentViews = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            fragment: Fragment,
            view: View,
            savedInstanceState: Bundle?,
        ) {
            markScrollSurfaces(view)
        }
    }

    private object LifecycleHooks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            applyToWindow(activity)
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentViews, true)
            }
        }

        override fun onActivityStarted(activity: Activity) = applyToWindow(activity)

        override fun onActivityResumed(activity: Activity) {
            activity.findViewById<View>(android.R.id.content)?.let { markScrollSurfaces(it) }
        }

        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
