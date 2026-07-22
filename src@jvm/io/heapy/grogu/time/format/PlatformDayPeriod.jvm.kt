package io.heapy.grogu.time.format

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val dayPeriodFormatters: ConcurrentHashMap<String, DateTimeFormatter> = ConcurrentHashMap()
private val dayPeriodValues: ConcurrentHashMap<String, List<LocaleDayPeriod>> = ConcurrentHashMap()

internal actual fun formatLocaleDayPeriod(
    languageTag: String,
    hour: Int,
    minute: Int,
    style: TextStyle,
): String = dayPeriodFormatter(languageTag, style).format(LocalTime.of(hour, minute))

internal actual fun localeDayPeriods(
    languageTag: String,
    style: TextStyle,
): List<LocaleDayPeriod> {
    val key = dayPeriodKey(languageTag, style)
    return dayPeriodValues.computeIfAbsent(key) {
        val formatter = dayPeriodFormatter(languageTag, style)
        discoverLocaleDayPeriods { hour, minute -> formatter.format(LocalTime.of(hour, minute)) }
    }
}

private fun dayPeriodFormatter(languageTag: String, style: TextStyle): DateTimeFormatter {
    val key = dayPeriodKey(languageTag, style)
    return dayPeriodFormatters.computeIfAbsent(key) {
        DateTimeFormatterBuilder()
            .appendDayPeriodText(java.time.format.TextStyle.valueOf(style.asNormal().name))
            .toFormatter(Locale.forLanguageTag(languageTag))
    }
}

private fun dayPeriodKey(languageTag: String, style: TextStyle): String =
    "$languageTag|${style.asNormal()}"
