package org.fossify.calendar.continuum

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SENDTO
import android.content.Intent.EXTRA_EMAIL
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.createChooser
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.databinding.ActivityContinuumAboutBinding
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.activities.FAQActivity
import org.fossify.commons.activities.LicenseActivity
import org.fossify.commons.dialogs.ConfirmationAdvancedDialog
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
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
import org.fossify.commons.helpers.SHOW_FAQ_BEFORE_MAIL
import org.fossify.commons.models.FAQItem

/** About without Fossify social links or Donate to Fossify. */
class ContinuumAboutActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityContinuumAboutBinding::inflate)
    private val appName get() = intent.getStringExtra(APP_NAME) ?: ""

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
        binding.aboutEmailHolder.setOnClickListener { onEmailClick() }
        binding.aboutPrivacyHolder.setOnClickListener {
            launchViewIntent("https://github.com/edwardlthompson/continuum-calendar")
        }
        binding.aboutLicensesHolder.setOnClickListener { launchLicenses() }
    }

    private fun onEmailClick() {
        if (intent.getBooleanExtra(SHOW_FAQ_BEFORE_MAIL, false) && !baseConfig.wasBeforeAskingShown) {
            baseConfig.wasBeforeAskingShown = true
            ConfirmationAdvancedDialog(
                activity = this,
                message = "${getString(org.fossify.commons.R.string.before_asking_question_read_faq)}\n\n${getString(org.fossify.commons.R.string.make_sure_latest)}",
                messageId = 0,
                positive = org.fossify.commons.R.string.read_faq,
                negative = org.fossify.commons.R.string.skip,
            ) { readFaq ->
                if (readFaq) launchFaq() else launchEmail()
            }
        } else {
            launchEmail()
        }
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

    private fun launchEmail() {
        val body = "${getString(org.fossify.commons.R.string.app_version, intent.getStringExtra(APP_VERSION_NAME))}\n" +
            "${getString(org.fossify.commons.R.string.device_os, Build.VERSION.RELEASE)}\n------------------------------\n\n"
        val address = getString(org.fossify.commons.R.string.my_fake_email)
        val selector = Intent(ACTION_SENDTO).setData("mailto:$address".toUri())
        val email = Intent(ACTION_SEND).apply {
            putExtra(EXTRA_EMAIL, arrayOf(address))
            putExtra(EXTRA_SUBJECT, appName)
            putExtra(EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.selector = selector
        }
        try {
            startActivity(email)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(createChooser(email, getString(org.fossify.commons.R.string.send_email)))
            } catch (_: Exception) {
                toast(org.fossify.commons.R.string.no_email_client_found)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    companion object {
        fun start(
            activity: SimpleActivity,
            appNameId: Int,
            licenseMask: Long,
            versionName: String,
            faqItems: ArrayList<FAQItem>,
            showFAQBeforeMail: Boolean,
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
                    putExtra(SHOW_FAQ_BEFORE_MAIL, showFAQBeforeMail)
                },
            )
        }
    }
}
