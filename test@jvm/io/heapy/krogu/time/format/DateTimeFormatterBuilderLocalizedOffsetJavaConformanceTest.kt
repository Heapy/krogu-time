package io.heapy.krogu.time.format

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
        fun kroguFormatter(style: TextStyle): DateTimeFormatter = DateTimeFormatterBuilder()
            .appendLocalizedOffset(style)
            .toFormatter()

        val javaFull = javaFormatter(java.time.format.TextStyle.FULL)
        val kroguFull = kroguFormatter(TextStyle.FULL)
        val javaShort = javaFormatter(java.time.format.TextStyle.SHORT)
        val kroguShort = kroguFormatter(TextStyle.SHORT)
        val totalSeconds = listOf(0, 7_200, 9_000, -19_845, 64_800)
        totalSeconds.forEach { seconds ->
            val javaOffset = java.time.ZoneOffset.ofTotalSeconds(seconds)
            val kroguOffset = io.heapy.krogu.time.ZoneOffset.ofTotalSeconds(seconds)
            val javaTime = java.time.OffsetTime.of(java.time.LocalTime.NOON, javaOffset)
            val kroguTime = io.heapy.krogu.time.OffsetTime.of(io.heapy.krogu.time.LocalTime.NOON, kroguOffset)
            listOf(javaFull to kroguFull, javaShort to kroguShort).forEach { (javaValue, kroguValue) ->
                val text = javaValue.format(javaTime)
                assertEquals(text, kroguValue.format(kroguTime))
                assertEquals(
                    java.time.ZoneOffset.from(javaValue.parse(text)).totalSeconds,
                    io.heapy.krogu.time.ZoneOffset.from(kroguValue.parse(text)).totalSeconds,
                )
            }
        }
        assertEquals(javaFull.toString(), kroguFull.toString())
        assertEquals(javaShort.toString(), kroguShort.toString())
    }

    @Test
    fun acceptedTextPatternsAndBuilderCompositionMatchJavaTime() {
        val javaFull = java.time.format.DateTimeFormatterBuilder()
            .appendLocalizedOffset(java.time.format.TextStyle.FULL)
            .toFormatter(Locale.ROOT)
        val kroguFull = DateTimeFormatterBuilder()
            .appendLocalizedOffset(TextStyle.FULL)
            .toFormatter()
        val javaShort = java.time.format.DateTimeFormatterBuilder()
            .appendLocalizedOffset(java.time.format.TextStyle.SHORT)
            .toFormatter(Locale.ROOT)
        val kroguShort = DateTimeFormatterBuilder()
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
            listOf(javaFull to kroguFull, javaShort to kroguShort).forEach { (javaValue, kroguValue) ->
                assertEquals(
                    runCatching { javaValue.parse(text) }.isSuccess,
                    runCatching { kroguValue.parse(text) }.isSuccess,
                    "${javaValue}:$text",
                )
            }
        }

        listOf("O", "OOOO", "ZZZZ").forEach { pattern ->
            val javaPattern = java.time.format.DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
            val kroguPattern = DateTimeFormatter.ofPattern(pattern)
            val javaTime = java.time.OffsetTime.of(
                java.time.LocalTime.NOON,
                java.time.ZoneOffset.ofHoursMinutes(2, 30),
            )
            val kroguTime = io.heapy.krogu.time.OffsetTime.of(
                io.heapy.krogu.time.LocalTime.NOON,
                io.heapy.krogu.time.ZoneOffset.ofHoursMinutes(2, 30),
            )
            assertEquals(javaPattern.format(javaTime), kroguPattern.format(kroguTime), pattern)
            assertEquals(javaPattern.toString(), kroguPattern.toString(), pattern)
        }
    }
}
