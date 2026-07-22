package io.heapy.grogu.time.format

internal data class ParsedLocaleZoneText(
    val zoneId: String,
    val endIndex: Int,
)

internal expect fun formatLocaleZoneText(
    languageTag: String,
    zoneId: String,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): String?

internal expect fun parseLocaleZoneText(
    languageTag: String,
    text: String,
    startIndex: Int,
    style: TextStyle,
    generic: Boolean,
    caseSensitive: Boolean,
): ParsedLocaleZoneText?
