package org.continuumcalendar.domain.oauth

/** Desktop Sign in: Calendar + private Drive App Data (KB-028; omit Contacts/Tasks). */
object GoogleOAuthScopes {
    const val CALENDAR = "https://www.googleapis.com/auth/calendar"
    const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    const val TASKS = "https://www.googleapis.com/auth/tasks"

    val DESKTOP_SIGNIN: String = listOf(CALENDAR, DRIVE_APPDATA).joinToString(" ")

    fun desktopSignIn(includeTasks: Boolean): String =
        if (includeTasks) listOf(CALENDAR, DRIVE_APPDATA, TASKS).joinToString(" ")
        else DESKTOP_SIGNIN
}
