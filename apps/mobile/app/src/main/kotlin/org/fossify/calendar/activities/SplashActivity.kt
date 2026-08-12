package org.fossify.calendar.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fossify.calendar.extensions.getNewEventTimestampFromCode
import org.fossify.calendar.helpers.*
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.isAutoTheme
import org.fossify.commons.extensions.isSystemInDarkMode
import org.fossify.commons.extensions.syncGlobalConfig
import org.fossify.commons.helpers.SIDELOADING_FALSE
import org.fossify.commons.R as CommonsR
import org.joda.time.DateTime

/**
 * Continuum splash — same routing as Fossify BaseSplashActivity, but skips the
 * commons "fake version / fossify.org" anti-repackaging dialog (we ship a renamed package).
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseConfig.appSideloadingStatus = SIDELOADING_FALSE

        syncGlobalConfig {
            baseConfig.apply {
                if (isAutoTheme()) {
                    val isUsingSystemDarkTheme = isSystemInDarkMode()
                    textColor = resources.getColor(
                        if (isUsingSystemDarkTheme) CommonsR.color.theme_dark_text_color
                        else CommonsR.color.theme_light_text_color
                    )
                    backgroundColor = resources.getColor(
                        if (isUsingSystemDarkTheme) CommonsR.color.theme_dark_background_color
                        else CommonsR.color.theme_light_background_color
                    )
                }
            }
            // Hold neon splash briefly so the marketing mark is visible on cold start.
            window.decorView.postDelayed({ initActivity() }, 900L)
        }
    }

    private fun initActivity() {
        when {
            intent.extras?.containsKey(DAY_CODE) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                putExtra(VIEW_TO_OPEN, intent.getIntExtra(VIEW_TO_OPEN, LAST_VIEW))
                startActivity(this)
            }

            intent.extras?.containsKey(EVENT_ID) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(EVENT_ID, intent.getLongExtra(EVENT_ID, 0L))
                putExtra(EVENT_OCCURRENCE_TS, intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L))
                putExtra(IS_TASK, intent.getBooleanExtra(IS_TASK, false))
                startActivity(this)
            }

            intent.action == SHORTCUT_NEW_EVENT -> {
                val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                Intent(this, EventActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                    startActivity(this)
                }
            }

            intent.action == SHORTCUT_NEW_TASK -> {
                val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                Intent(this, TaskActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                    startActivity(this)
                }
            }

            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
