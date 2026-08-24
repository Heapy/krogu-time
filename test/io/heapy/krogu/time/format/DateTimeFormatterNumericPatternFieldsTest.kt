package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterNumericPatternFieldsTest {
    @Test
    fun formatsAndParsesOrdinalDatePatterns() {
        val date = LocalDate.of(2024, 2, 29)

        listOf(
            "u-D" to "2024-60",
            "uuuu-DD" to "2024-60",
            "uuuu-DDD" to "2024-060",
        ).forEach { (pattern, text) ->
            val formatter = DateTimeFormatter.ofPattern(pattern)

            assertEquals(text, formatter.format(date), pattern)
            assertEquals(date, formatter.parse(text, LocalDate::from), pattern)
        }

        val lastDay = LocalDate.of(2024, 12, 31)
        val variableWidth = DateTimeFormatter.ofPattern("uuuu-DD")
        assertEquals("2024-366", variableWidth.format(lastDay))
        assertEquals(lastDay, variableWidth.parse("2024-366", LocalDate::from))
    }

    @Test
    fun formatsRemainingNumericDateAndTimePatternFields() {
        val dateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val formatter = DateTimeFormatter.ofPattern(
            "D F k kk K KK h hh A n N",
        )

        assertEquals(
            "60 5 13 13 1 01 1 01 47655123 123456789 47655123456789",
            formatter.format(dateTime),
        )
    }

    @Test
    fun resolvesAggregateAndClockTimePatternFields() {
        assertEquals(
            LocalTime.of(13, 14, 15, 123_456_789),
            DateTimeFormatter.ofPattern("N")
                .parse("47655123456789", LocalTime::from),
        )
        assertEquals(
            LocalTime.of(13, 14, 15, 123_000_000),
            DateTimeFormatter.ofPattern("A")
                .parse("47655123", LocalTime::from),
        )
        assertEquals(
            LocalTime.MIDNIGHT,
            DateTimeFormatter.ofPattern("k").parse("24", LocalTime::from),
        )

        val afternoon = DateTimeFormatterBuilder()
            .appendPattern("h:mm:ss.n")
            .parseDefaulting(ChronoField.AMPM_OF_DAY, 1)
            .toFormatter()
        assertEquals(
            LocalTime.of(13, 14, 15, 123_456_789),
            afternoon.parse("1:14:15.123456789", LocalTime::from),
        )
    }

    @Test
    fun resolvesAggregateTimeFieldsAppendedDirectly() {
        val cases = listOf(
            ChronoField.NANO_OF_DAY to ("47655123456789" to LocalTime.of(13, 14, 15, 123_456_789)),
            ChronoField.MICRO_OF_DAY to ("47655123456" to LocalTime.of(13, 14, 15, 123_456_000)),
            ChronoField.MILLI_OF_DAY to ("47655123" to LocalTime.of(13, 14, 15, 123_000_000)),
            ChronoField.SECOND_OF_DAY to ("47655" to LocalTime.of(13, 14, 15)),
            ChronoField.MINUTE_OF_DAY to ("794" to LocalTime.of(13, 14)),
            ChronoField.CLOCK_HOUR_OF_DAY to ("24" to LocalTime.MIDNIGHT),
        )

        cases.forEach { (field, expected) ->
            val formatter = DateTimeFormatterBuilder().appendValue(field).toFormatter()
            assertEquals(expected.second, formatter.parse(expected.first, LocalTime::from), field.toString())
        }
    }

    @Test
    fun detectsConflictingAggregateAndPartialTimeFields() {
        val aggregateConflict = DateTimeFormatterBuilder()
            .appendValue(ChronoField.NANO_OF_DAY)
            .appendLiteral('/')
            .appendValue(ChronoField.HOUR_OF_DAY)
            .toFormatter()
        assertFailsWith<DateTimeParseException> {
            aggregateConflict.parse("47655123456789/12")
        }

        fun partialSecondFormatter(): DateTimeFormatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.MILLI_OF_SECOND)
            .appendLiteral('/')
            .appendValue(ChronoField.MICRO_OF_SECOND)
            .toFormatter()

        assertEquals(
            123_456_000,
            partialSecondFormatter().parse("123/123456").getLong(ChronoField.NANO_OF_SECOND),
        )
        assertFailsWith<DateTimeParseException> {
            partialSecondFormatter().parse("123/456789")
        }
    }

    @Test
    fun exposesJavaCompatiblePatternDescriptions() {
        assertEquals("Value(DayOfYear)", DateTimeFormatter.ofPattern("D").toString())
        assertEquals(
            "Value(DayOfYear,2,3,NOT_NEGATIVE)",
            DateTimeFormatter.ofPattern("DD").toString(),
        )
        assertEquals("Value(DayOfYear,3)", DateTimeFormatter.ofPattern("DDD").toString())
        assertEquals("Value(AlignedWeekOfMonth)", DateTimeFormatter.ofPattern("F").toString())
    }

    @Test
    fun rejectsInvalidNumericPatternWidths() {
        listOf("DDDD", "FF", "kkk", "KKK", "hhh", "A".repeat(20), "n".repeat(20), "N".repeat(20))
            .forEach { pattern ->
                assertFailsWith<IllegalArgumentException>(pattern) {
                    DateTimeFormatter.ofPattern(pattern)
                }
            }
    }
}
