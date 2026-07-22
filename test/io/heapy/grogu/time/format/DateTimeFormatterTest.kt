package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.temporal.TemporalQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterTest {
    @Test
    fun formatsAndParsesTheCoreIsoTypes() {
        val date = LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(23, 5, 7, 123_400_000)
        val dateTime = LocalDateTime.of(date, time)
        val instant = Instant.parse("2024-02-29T21:05:07.1234Z")

        assertEquals("2024-02-29", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
        assertEquals("23:05:07.1234", DateTimeFormatter.ISO_LOCAL_TIME.format(time))
        assertEquals(
            "2024-02-29T23:05:07.1234",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime),
        )
        assertEquals("2024-02-29T21:05:07.123400Z", DateTimeFormatter.ISO_INSTANT.format(instant))

        assertEquals(date, LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-02-29")))
        assertEquals(time, LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse("23:05:07.1234")))
        assertEquals(
            dateTime,
            LocalDateTime.from(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse("2024-02-29T23:05:07.1234"),
            ),
        )
        assertEquals(
            instant,
            DateTimeFormatter.ISO_INSTANT.parse(
                "2024-02-29T21:05:07.1234Z",
                TemporalQuery(Instant::from),
            ),
        )
    }

    @Test
    fun formatsIntoAppendablesAndSupportsValueTypeOverloads() {
        val date = LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(12, 30)
        val dateTime = LocalDateTime.of(date, time)
        val instant = Instant.parse("2024-02-29T12:30:00Z")

        val output = StringBuilder("date=")
        DateTimeFormatter.ISO_LOCAL_DATE.formatTo(date, output)
        assertEquals("date=2024-02-29", output.toString())

        assertEquals("2024-02-29", date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        assertEquals("12:30:00", time.format(DateTimeFormatter.ISO_LOCAL_TIME))
        assertEquals("2024-02-29T12:30:00", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        assertEquals("2024-02-29T12:30:00Z", instant.format(DateTimeFormatter.ISO_INSTANT))

        assertEquals(date, LocalDate.parse("2024-02-29", DateTimeFormatter.ISO_LOCAL_DATE))
        assertEquals(time, LocalTime.parse("12:30", DateTimeFormatter.ISO_LOCAL_TIME))
        assertEquals(
            dateTime,
            LocalDateTime.parse("2024-02-29T12:30", DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
        assertEquals(instant, Instant.parse("2024-02-29T12:30:00Z", DateTimeFormatter.ISO_INSTANT))
    }

    @Test
    fun rejectsTemporalsAndTextThatDoNotMatchTheFormatter() {
        assertFailsWith<DateTimeException> {
            DateTimeFormatter.ISO_LOCAL_DATE.format(LocalTime.NOON)
        }
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-02-30")
        }
    }
}
