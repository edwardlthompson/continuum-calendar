package org.fossify.calendar.continuum

import android.content.Intent
import android.os.Bundle
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.databinding.ActivityContinuumAboutBinding
import org.fossify.commons.activities.FAQActivity
import org.fossify.commons.activities.LicenseActivity
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.APP_FAQ
import org.fossify.commons.helpers.APP_ICON_IDS
import org.fossify.commons.helpers.APP_LAUNCHER_NAME
import org.fossify.commons.helpers.APP_LICENSES
import org.fossify.commons.helpers.APP_NAME
import org.fossify.commons.helpers.APP_PACKAGE_NAME
import org.fossify.commons.helpers.APP_VERSION_NAME
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.FAQItem

/** About without Fossify email, social links, or Donate to Fossify. */
class ContinuumAboutActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityContinuumAboutBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.aboutNestedScrollview))
        setupMaterialScrollListener(binding.aboutNestedScrollview, binding.aboutAppbar)
        bindRows()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(topAppBar = binding.aboutAppbar, navigationIcon = NavigationIcon.Arrow)
        updateTextColors(binding.aboutHolder)
    }

    private fun bindRows() {
        val versionName = intent.getStringExtra(APP_VERSION_NAME) ?: ""
        binding.aboutVersion.text = getString(org.fossify.commons.R.string.version_placeholder, versionName)
        binding.aboutPackage.text = intent.getStringExtra(APP_PACKAGE_NAME) ?: packageName
        binding.aboutFaqHolder.setOnClickListener { launchFaq() }
        binding.aboutDonateHolder.setOnClickListener {
            launchViewIntent(ContinuumAboutLinks.DONATION_URL)
        }
        binding.aboutPrivacyHolder.setOnClickListener {
            launchViewIntent("https://github.com/edwardlthompson/continuum-calendar")
        }
        binding.aboutLicensesHolder.setOnClickListener { launchLicenses() }
    }

    private fun launchFaq() {
        val faqItems = intent.getSerializableExtra(APP_FAQ) as? ArrayList<FAQItem> ?: return
        startActivity(
            Intent(applicationContext, FAQActivity::class.java).apply {
                putExtra(APP_ICON_IDS, intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList<Int>())
                putExtra(APP_LAUNCHER_NAME, intent.getStringExtra(APP_LAUNCHER_NAME) ?: "")
                putExtra(APP_FAQ, faqItems)
            },
        )
    }

    private fun launchLicenses() {
        startActivity(
            Intent(applicationContext, LicenseActivity::class.java).apply {
                putExtra(APP_ICON_IDS, intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList<Int>())
                putExtra(APP_LAUNCHER_NAME, intent.getStringExtra(APP_LAUNCHER_NAME) ?: "")
                putExtra(APP_LICENSES, intent.getLongExtra(APP_LICENSES, 0))
            },
        )
    }

    companion object {
        fun start(
            activity: SimpleActivity,
            appNameId: Int,
            licenseMask: Long,
            versionName: String,
            faqItems: ArrayList<FAQItem>,
        ) {
            activity.hideKeyboard()
            activity.startActivity(
                Intent(activity.applicationContext, ContinuumAboutActivity::class.java).apply {
                    putExtra(APP_ICON_IDS, activity.getAppIconIDs())
                    putExtra(APP_LAUNCHER_NAME, activity.getAppLauncherName())
                    putExtra(APP_NAME, activity.getString(appNameId))
                    putExtra(APP_LICENSES, licenseMask)
                    putExtra(APP_VERSION_NAME, versionName)
                    putExtra(APP_PACKAGE_NAME, activity.baseConfig.appId)
                    putExtra(APP_FAQ, faqItems)
                },
            )
        }
    }
}
