package org.fossify.calendar.extensions

import android.app.Activity
import android.net.Uri
import org.fossify.calendar.BuildConfig
import org.fossify.calendar.activities.SimpleActivity
import org.fossify.calendar.dialogs.CustomEventRepeatIntervalDialog
import org.fossify.calendar.dialogs.ImportEventsDialog
import org.fossify.calendar.helpers.*
import org.fossify.calendar.models.Event
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.RadioItem
import java.io.File
import java.io.FileOutputStream
import java.util.TreeSet

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsOrTasksWithIds(ids) as ArrayList<Event>
        if (events.isEmpty()) {
            toast(org.fossify.commons.R.string.no_items_found)
        }

        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter(this).exportEvents(it, events, false) { result ->
                if (result == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(org.fossify.commons.R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) {
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}

fun SimpleActivity.tryImportEventsFromFile(uri: Uri, callback: (Boolean) -> Unit = {}) {
    when (uri.scheme?.lowercase()) {
        "file" -> showImportEventsDialog(uri.path!!, callback)
        "content" -> {
            val tempFile = getTempFile()
            if (tempFile == null) {
                toast(org.fossify.commons.R.string.unknown_error_occurred)
                return
            }

            try {
                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream!!.copyTo(out)
                showImportEventsDialog(tempFile.absolutePath, callback)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

        "webcal", "webcals", "http", "https" -> {
            ensureBackgroundThread {
                try {
                    val tempFile = getTempFile()
                    if (tempFile == null) {
                        toast(org.fossify.commons.R.string.unknown_error_occurred)
                        return@ensureBackgroundThread
                    }
                    val fetchUrl = when (uri.scheme?.lowercase()) {
                        "webcal" -> uri.toString().replaceFirst("webcal://", "https://")
                        "webcals" -> uri.toString().replaceFirst("webcals://", "https://")
                        else -> uri.toString()
                    }
                    val connection = java.net.URL(fetchUrl).openConnection().apply {
                        connectTimeout = 20_000
                        readTimeout = 20_000
                    }
                    connection.getInputStream().use { input ->
                        FileOutputStream(tempFile).use { out ->
                            val buffer = ByteArray(8192)
                            var total = 0
                            val maxBytes = 2_000_000
                            while (true) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                total += n
                                if (total > maxBytes) {
                                    throw IllegalStateException("ICS download too large")
                                }
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                    runOnUiThread {
                        showImportEventsDialog(tempFile.absolutePath, callback)
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                    callback(false)
                }
            }
        }

        else -> toast(org.fossify.commons.R.string.invalid_file_format)
    }
}

fun SimpleActivity.showImportEventsDialog(path: String, callback: (Boolean) -> Unit) {
    ImportEventsDialog(this, path, callback)
}
