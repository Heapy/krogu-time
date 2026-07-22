package io.heapy.grogu.time.format

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderLocalizedOffsetJavaConformanceTest {
    @Test
    fun fullAndShortFormattingParsingAndDescriptionsMatchJavaTime() {
        fun javaFormatter(style: java.time.format.TextStyle): java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatterBuilder()
                .appendLocalizedOffset(style)
                .toFormatter(Locale.ROOT)
        fun groguFormatter(style: TextStyle): DateTimeFormatter = DateTimeFormatterBuilder()
            .appendLocalizedOffset(style)
            .toFormatter()

        val javaFull = javaFormatter(java.time.format.TextStyle.FULL)
        val groguFull = groguFormatter(TextStyle.FULL)
        val javaShort = javaFormatter(java.time.format.TextStyle.SHORT)
        val groguShort = groguFormatter(TextStyle.SHORT)
        val totalSeconds = listOf(0, 7_200, 9_000, -19_845, 64_800)
        totalSeconds.forEach { seconds ->
            val javaOffset = java.time.ZoneOffset.ofTotalSeconds(seconds)
            val groguOffset = io.heapy.grogu.time.ZoneOffset.ofTotalSeconds(seconds)
            val javaTime = java.time.OffsetTime.of(java.time.LocalTime.NOON, javaOffset)
            val groguTime = io.heapy.grogu.time.OffsetTime.of(io.heapy.grogu.time.LocalTime.NOON, groguOffset)
            listOf(javaFull to groguFull, javaShort to groguShort).forEach { (javaValue, groguValue) ->
                val text = javaValue.format(javaTime)
                assertEquals(text, groguValue.format(groguTime))
                assertEquals(
                    java.time.ZoneOffset.from(javaValue.parse(text)).totalSeconds,
                    io.heapy.grogu.time.ZoneOffset.from(groguValue.parse(text)).totalSeconds,
                )
            }
        }
        assertEquals(javaFull.toString(), groguFull.toString())
        assertEquals(javaShort.toString(), groguShort.toString())
    }

    @Test
    fun acceptedTextPatternsAndBuilderCompositionMatchJavaTime() {
        val javaFull = java.time.format.DateTimeFormatterBuilder()
            .appendLocalizedOffset(java.time.format.TextStyle.FULL)
            .toFormatter(Locale.ROOT)
        val groguFull = DateTimeFormatterBuilder()
            .appendLocalizedOffset(TextStyle.FULL)
            .toFormatter()
        val javaShort = java.time.format.DateTimeFormatterBuilder()
            .appendLocalizedOffset(java.time.format.TextStyle.SHORT)
            .toFormatter(Locale.ROOT)
        val groguShort = DateTimeFormatterBuilder()
            .appendLocalizedOffset(TextStyle.SHORT)
            .toFormatter()
        val samples = listOf(
            "GMT",
            "GMT+2",
            "GMT+02",
            "GMT+2:30",
            "GMT+02:30",
            "GMT+02:30:45",
            "gmt+2",
            "UTC+02:00",
        )
        samples.forEach { text ->
            listOf(javaFull to groguFull, javaShort to groguShort).forEach { (javaValue, groguValue) ->
                assertEquals(
                    runCatching { javaValue.parse(text) }.isSuccess,
                    runCatching { groguValue.parse(text) }.isSuccess,
                    "${javaValue}:$text",
                )
            }
        }

        listOf("O", "OOOO", "ZZZZ").forEach { pattern ->
            val javaPattern = java.time.format.DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
            val groguPattern = DateTimeFormatter.ofPattern(pattern)
            val javaTime = java.time.OffsetTime.of(
                java.time.LocalTime.NOON,
                java.time.ZoneOffset.ofHoursMinutes(2, 30),
            )
            val groguTime = io.heapy.grogu.time.OffsetTime.of(
                io.heapy.grogu.time.LocalTime.NOON,
                io.heapy.grogu.time.ZoneOffset.ofHoursMinutes(2, 30),
            )
            assertEquals(javaPattern.format(javaTime), groguPattern.format(groguTime), pattern)
            assertEquals(javaPattern.toString(), groguPattern.toString(), pattern)
        }
    }
}
