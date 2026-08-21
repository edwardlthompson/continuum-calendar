package org.fossify.calendar.continuum

/** About menu: Continuum donate only — never Fossify social or donation destinations. */
object ContinuumAboutLinks {
    const val SHOW_DONATE = true
    const val SHOW_SOCIAL = false
    const val DONATION_URL = "https://venmo.com/code?user_id=1857304970395648420"

    private val blocked = listOf(
        "github.com/fossifyorg",
        "reddit.com/r/fossify",
        "t.me/fossify",
        "fossify.org/donate",
        "hello@fossify.org",
    )

    fun isFossifyPromo(url: String): Boolean {
        val lower = url.lowercase()
        return blocked.any { lower.contains(it) }
    }
}
