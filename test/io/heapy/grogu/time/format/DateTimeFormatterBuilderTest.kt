package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderTest {
    @Test
    fun composesPatternsAndLiteralsFluently() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.appendPattern("uuuu-MM-dd"))
        assertSame(builder, builder.appendLiteral('T'))
        assertSame(builder, builder.appendPattern("HH:mm"))
        assertSame(builder, builder.appendLiteral(" o'clock"))

        val formatter = builder.toFormatter()
        val dateTime = LocalDateTime.of(2024, 3, 1, 5, 6)
        assertEquals("2024-03-01T05:06 o'clock", formatter.format(dateTime))
        assertEquals(
            dateTime,
            formatter.parse("2024-03-01T05:06 o'clock", LocalDateTime::from),
        )
    }

    @Test
    fun createsIndependentFormatterSnapshots() {
        val builder = DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd")
        val dateFormatter = builder.toFormatter()
        builder.appendLiteral('T').appendPattern("HH:mm")
        val dateTimeFormatter = builder.toFormatter()
        val dateTime = LocalDateTime.of(2024, 3, 1, 5, 6)

        assertEquals("2024-03-01", dateFormatter.format(dateTime))
        assertEquals("2024-03-01T05:06", dateTimeFormatter.format(dateTime))
    }

    @Test
    fun appendsNumericValuesWithConfiguredWidthsAndSigns() {
        val builder = DateTimeFormatterBuilder()
        assertSame(
            builder,
            builder.appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD),
        )
        builder.appendLiteral('-')
        assertSame(builder, builder.appendValue(ChronoField.MONTH_OF_YEAR, 2))
        builder.appendLiteral('-')
        assertSame(builder, builder.appendValue(ChronoField.DAY_OF_MONTH))

        val formatter = builder.toFormatter()
        val date = LocalDate.of(12_024, 3, 1)
        assertEquals("+12024-03-1", formatter.format(date))
        assertEquals(date, formatter.parse("+12024-03-1", LocalDate::from))
    }

    @Test
    fun retainsUnresolvedNumericFieldsForTemporalQueries() {
        val formatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_YEAR)
            .toFormatter()

        assertEquals(60, formatter.parse("60").getLong(ChronoField.DAY_OF_YEAR))
    }

    @Test
    fun rejectsInvalidNumericWidths() {
        val builder = DateTimeFormatterBuilder()

        assertFailsWith<IllegalArgumentException> {
            builder.appendValue(ChronoField.YEAR, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            builder.appendValue(ChronoField.YEAR, 3, 2, SignStyle.NORMAL)
        }
        assertFailsWith<IllegalArgumentException> {
            builder.appendValue(ChronoField.YEAR, 1, 20, SignStyle.NORMAL)
        }
    }
}
