package io.heapy.grogu.time.format

import android.os.Build
import java.text.DateFormatSymbols
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val dayPeriodValues: ConcurrentHashMap<String, List<LocaleDayPeriod>> = ConcurrentHashMap()

internal actual fun formatLocaleDayPeriod(
    languageTag: String,
    hour: Int,
    minute: Int,
    style: TextStyle,
): String {
    val locale = Locale.forLanguageTag(languageTag)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        dayPeriodFormatter(locale, style).format(Date((hour * 60L + minute) * 60_000L))
    } else {
        DateFormatSymbols.getInstance(locale).amPmStrings[hour / 12]
    }
}

internal actual fun localeDayPeriods(
    languageTag: String,
    style: TextStyle,
): List<LocaleDayPeriod> {
    val key = "$languageTag|${style.asNormal()}"
    dayPeriodValues[key]?.let { return it }
    val discovered = run {
        val locale = Locale.forLanguageTag(languageTag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val formatter = dayPeriodFormatter(locale, style)
            discoverLocaleDayPeriods { hour, minute ->
                formatter.format(Date((hour * 60L + minute) * 60_000L))
            }
        } else {
            val symbols = DateFormatSymbols.getInstance(locale).amPmStrings
            listOf(
                LocaleDayPeriod(symbols[0], 0, 12 * 60),
                LocaleDayPeriod(symbols[1], 12 * 60, 24 * 60),
            )
        }
    }
    return dayPeriodValues.putIfAbsent(key, discovered) ?: discovered
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun dayPeriodFormatter(
    locale: Locale,
    style: TextStyle,
): android.icu.text.SimpleDateFormat = android.icu.text.SimpleDateFormat(
    style.dayPeriodPattern(),
    locale,
).apply {
    timeZone = android.icu.util.TimeZone.GMT_ZONE
}

private fun TextStyle.dayPeriodPattern(): String = when (asNormal()) {
    TextStyle.FULL -> "BBBB"
    TextStyle.SHORT -> "B"
    TextStyle.NARROW -> "BBBBB"
    else -> error("Unreachable day-period style: $this")
}
