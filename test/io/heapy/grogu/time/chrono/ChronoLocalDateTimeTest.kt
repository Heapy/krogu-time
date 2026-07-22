package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChronoLocalDateTimeTest {
    @Test
    fun localDateTimeImplementsTheGenericChronologyDateTimeContract() {
        val date = LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(13, 14, 15, 123_456_789)
        val dateTime: ChronoLocalDateTime<LocalDate> = LocalDateTime.of(date, time)

        assertSame(date, dateTime.date)
        assertSame(time, dateTime.time)
        assertSame(date, dateTime.toLocalDate())
        assertSame(time, dateTime.toLocalTime())
        assertSame(IsoChronology, dateTime.chronology)
        assertTrue(dateTime.isSupported(ChronoField.EPOCH_DAY))
        assertTrue(dateTime.isSupported(ChronoUnit.NANOS))
        assertSame(IsoChronology, dateTime.query(TemporalQueries.chronology()))
        assertSame(time, dateTime.query(TemporalQueries.localTime()))
        assertEquals(ChronoUnit.NANOS, dateTime.query(TemporalQueries.precision()))
        assertNull(dateTime.query(TemporalQueries.zone()))
    }

    @Test
    fun composesConvertsAdjustsAndOrdersOnTheSharedLocalTimeline() {
        val date: ChronoLocalDate = LocalDate.of(2024, 2, 29)
        val first = date.atTime(LocalTime.of(23, 59, 59, 999_999_999))
        val second: ChronoLocalDateTime<*> = LocalDateTime.of(2024, 3, 1, 0, 0)
        val offset = ZoneOffset.ofHours(2)

        assertSame(first, ChronoLocalDateTime.from(first))
        assertEquals(second, first.plus(1, ChronoUnit.NANOS))
        assertEquals(first, second.minus(1, ChronoUnit.NANOS))
        assertTrue(first.isBefore(second))
        assertTrue(second.isAfter(first))
        assertTrue(first.isEqual(LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_999_999)))
        assertTrue(first < second)
        assertEquals(
            listOf(first, second),
            listOf(second, first).sortedWith(ChronoLocalDateTime.timeLineOrder()),
        )
        assertEquals(1_709_243_999L, first.toEpochSecond(offset))
        assertEquals("2024-02-29T21:59:59.999999999Z", first.toInstant(offset).toString())
        assertEquals(
            LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_999_999),
            first.adjustInto(LocalDateTime.of(2000, 1, 1, 12, 0)),
        )
    }

    @Test
    fun chronologyFactoriesExposeGenericLocalDateTimes() {
        val chronology: Chronology = IsoChronology
        val source = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)

        assertEquals(source, chronology.localDateTime(source))
        assertEquals(source, ChronoLocalDateTime.from(source))
    }
}
