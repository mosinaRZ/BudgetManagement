package ir.hamedan.budgetmanagement.ui.screens.admin

import ir.hamedan.budgetmanagement.utils.DateUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure formatting helpers (no Compose). Labels and icons live in AdminLabels.kt. */

internal fun numberFormat(isPersian: Boolean, fractionDigits: Int = 0): NumberFormat =
    NumberFormat.getNumberInstance(if (isPersian) Locale.forLanguageTag("fa-IR") else Locale.US).apply {
        maximumFractionDigits = fractionDigits
        minimumFractionDigits = 0
    }

internal fun fmtNumber(value: Long, isPersian: Boolean): String = numberFormat(isPersian).format(value)

internal fun fmtNumber(value: Int, isPersian: Boolean): String = fmtNumber(value.toLong(), isPersian)

internal fun fmtPercent(value: Int, isPersian: Boolean): String =
    fmtNumber(value, isPersian) + if (isPersian) "٪" else "%"

/** 12 345 -> "12.3K" style short form for chart axes and KPI cards. */
internal fun fmtCompact(value: Long, isPersian: Boolean): String {
    val nf = numberFormat(isPersian, 1)
    return when {
        value >= 1_000_000 -> nf.format(value / 1_000_000.0) + "M"
        value >= 10_000 -> nf.format(value / 1_000.0) + "K"
        else -> fmtNumber(value, isPersian)
    }
}

internal fun fmtBytes(bytes: Long, isPersian: Boolean): String {
    val nf = numberFormat(isPersian, 1)
    return when {
        bytes < 1024 -> nf.format(bytes) + " B"
        bytes < 1024L * 1024 -> nf.format(bytes / 1024.0) + " KB"
        bytes < 1024L * 1024 * 1024 -> nf.format(bytes / (1024.0 * 1024)) + " MB"
        else -> nf.format(bytes / (1024.0 * 1024 * 1024)) + " GB"
    }
}

internal fun relativeTime(millis: Long, isPersian: Boolean, now: Long = System.currentTimeMillis()): String {
    if (millis <= 0) return if (isPersian) "نامشخص" else "Unknown"
    val minutes = ((now - millis).coerceAtLeast(0)) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> if (isPersian) "چند لحظه پیش" else "Just now"
        minutes < 60 -> if (isPersian) "${fmtNumber(minutes, true)} دقیقه پیش" else "$minutes min ago"
        hours < 24 -> if (isPersian) "${fmtNumber(hours, true)} ساعت پیش" else "$hours h ago"
        days < 30 -> if (isPersian) "${fmtNumber(days, true)} روز پیش" else "$days d ago"
        else -> DateUtils.formatTimestamp(millis, isPersian)
    }
}

internal fun dateTime(millis: Long, isPersian: Boolean): String {
    if (millis <= 0) return if (isPersian) "نامشخص" else "Unknown"
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "${DateUtils.formatTimestamp(millis, isPersian)} - $time"
}

/** "2026-11-03" -> "11/03" (Gregorian) or "08/12" (Jalali), used on chart axes. */
internal fun dayLabel(day: String, isPersian: Boolean): String = runCatching {
    val date = LocalDate.parse(day)
    if (isPersian) {
        val (_, m, d) = DateUtils.toJalali(date)
        val nf = numberFormat(true).apply { minimumIntegerDigits = 2 }
        "${nf.format(m)}/${nf.format(d)}"
    } else {
        "%02d/%02d".format(date.monthValue, date.dayOfMonth)
    }
}.getOrDefault(day)