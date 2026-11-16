package org.continuumcalendar.data.settings

import java.io.File

/**
 * Windows HKCU Run key for Continuum Calendar CMP.
 * Prefer a packaged `.exe`; gradle/java launches cannot safely register.
 */
object DesktopAutostart {
    const val RUN_VALUE = "ContinuumCalendarCMP"
    private const val RUN_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""

    fun isEnabled(): Boolean {
        if (!isWindows()) return false
        val out = runReg("query", RUN_KEY, "/v", RUN_VALUE) ?: return false
        return out.contains(RUN_VALUE) && !out.contains("ERROR", ignoreCase = true)
    }

    fun setEnabled(on: Boolean): Result<Unit> {
        if (!isWindows()) return Result.failure(IllegalStateException("Open at startup is only on Windows"))
        return runCatching {
            if (on) {
                val cmd = resolveLaunchCommand()
                    ?: error("Open at startup needs the installed Continuum Calendar CMP app (not a gradle/java run).")
                runReg("add", RUN_KEY, "/v", RUN_VALUE, "/t", "REG_SZ", "/d", cmd, "/f")
                    ?: error("Could not write startup entry")
            } else {
                val out = runReg("delete", RUN_KEY, "/v", RUN_VALUE, "/f").orEmpty()
                if (out.contains("ERROR", ignoreCase = true) &&
                    !out.contains("unable to find", ignoreCase = true)
                ) {
                    error("Could not remove startup entry")
                }
            }
            Unit
        }
    }

    fun resolveLaunchCommand(): String? {
        System.getenv("CONTINUUM_CMP_EXE")?.trim()?.takeIf { it.isNotEmpty() && File(it).isFile }?.let {
            return "\"$it\""
        }
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val file = File(command)
        if (!file.name.equals("java.exe", ignoreCase = true) &&
            !file.name.equals("javaw.exe", ignoreCase = true) &&
            file.name.endsWith(".exe", ignoreCase = true)
        ) {
            return "\"${file.absolutePath}\""
        }
        return null
    }

    fun canRegisterNow(): Boolean = resolveLaunchCommand() != null

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    private fun runReg(vararg args: String): String? {
        return runCatching {
            val pb = ProcessBuilder(listOf("reg.exe") + args.toList())
            pb.redirectErrorStream(true)
            val p = pb.start()
            val text = p.inputStream.bufferedReader(Charsets.UTF_8).readText()
            p.waitFor()
            text
        }.getOrNull()
    }
}
