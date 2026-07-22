package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.temporal.TemporalQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DateTimeFormatterResolverStyleTest {
    @Test
    fun exposesDefaultsAndCopiesResolverStyleImmutably() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        assertSame(ResolverStyle.STRICT, formatter.resolverStyle)
        assertSame(ResolverStyle.SMART, DateTimeFormatter.RFC_1123_DATE_TIME.resolverStyle)
        assertSame(formatter, formatter.withResolverStyle(ResolverStyle.STRICT))

        val smart = formatter.withResolverStyle(ResolverStyle.SMART)
        assertNotSame(formatter, smart)
        assertSame(ResolverStyle.SMART, smart.resolverStyle)
        assertSame(ResolverStyle.STRICT, formatter.resolverStyle)
        assertEquals(formatter.toString(), smart.toString())
    }

    @Test
    fun resolvesDatesAccordingToTheConfiguredStyle() {
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_LOCAL_DATE.parse("2019-02-29")
        }
        assertEquals(
            LocalDate.of(2019, 2, 28),
            DateTimeFormatter.ISO_LOCAL_DATE
                .withResolverStyle(ResolverStyle.SMART)
                .parse("2019-02-29", TemporalQuery(LocalDate::from)),
        )
        assertEquals(
            LocalDate.of(2020, 4, 9),
            DateTimeFormatter.ISO_LOCAL_DATE
                .withResolverStyle(ResolverStyle.LENIENT)
                .parse("2019-15-40", TemporalQuery(LocalDate::from)),
        )
    }

    @Test
    fun resolvesTimesAndRetainsTimeOnlyExcessDays() {
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.ISO_LOCAL_TIME.parse("24:00")
        }

        val smart = DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(ResolverStyle.SMART)
            .parse("24:00")
        assertEquals(LocalTime.MIDNIGHT, LocalTime.from(smart))
        assertEquals(Period.ofDays(1), smart.query(DateTimeFormatter.parsedExcessDays()))

        val lenient = DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(ResolverStyle.LENIENT)
            .parse("25:61")
        assertEquals(LocalTime.of(2, 1), LocalTime.from(lenient))
        assertEquals(Period.ofDays(1), lenient.query(DateTimeFormatter.parsedExcessDays()))
    }

    @Test
    fun appliesExcessDaysToDateTimeAndOffsetDateTimeResults() {
        assertEquals(
            LocalDateTime.of(2019, 3, 1, 0, 0),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withResolverStyle(ResolverStyle.SMART)
                .parse(
                    "2019-02-29T24:00",
                    TemporalQuery(LocalDateTime::from),
                ),
        )
        assertEquals(
            OffsetDateTime.parse("2019-03-01T00:00+02:00"),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withResolverStyle(ResolverStyle.SMART)
                .parse(
                    "2019-02-29T24:00+02:00",
                    TemporalQuery(OffsetDateTime::from),
                ),
        )
    }

    @Test
    fun resolvesAlternativeIsoDatesAndRfcDateTimes() {
        assertEquals(
            LocalDate.of(2020, 1, 1),
            DateTimeFormatter.ISO_ORDINAL_DATE
                .withResolverStyle(ResolverStyle.LENIENT)
                .parse("2019-366", TemporalQuery(LocalDate::from)),
        )
        assertEquals(
            LocalDate.of(2019, 12, 30),
            DateTimeFormatter.ISO_WEEK_DATE
                .withResolverStyle(ResolverStyle.SMART)
                .parse("2019-W53-1", TemporalQuery(LocalDate::from)),
        )
        assertEquals(
            LocalDate.of(2019, 2, 28),
            DateTimeFormatter.BASIC_ISO_DATE
                .withResolverStyle(ResolverStyle.SMART)
                .parse("20190229", TemporalQuery(LocalDate::from)),
        )
        assertEquals(
            OffsetDateTime.parse("2019-02-28T12:00Z"),
            DateTimeFormatter.RFC_1123_DATE_TIME.parse(
                "29 Feb 2019 12:00 GMT",
                TemporalQuery(OffsetDateTime::from),
            ),
        )
        assertFailsWith<DateTimeParseException> {
            DateTimeFormatter.RFC_1123_DATE_TIME
                .withResolverStyle(ResolverStyle.STRICT)
                .parse("29 Feb 2019 12:00 GMT")
        }
    }
}
