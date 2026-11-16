package org.continuumcalendar.data

import java.io.File
import java.time.Instant

data class PendingCrash(
    val message: String,
    val stack: String,
    val atIso: String,
)

/** Local-only crash capture (never auto-sends). */
object DesktopCrashCapture {
    const val APP_ID = "org.continuumcalendar.app"
    const val FILE_NAME = "continuum-last-crash.txt"

    fun crashFile(): File {
        val appData = System.getenv("APPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming")
        return File(File(appData, APP_ID), FILE_NAME)
    }

    fun installDefaultHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(error: Throwable) {
        val file = crashFile()
        file.parentFile?.mkdirs()
        val stack = error.stackTraceToString().take(12_000)
        val body = buildString {
            appendLine(Instant.now().toString())
            appendLine(error::class.qualifiedName ?: "Throwable")
            appendLine(error.message ?: "(no message)")
            appendLine("---")
            append(stack)
        }
        file.writeText(body, Charsets.UTF_8)
    }

    fun readPending(): PendingCrash? {
        val file = crashFile()
        if (!file.isFile) return null
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull() ?: return null
        if (lines.size < 3) return null
        val at = lines[0]
        val message = lines[2]
        val stack = lines.drop(4).joinToString("\n")
        return PendingCrash(message = message, stack = stack, atIso = at)
    }

    fun clear() {
        runCatching { crashFile().delete() }
    }

    fun feedbackIssueUrl(crash: PendingCrash?): String {
        val title = if (crash != null) {
            "[crash] Continuum Calendar CMP"
        } else {
            "Continuum Calendar feedback"
        }
        val body = buildString {
            appendLine("## What happened")
            appendLine()
            if (crash != null) {
                appendLine(crash.message.take(500))
                appendLine()
                appendLine("```")
                appendLine(crash.stack.take(1500))
                appendLine("```")
            } else {
                appendLine("(describe the issue)")
            }
        }
        val q = java.net.URLEncoder.encode(title, Charsets.UTF_8)
        val b = java.net.URLEncoder.encode(body, Charsets.UTF_8)
        return "https://github.com/edwardlthompson/continuum-calendar/issues/new?title=$q&body=$b"
    }
}
