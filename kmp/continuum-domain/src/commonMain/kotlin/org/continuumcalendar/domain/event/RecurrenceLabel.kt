package org.continuumcalendar.domain.event

enum class RepeatFreq {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

fun rruleBlob(rules: List<String>?): String =
    (rules ?: emptyList()).joinToString(";").uppercase()

/** Display-only frequency from Google RRULE list (shipping freqFromRrule subset). */
fun freqFromRrule(rules: List<String>?): RepeatFreq {
    val blob = rruleBlob(rules)
    return when {
        blob.contains("FREQ=DAILY") -> RepeatFreq.DAILY
        blob.contains("FREQ=WEEKLY") -> RepeatFreq.WEEKLY
        blob.contains("FREQ=MONTHLY") -> RepeatFreq.MONTHLY
        blob.contains("FREQ=YEARLY") -> RepeatFreq.YEARLY
        else -> RepeatFreq.NONE
    }
}

fun formatRepeatFreq(freq: RepeatFreq): String = when (freq) {
    RepeatFreq.NONE -> "Does not repeat"
    RepeatFreq.DAILY -> "Daily"
    RepeatFreq.WEEKLY -> "Weekly"
    RepeatFreq.MONTHLY -> "Monthly"
    RepeatFreq.YEARLY -> "Yearly"
}
