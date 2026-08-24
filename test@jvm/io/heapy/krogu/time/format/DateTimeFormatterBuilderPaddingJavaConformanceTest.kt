package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderPaddingJavaConformanceTest {
    @Test
    fun paddingFormattingAndParsingMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .padNext(3)
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH)
            .appendLiteral('|')
            .padNext(4, '_')
            .appendLiteral('X')
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .padNext(3)
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH)
            .appendLiteral('|')
            .padNext(4, '_')
            .appendLiteral('X')
            .toFormatter()

        assertEquals(
            javaFormatter.format(java.time.LocalDate.of(2024, 1, 3)),
            kroguFormatter.format(io.heapy.krogu.time.LocalDate.of(2024, 1, 3)),
        )
        listOf("  3|___X", " 3|___X", "3|___X", "   |___X").forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { kroguFormatter.parse(text) }.isSuccess,
                text,
            )
        }
    }

    @Test
    fun lenientCaseInsensitiveAndOptionalPaddingMatchJavaTime() {
        val javaLenient = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .padNext(3, 'A')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()
        val kroguLenient = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .padNext(3, 'A')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()
        listOf("aa3", "a3", "3", "aaa3").forEach { text ->
            assertEquals(
                runCatching { javaLenient.parse(text) }.isSuccess,
                runCatching { kroguLenient.parse(text) }.isSuccess,
                text,
            )
        }

        val javaOptional = java.time.format.DateTimeFormatterBuilder()
            .padNext(3, '_')
            .optionalStart()
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            .optionalEnd()
            .toFormatter()
        val kroguOptional = DateTimeFormatterBuilder()
            .padNext(3, '_')
            .optionalStart()
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR)
            .optionalEnd()
            .toFormatter()
        assertEquals(javaOptional.format(java.time.Year.of(2024)), kroguOptional.format(io.heapy.krogu.time.Year.of(2024)))
        assertEquals(
            javaOptional.format(java.time.YearMonth.of(2024, 3)),
            kroguOptional.format(io.heapy.krogu.time.YearMonth.of(2024, 3)),
        )
    }

    @Test
    fun padPatternSyntaxMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatter.ofPattern("ppH:mm")
        val kroguFormatter = DateTimeFormatter.ofPattern("ppH:mm")

        assertEquals(
            javaFormatter.format(java.time.LocalTime.of(3, 5)),
            kroguFormatter.format(io.heapy.krogu.time.LocalTime.of(3, 5)),
        )
        listOf(" 3:05", "3:05", " 03:05").forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { kroguFormatter.parse(text) }.isSuccess,
                text,
            )
        }
    }
}
