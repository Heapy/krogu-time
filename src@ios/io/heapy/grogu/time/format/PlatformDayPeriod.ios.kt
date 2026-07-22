package io.heapy.grogu.time.format

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLock
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT

private val dayPeriodValues: MutableMap<String, List<LocaleDayPeriod>> = mutableMapOf()
private val dayPeriodValuesLock: NSLock = NSLock()

internal actual fun formatLocaleDayPeriod(
    languageTag: String,
    hour: Int,
    minute: Int,
    style: TextStyle,
): String = dayPeriodFormatter(languageTag, style).stringFromDate(
    NSDate.dateWithTimeIntervalSince1970((hour * 60L + minute) * 60.0),
)

internal actual fun localeDayPeriods(
    languageTag: String,
    style: TextStyle,
): List<LocaleDayPeriod> {
    val key = "$languageTag|${style.asNormal()}"
    dayPeriodValuesLock.lock()
    return try {
        dayPeriodValues.getOrPut(key) {
            val formatter = dayPeriodFormatter(languageTag, style)
            discoverLocaleDayPeriods { hour, minute ->
                formatter.stringFromDate(
                    NSDate.dateWithTimeIntervalSince1970((hour * 60L + minute) * 60.0),
                )
            }
        }
    } finally {
        dayPeriodValuesLock.unlock()
    }
}

private fun dayPeriodFormatter(
    languageTag: String,
    style: TextStyle,
): NSDateFormatter = NSDateFormatter().apply {
    setLocale(NSLocale(localeIdentifier = languageTag))
    setTimeZone(NSTimeZone.timeZoneForSecondsFromGMT(0))
    setDateFormat(style.dayPeriodPattern())
}

private fun TextStyle.dayPeriodPattern(): String = when (asNormal()) {
    TextStyle.FULL -> "BBBB"
    TextStyle.SHORT -> "B"
    TextStyle.NARROW -> "BBBBB"
    else -> error("Unreachable day-period style: $this")
}
