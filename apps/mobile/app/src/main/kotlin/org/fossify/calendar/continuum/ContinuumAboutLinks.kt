package org.fossify.calendar.continuum

/** About menu must not promote Fossify social or donation destinations. */
object ContinuumAboutLinks {
    const val SHOW_DONATE = false
    const val SHOW_SOCIAL = false

    private val blocked = listOf(
        "github.com/fossifyorg",
        "reddit.com/r/fossify",
        "t.me/fossify",
        "fossify.org/donate",
    )

    fun isFossifyPromo(url: String): Boolean {
        val lower = url.lowercase()
        return blocked.any { lower.contains(it) }
    }
}
