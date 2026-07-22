package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderJavaConformanceTest {
    @Test
    fun patternAndLiteralCompositionMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('T')
            .appendPattern("HH:mm:ssXXX")
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('T')
            .appendPattern("HH:mm:ssXXX")
            .toFormatter()
        val javaDateTime = java.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val groguDateTime = io.heapy.grogu.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val javaText = javaFormatter.format(javaDateTime)
        val groguText = groguFormatter.format(groguDateTime)

        assertEquals(javaText, groguText)
        assertEquals(
            java.time.LocalDateTime.from(javaFormatter.parse(javaText)).toString(),
            groguFormatter.parse(groguText, LocalDateTime::from).toString(),
        )
    }

    @Test
    fun numericValueCompositionMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValue(
                java.time.temporal.ChronoField.YEAR,
                4,
                10,
                java.time.format.SignStyle.EXCEEDS_PAD,
            )
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendValue(
                io.heapy.grogu.time.temporal.ChronoField.YEAR,
                4,
                10,
                SignStyle.EXCEEDS_PAD,
            )
            .appendLiteral('-')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()

        listOf(-1, 1, 9_999, 12_024).forEach { year ->
            val javaDate = java.time.LocalDate.of(year, 3, 1)
            val groguDate = io.heapy.grogu.time.LocalDate.of(year, 3, 1)
            val javaText = javaFormatter.format(javaDate)
            val groguText = groguFormatter.format(groguDate)

            assertEquals(javaText, groguText)
            assertEquals(
                java.time.LocalDate.parse(javaText, javaFormatter).toString(),
                groguFormatter.parse(groguText, io.heapy.grogu.time.LocalDate::from).toString(),
            )
        }
    }

    @Test
    fun adjacentNumericValueParsingMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValue(
                java.time.temporal.ChronoField.YEAR,
                4,
                10,
                java.time.format.SignStyle.EXCEEDS_PAD,
            )
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendValue(
                io.heapy.grogu.time.temporal.ChronoField.YEAR,
                4,
                10,
                SignStyle.EXCEEDS_PAD,
            )
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()

        listOf("20240301", "+120240301").forEach { text ->
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter).toString(),
                groguFormatter.parse(text, io.heapy.grogu.time.LocalDate::from).toString(),
            )
        }
    }
}
