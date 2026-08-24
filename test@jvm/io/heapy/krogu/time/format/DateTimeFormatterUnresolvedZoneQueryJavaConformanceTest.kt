package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterUnresolvedZoneQueryJavaConformanceTest {
    private val zoneTexts = listOf(
        "Z",
        "z",
        "+01:30",
        "-08:00",
        "+00:00",
        "-00:00",
        "Europe/Paris",
        "UTC",
        "UTC+01:00",
        "GMT-05:00",
    )

    // A zone parsed from an offset id answers the offset query, while a
    // region id answers null. Java does this without ever setting
    // OFFSET_SECONDS, so a query that only reads that field is not enough.
    @Test
    fun unresolvedZoneIdQueriesMatchJavaTime() {
        val mismatches = zoneTexts.mapNotNull { text ->
            val javaParsed = java.time.format.DateTimeFormatterBuilder()
                .appendZoneId()
                .toFormatter()
                .parseUnresolved(text, java.text.ParsePosition(0))
            val kroguParsed = DateTimeFormatterBuilder()
                .appendZoneId()
                .toFormatter()
                .parseUnresolved(text, ParsePosition(0))

            val expected = listOf(
                javaParsed?.query(java.time.temporal.TemporalQueries.zoneId())?.toString(),
                javaParsed?.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                javaParsed?.query(java.time.temporal.TemporalQueries.zone())?.toString(),
                javaParsed?.isSupported(java.time.temporal.ChronoField.OFFSET_SECONDS),
            )
            val actual = listOf(
                kroguParsed?.query(TemporalQueries.zoneId())?.toString(),
                kroguParsed?.query(TemporalQueries.offset())?.toString(),
                kroguParsed?.query(TemporalQueries.zone())?.toString(),
                kroguParsed?.isSupported(ChronoField.OFFSET_SECONDS),
            )
            if (expected == actual) null else "$text: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // An explicit offset pattern does set OFFSET_SECONDS, so the same query
    // must keep answering from the field rather than from the zone.
    @Test
    fun unresolvedOffsetPatternQueriesMatchJavaTime() {
        val mismatches = listOf("+01:30", "-08:00", "Z").mapNotNull { text ->
            val javaParsed = java.time.format.DateTimeFormatterBuilder()
                .appendOffsetId()
                .toFormatter()
                .parseUnresolved(text, java.text.ParsePosition(0))
            val kroguParsed = DateTimeFormatterBuilder()
                .appendOffsetId()
                .toFormatter()
                .parseUnresolved(text, ParsePosition(0))

            val expected = listOf(
                javaParsed?.query(java.time.temporal.TemporalQueries.offset())?.toString(),
                javaParsed?.getLong(java.time.temporal.ChronoField.OFFSET_SECONDS),
            )
            val actual = listOf(
                kroguParsed?.query(TemporalQueries.offset())?.toString(),
                kroguParsed?.getLong(ChronoField.OFFSET_SECONDS),
            )
            if (expected == actual) null else "$text: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }
}
