package io.heapy.grogu.time.format

import java.text.ParsePosition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalQuery
import java.time.temporal.TemporalQueries
import java.util.Locale

internal actual fun formatLocaleZoneText(
    languageTag: String,
    zoneId: String,
    epochSecond: Long?,
    style: TextStyle,
    generic: Boolean,
): String? {
    val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
    val formatter = zoneTextBuilder(style, generic)
        .toFormatter(Locale.forLanguageTag(languageTag))
    val temporal: TemporalAccessor = if (epochSecond != null) {
        java.time.ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zone)
    } else {
        ZoneOnlyTemporal(zone)
    }
    return formatter.format(temporal)
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
    val builder = DateTimeFormatterBuilder()
    if (caseSensitive) builder.parseCaseSensitive() else builder.parseCaseInsensitive()
    if (generic) {
        val javaStyle = java.time.format.TextStyle.valueOf(style.name)
        if (preferredZoneIds.isEmpty()) {
            builder.appendGenericZoneText(javaStyle)
        } else {
            builder.appendGenericZoneText(javaStyle, preferredZoneIds.mapTo(mutableSetOf(), ZoneId::of))
        }
    } else {
        val javaStyle = java.time.format.TextStyle.valueOf(style.name)
        if (preferredZoneIds.isEmpty()) {
            builder.appendZoneText(javaStyle)
        } else {
            builder.appendZoneText(javaStyle, preferredZoneIds.mapTo(mutableSetOf(), ZoneId::of))
        }
    }
    val position = ParsePosition(startIndex)
    val parsed = builder
        .toFormatter(Locale.forLanguageTag(languageTag))
        .parseUnresolved(text, position)
        ?: return null
    val zone = parsed.query(TemporalQueries.zone()) ?: return null
    return ParsedLocaleZoneText(zone.id, position.index)
}

private fun zoneTextBuilder(style: TextStyle, generic: Boolean): DateTimeFormatterBuilder =
    DateTimeFormatterBuilder().apply {
        val javaStyle = java.time.format.TextStyle.valueOf(style.name)
        if (generic) appendGenericZoneText(javaStyle) else appendZoneText(javaStyle)
    }

private class ZoneOnlyTemporal(
    private val zone: ZoneId,
) : TemporalAccessor {
    override fun isSupported(field: java.time.temporal.TemporalField): Boolean = false

    override fun getLong(field: java.time.temporal.TemporalField): Long =
        throw java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: $field")

    override fun <R : Any?> query(query: TemporalQuery<R>): R? {
        if (query === TemporalQueries.zoneId() || query === TemporalQueries.zone()) {
            @Suppress("UNCHECKED_CAST")
            return zone as R
        }
        return super.query(query)
    }
}
