package io.heapy.krogu.time.format

import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSTimeZoneNameStyle
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.knownTimeZoneNames
import platform.Foundation.localizedName
import platform.Foundation.timeZoneWithName

internal actual fun formatLocaleZoneText(
    languageTag: String,
    zoneId: String,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): String? {
    if (style == TextStyle.NARROW) return null
    val zone = NSTimeZone.timeZoneWithName(zoneId) ?: return null
    val nameStyle = zoneNameStyle(zone, epochSecond, style, generic)
    return zone.localizedName(nameStyle, NSLocale(localeIdentifier = languageTag))
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
    if (style == TextStyle.NARROW) return null
    val locale = NSLocale(localeIdentifier = languageTag)
    val zoneIds = preferredZoneIds +
        preferredZoneIds(languageTag) +
        NSTimeZone.knownTimeZoneNames.mapNotNull { it as? String }
    val styles = if (generic) {
        listOf(genericZoneNameStyle(style))
    } else {
        listOf(
            specificZoneNameStyle(style, daylight = false),
            specificZoneNameStyle(style, daylight = true),
            genericZoneNameStyle(style),
        )
    }
    return zoneIds.asSequence()
        .distinct()
        .mapNotNull { zoneId ->
            val zone = NSTimeZone.timeZoneWithName(zoneId) ?: return@mapNotNull null
            styles.asSequence()
                .mapNotNull { nameStyle -> zone.localizedName(nameStyle, locale) }
                .filter { name -> text.matchesAt(startIndex, name, caseSensitive) }
                .map { name -> ParsedLocaleZoneText(zoneId, startIndex + name.length) }
                .maxByOrNull { parsed -> parsed.endIndex }
        }
        .maxByOrNull { parsed -> parsed.endIndex }
}

private fun zoneNameStyle(
    zone: NSTimeZone,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): NSTimeZoneNameStyle {
    if (generic || epochSecond == null) return genericZoneNameStyle(style)
    val date = NSDate.dateWithTimeIntervalSince1970(epochSecond.toDouble())
    return specificZoneNameStyle(style, zone.isDaylightSavingTimeForDate(date))
}

private fun specificZoneNameStyle(style: TextStyle, daylight: Boolean): NSTimeZoneNameStyle = when {
    daylight && style.asNormal() == TextStyle.FULL -> NSTimeZoneNameStyle.NSTimeZoneNameStyleDaylightSaving
    daylight -> NSTimeZoneNameStyle.NSTimeZoneNameStyleShortDaylightSaving
    style.asNormal() == TextStyle.FULL -> NSTimeZoneNameStyle.NSTimeZoneNameStyleStandard
    else -> NSTimeZoneNameStyle.NSTimeZoneNameStyleShortStandard
}

private fun genericZoneNameStyle(style: TextStyle): NSTimeZoneNameStyle =
    if (style.asNormal() == TextStyle.FULL) {
        NSTimeZoneNameStyle.NSTimeZoneNameStyleGeneric
    } else {
        NSTimeZoneNameStyle.NSTimeZoneNameStyleShortGeneric
    }

private fun preferredZoneIds(languageTag: String): List<String> = when (
    languageTag.substringAfter('-', "").substringBefore('-').uppercase()
) {
    "US" -> listOf(
        "America/New_York",
        "America/Chicago",
        "America/Denver",
        "America/Los_Angeles",
        "America/Anchorage",
        "Pacific/Honolulu",
    )
    "GB" -> listOf("Europe/London")
    "FR" -> listOf("Europe/Paris")
    "DE" -> listOf("Europe/Berlin")
    "JP" -> listOf("Asia/Tokyo")
    else -> emptyList()
}

private fun String.matchesAt(index: Int, value: String, caseSensitive: Boolean): Boolean =
    index >= 0 && index + value.length <= length &&
        regionMatches(index, value, 0, value.length, ignoreCase = !caseSensitive)
