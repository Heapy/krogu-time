package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterNumericPatternFieldsJavaConformanceTest {
    @Test
    fun numericPatternPrintingAndDescriptionsMatchJavaTime() {
        val javaDateTime = java.time.LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val kroguDateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val patterns = listOf(
            "D", "DD", "DDD", "F",
            "k", "kk", "K", "KK", "h", "hh",
            "A", "AAA", "n", "nnn", "N", "NNN",
        )

        patterns.forEach { pattern ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
            val kroguFormatter = DateTimeFormatter.ofPattern(pattern)

            assertEquals(javaFormatter.toString(), kroguFormatter.toString(), pattern)
            assertEquals(javaFormatter.format(javaDateTime), kroguFormatter.format(kroguDateTime), pattern)
        }
    }

    @Test
    fun ordinalAndAggregateTimeResolutionMatchesJavaTime() {
        listOf("u-D" to "2024-60", "uuuu-DD" to "2024-366", "uuuu-DDD" to "2024-060")
            .forEach { (pattern, text) ->
                val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                val kroguFormatter = DateTimeFormatter.ofPattern(pattern)
                assertEquals(
                    java.time.LocalDate.parse(text, javaFormatter).toString(),
                    kroguFormatter.parse(text, LocalDate::from).toString(),
                    pattern,
                )
            }

        listOf("N" to "47655123456789", "A" to "47655123", "k" to "24")
            .forEach { (pattern, text) ->
                val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                val kroguFormatter = DateTimeFormatter.ofPattern(pattern)
                assertEquals(
                    java.time.LocalTime.parse(text, javaFormatter).toString(),
                    kroguFormatter.parse(text, LocalTime::from).toString(),
                    pattern,
                )
            }
    }

    @Test
    fun aggregateBuilderFieldResolutionMatchesJavaTime() {
        val fields = listOf(
            java.time.temporal.ChronoField.NANO_OF_DAY to ChronoField.NANO_OF_DAY,
            java.time.temporal.ChronoField.MICRO_OF_DAY to ChronoField.MICRO_OF_DAY,
            java.time.temporal.ChronoField.MILLI_OF_DAY to ChronoField.MILLI_OF_DAY,
            java.time.temporal.ChronoField.SECOND_OF_DAY to ChronoField.SECOND_OF_DAY,
            java.time.temporal.ChronoField.MINUTE_OF_DAY to ChronoField.MINUTE_OF_DAY,
            java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY to ChronoField.CLOCK_HOUR_OF_DAY,
        )
        val values = listOf(
            "47655123456789",
            "47655123456",
            "47655123",
            "47655",
            "794",
            "24",
        )

        fields.zip(values).forEach { (fieldPair, text) ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .appendValue(fieldPair.first)
                .toFormatter()
            val kroguFormatter = DateTimeFormatterBuilder()
                .appendValue(fieldPair.second)
                .toFormatter()

            assertEquals(
                java.time.LocalTime.parse(text, javaFormatter).toString(),
                kroguFormatter.parse(text, LocalTime::from).toString(),
                fieldPair.second.toString(),
            )
        }
    }
}
