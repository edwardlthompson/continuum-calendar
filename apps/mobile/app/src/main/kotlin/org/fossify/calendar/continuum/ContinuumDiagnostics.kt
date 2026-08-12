package org.fossify.calendar.continuum

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * FOSS-friendly local crash / error logging. Writes under app filesDir — no telemetry.
 */
object ContinuumDiagnostics {
    private const val TAG = "ContinuumDiag"
    private const val LOG_FILE = "continuum-errors.log"
    private const val CRASH_MARKER = "continuum-last-crash.txt"
    private const val MAX_BYTES = 256 * 1024
    private val io = Executors.newSingleThreadExecutor()

    @Volatile
    private var appContext: Context? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        if (previousHandler != null) return
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(throwable, "uncaught on ${thread.name}")
            } catch (_: Exception) {
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
        e("ContinuumDiagnostics installed")
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
            append("ERROR", message, throwable)
        } else {
            Log.e(TAG, message)
            append("ERROR", message, null)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
            append("WARN", message, throwable)
        } else {
            Log.w(TAG, message)
            append("WARN", message, null)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
        append("INFO", message, null)
    }

    fun lastCrashSummary(context: Context): String? {
        val f = File(context.filesDir, CRASH_MARKER)
        if (!f.exists()) return null
        return try {
            f.readText().take(4000)
        } catch (_: Exception) {
            null
        }
    }

    fun clearCrashMarker(context: Context) {
        try {
            File(context.filesDir, CRASH_MARKER).delete()
        } catch (_: Exception) {
        }
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE)

    private fun recordCrash(throwable: Throwable, note: String) {
        val ctx = appContext ?: return
        val stamp = stamp()
        val body = buildString {
            appendLine("=== CRASH $stamp ===")
            appendLine(note)
            appendLine(deviceFooter())
            appendLine(stack(throwable))
        }
        try {
            File(ctx.filesDir, CRASH_MARKER).writeText(body)
        } catch (_: Exception) {
        }
        append("CRASH", note, throwable)
    }

    private fun append(level: String, message: String, throwable: Throwable?) {
        val ctx = appContext ?: return
        val line = buildString {
            append(stamp())
            append(' ')
            append(level)
            append(' ')
            append(message)
            if (throwable != null) {
                append('\n')
                append(stack(throwable))
            }
            append('\n')
        }
        io.execute {
            try {
                val file = File(ctx.filesDir, LOG_FILE)
                if (file.exists() && file.length() > MAX_BYTES) {
                    val trimmed = file.readText().takeLast(MAX_BYTES / 2)
                    file.writeText("…truncated…\n$trimmed")
                }
                file.appendText(line)
            } catch (_: Exception) {
            }
        }
    }

    private fun stack(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun deviceFooter(): String =
        "sdk=${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL} " +
            "app=${appContext?.packageName}"
}
