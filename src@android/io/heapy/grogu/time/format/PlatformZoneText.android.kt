package io.heapy.grogu.time.format

import android.os.Build
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun formatLocaleZoneText(
    languageTag: String,
    zoneId: String,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): String? {
    val locale = Locale.forLanguageTag(languageTag)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        formatLocaleZoneTextFromIcu(locale, zoneId, epochSecond, style, generic)
    } else {
        val zone = TimeZone.getTimeZone(zoneId)
        if (zone.id == "GMT" && zoneId != "GMT") return null
        val daylight = epochSecond?.let { zone.inDaylightTime(Date(it * 1_000)) } ?: false
        zone.getDisplayName(
            daylight,
            if (style.asNormal() == TextStyle.FULL) TimeZone.LONG else TimeZone.SHORT,
            locale,
        )
    }
}

internal actual fun parseLocaleZoneText(
    languageTag: String,
    text: String,
    startIndex: Int,
    style: TextStyle,
    generic: Boolean,
    caseSensitive: Boolean,
): ParsedLocaleZoneText? {
    val locale = Locale.forLanguageTag(languageTag)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        parseLocaleZoneTextFromIcu(locale, text, startIndex, style, generic)
    } else {
        val formatter = SimpleDateFormat(
            if (style.asNormal() == TextStyle.FULL) "zzzz" else "z",
            locale,
        )
        val position = ParsePosition(startIndex)
        formatter.parse(text, position) ?: return null
        ParsedLocaleZoneText(formatter.calendar.timeZone.id, position.index)
    }
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun formatLocaleZoneTextFromIcu(
    locale: Locale,
    zoneId: String,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): String? {
    val zone = android.icu.util.TimeZone.getTimeZone(zoneId)
    if (zone.id == "Etc/Unknown") return null
    val zoneStyle = zoneTextStyle(style, generic || epochSecond == null)
    return android.icu.text.TimeZoneFormat.getInstance(locale)
        .format(zoneStyle, zone, (epochSecond ?: 0) * 1_000)
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun parseLocaleZoneTextFromIcu(
    locale: Locale,
    text: String,
    startIndex: Int,
    style: TextStyle,
    generic: Boolean,
): ParsedLocaleZoneText? {
    val position = ParsePosition(startIndex)
    val zone = android.icu.text.TimeZoneFormat.getInstance(locale).parse(
        zoneTextStyle(style, generic),
        text,
        position,
        android.icu.util.Output(),
    ) ?: return null
    return ParsedLocaleZoneText(zone.id, position.index)
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun zoneTextStyle(
    style: TextStyle,
    generic: Boolean,
): android.icu.text.TimeZoneFormat.Style = when {
    generic && style.asNormal() == TextStyle.FULL -> android.icu.text.TimeZoneFormat.Style.GENERIC_LONG
    generic -> android.icu.text.TimeZoneFormat.Style.GENERIC_SHORT
    style.asNormal() == TextStyle.FULL -> android.icu.text.TimeZoneFormat.Style.SPECIFIC_LONG
    else -> android.icu.text.TimeZoneFormat.Style.SPECIFIC_SHORT
}
