package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
