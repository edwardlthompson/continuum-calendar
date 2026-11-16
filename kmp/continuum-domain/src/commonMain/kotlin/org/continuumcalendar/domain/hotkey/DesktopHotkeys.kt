package org.continuumcalendar.domain.hotkey

data class DesktopHotkeyEntry(val label: String, val hint: String)

val DESKTOP_HOTKEY_CATALOG = listOf(
    DesktopHotkeyEntry("New event", "N"),
    DesktopHotkeyEntry("Today", "T"),
    DesktopHotkeyEntry("Agenda view", "1"),
    DesktopHotkeyEntry("Week view", "2"),
    DesktopHotkeyEntry("Month view", "3"),
    DesktopHotkeyEntry("Year view", "4"),
    DesktopHotkeyEntry("Search events", "/ or F"),
    DesktopHotkeyEntry("Go to date", "G"),
    DesktopHotkeyEntry("Command palette", "Ctrl+K"),
)

enum class ShellHotkeyAction {
    NEW_EVENT,
    TODAY,
    VIEW_AGENDA,
    VIEW_WEEK,
    VIEW_MONTH,
    VIEW_YEAR,
    FOCUS_SEARCH,
    FOCUS_GOTO,
    COMMAND_PALETTE,
}

fun resolveShellHotkey(
    key: String,
    ctrl: Boolean,
    meta: Boolean,
    alt: Boolean,
    typingInField: Boolean,
): ShellHotkeyAction? {
    if (alt) return null
    val k = key.lowercase()
    if ((ctrl || meta) && k == "k") return ShellHotkeyAction.COMMAND_PALETTE
    if (typingInField) return null
    return when (k) {
        "n" -> ShellHotkeyAction.NEW_EVENT
        "t" -> ShellHotkeyAction.TODAY
        "1" -> ShellHotkeyAction.VIEW_AGENDA
        "2" -> ShellHotkeyAction.VIEW_WEEK
        "3" -> ShellHotkeyAction.VIEW_MONTH
        "4" -> ShellHotkeyAction.VIEW_YEAR
        "f", "/" -> ShellHotkeyAction.FOCUS_SEARCH
        "g" -> ShellHotkeyAction.FOCUS_GOTO
        else -> null
    }
}
