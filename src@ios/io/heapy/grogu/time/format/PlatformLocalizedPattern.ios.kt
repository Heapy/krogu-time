package io.heapy.grogu.time.format

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale

internal actual fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    dateStyle: FormatStyle?,
    timeStyle: FormatStyle?,
): String {
    require(dateStyle != null || timeStyle != null) {
        "Either dateStyle or timeStyle must be non-null"
    }
    val formatter = NSDateFormatter()
    formatter.setLocale(NSLocale(localeIdentifier = chronologyLocaleIdentifier(languageTag, chronologyId)))
    formatter.setDateStyle(dateStyle?.toNSDateFormatterStyle() ?: NSDateFormatterNoStyle)
    formatter.setTimeStyle(timeStyle?.toNSDateFormatterStyle() ?: NSDateFormatterNoStyle)
    return formatter.dateFormat()
}

internal actual fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    requestedTemplate: String,
): String = NSDateFormatter.dateFormatFromTemplate(
    requestedTemplate,
    options = 0u,
    locale = NSLocale(localeIdentifier = chronologyLocaleIdentifier(languageTag, chronologyId)),
) ?: error("Platform date formatter does not expose a pattern for $requestedTemplate")

private fun FormatStyle.toNSDateFormatterStyle(): ULong = when (this) {
    FormatStyle.FULL -> NSDateFormatterFullStyle
    FormatStyle.LONG -> NSDateFormatterLongStyle
    FormatStyle.MEDIUM -> NSDateFormatterMediumStyle
    FormatStyle.SHORT -> NSDateFormatterShortStyle
}

private fun chronologyLocaleIdentifier(languageTag: String, chronologyId: String): String {
    val calendarType = when (chronologyId) {
        "ISO" -> null
        "Japanese" -> "japanese"
        "Hijrah-umalqura" -> "islamic-umalqura"
        "Minguo" -> "roc"
        "ThaiBuddhist" -> "buddhist"
        else -> null
    }
    return calendarType?.let { "$languageTag@calendar=$it" } ?: languageTag
}
