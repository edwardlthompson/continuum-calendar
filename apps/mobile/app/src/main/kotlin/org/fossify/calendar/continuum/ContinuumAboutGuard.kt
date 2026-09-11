package org.fossify.calendar.continuum

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

/** Commons About always shows hello@fossify.org; swap it for Continuum About. */
object ContinuumAboutGuard {
    const val COMMONS_ABOUT = "org.fossify.commons.activities.AboutActivity"

    fun shouldRedirect(className: String): Boolean = className == COMMONS_ABOUT

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(Redirect)
    }

    internal fun redirect(activity: Activity) {
        if (!shouldRedirect(activity.javaClass.name) || activity.isFinishing) {
            return
        }
        val next = Intent(activity, ContinuumAboutActivity::class.java)
        activity.intent.extras?.let { next.putExtras(it) }
        activity.startActivity(next)
        activity.finish()
    }

    private object Redirect : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            redirect(activity)
        }

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
