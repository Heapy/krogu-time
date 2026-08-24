package io.heapy.krogu.time.format

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
    preferredZoneIds: Set<String>,
): ParsedLocaleZoneText? {
    val locale = Locale.forLanguageTag(languageTag)
    parsePreferredZoneText(
        locale = locale,
        text = text,
        startIndex = startIndex,
        style = style,
        generic = generic,
        caseSensitive = caseSensitive,
        preferredZoneIds = preferredZoneIds,
    )?.let { return it }
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

private fun parsePreferredZoneText(
    locale: Locale,
    text: String,
    startIndex: Int,
    style: TextStyle,
    generic: Boolean,
    caseSensitive: Boolean,
    preferredZoneIds: Set<String>,
): ParsedLocaleZoneText? = preferredZoneIds.asSequence()
    .flatMap { zoneId ->
        preferredZoneNames(locale, zoneId, style, generic)
            .asSequence()
            .map { name -> zoneId to name }
    }
    .filter { (_, name) -> text.matchesAt(startIndex, name, caseSensitive) }
    .maxByOrNull { (_, name) -> name.length }
    ?.let { (zoneId, name) -> ParsedLocaleZoneText(zoneId, startIndex + name.length) }

private fun preferredZoneNames(
    locale: Locale,
    zoneId: String,
    style: TextStyle,
    generic: Boolean,
): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    preferredZoneNamesFromIcu(locale, zoneId, style, generic)
} else {
    val zone = TimeZone.getTimeZone(zoneId)
    if (zone.id == "GMT" && zoneId != "GMT") {
        emptyList()
    } else {
        val displayStyle = if (style.asNormal() == TextStyle.FULL) TimeZone.LONG else TimeZone.SHORT
        listOf(
            zone.getDisplayName(false, displayStyle, locale),
            zone.getDisplayName(true, displayStyle, locale),
        ).distinct()
    }
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun preferredZoneNamesFromIcu(
    locale: Locale,
    zoneId: String,
    style: TextStyle,
    generic: Boolean,
): List<String> {
    val zone = android.icu.util.TimeZone.getTimeZone(zoneId)
    if (zone.id == "Etc/Unknown") return emptyList()
    val formatter = android.icu.text.TimeZoneFormat.getInstance(locale)
    return if (generic) {
        listOf(formatter.format(zoneTextStyle(style, generic = true), zone, 0))
    } else {
        listOf(
            formatter.format(zoneTextStyle(style, generic = false), zone, JANUARY_2024_MILLIS),
            formatter.format(zoneTextStyle(style, generic = false), zone, JULY_2024_MILLIS),
            formatter.format(zoneTextStyle(style, generic = true), zone, 0),
        ).distinct()
    }
}

private fun String.matchesAt(index: Int, value: String, caseSensitive: Boolean): Boolean =
    index >= 0 && index + value.length <= length &&
        regionMatches(index, value, 0, value.length, ignoreCase = !caseSensitive)

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

private const val JANUARY_2024_MILLIS: Long = 1_704_067_200_000L
private const val JULY_2024_MILLIS: Long = 1_719_792_000_000L
