package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChronoZonedDateTimeTest {
    @Test
    fun zonedDateTimeImplementsTheGenericChronologyContract() {
        val local = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val offset = ZoneOffset.ofHours(2)
        val zoned: ChronoZonedDateTime<LocalDate> = ZonedDateTime.of(local, offset)

        assertSame(local, zoned.dateTime)
        assertSame(local, zoned.toLocalDateTime())
        assertSame(local.date, zoned.date)
        assertSame(local.time, zoned.time)
        assertSame(IsoChronology, zoned.chronology)
        assertSame(offset, zoned.offset)
        assertSame(offset, zoned.zone)
        assertTrue(zoned.isSupported(ChronoField.INSTANT_SECONDS))
        assertTrue(zoned.isSupported(ChronoUnit.NANOS))
        assertSame(IsoChronology, zoned.query(TemporalQueries.chronology()))
        assertSame(offset, zoned.query(TemporalQueries.offset()))
        assertSame(offset, zoned.query(TemporalQueries.zone()))
        assertEquals(ChronoUnit.NANOS, zoned.query(TemporalQueries.precision()))
    }

    @Test
    fun convertsChangesZonesAndOrdersOnTheInstantTimeline() {
        val local: ChronoLocalDateTime<LocalDate> = LocalDateTime.of(2024, 2, 29, 12, 0)
        val first = local.atZone(ZoneOffset.ofHours(2))
        val sameInstant: ChronoZonedDateTime<*> =
            ZonedDateTime.of(LocalDateTime.of(2024, 2, 29, 10, 0), ZoneOffset.UTC)
        val later: ChronoZonedDateTime<*> = sameInstant.plus(1, ChronoUnit.NANOS)

        assertSame(first, ChronoZonedDateTime.from(first))
        assertEquals("2024-02-29T10:00:00Z", first.toInstant().toString())
        assertEquals(first.toInstant().epochSecond, first.toEpochSecond())
        assertTrue(first.isEqual(sameInstant))
        assertNotEquals(0, first.compareTo(sameInstant))
        assertEquals(0, ChronoZonedDateTime.timeLineOrder().compare(first, sameInstant))
        assertTrue(later.isAfter(first))
        assertTrue(first.isBefore(later))
        assertEquals(first, first.withZoneSameLocal(ZoneOffset.ofHours(2)))
        assertEquals(sameInstant, first.withZoneSameInstant(ZoneOffset.UTC))
    }

    @Test
    fun chronologyFactoriesExposeGenericZonedDateTimes() {
        val chronology: Chronology = IsoChronology
        val instant = Instant.parse("2024-02-29T10:00:00.123456789Z")
        val expected = ZonedDateTime.ofInstant(instant, ZoneOffset.ofHours(2))

        assertEquals(expected, chronology.zonedDateTime(instant, ZoneOffset.ofHours(2)))
        assertEquals(expected, chronology.zonedDateTime(expected))
        assertEquals(
            ZonedDateTime.of(LocalDateTime.of(2024, 2, 29, 12, 0), ZoneOffset.ofHours(2)),
            chronology.zonedDateTime(
                LocalZonedAccessor(
                    LocalDate.of(2024, 2, 29),
                    LocalTime.NOON,
                    ZoneOffset.ofHours(2),
                ),
            ),
        )
        assertEquals(expected, ChronoZonedDateTime.from(expected))
    }

    private class LocalZonedAccessor(
        private val date: LocalDate,
        private val time: LocalTime,
        private val zone: ZoneOffset,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.EPOCH_DAY || field === ChronoField.NANO_OF_DAY

        override fun getLong(field: TemporalField): Long = when (field) {
            ChronoField.EPOCH_DAY -> date.toEpochDay()
            ChronoField.NANO_OF_DAY -> time.toNanoOfDay()
            else -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }

        override fun <R> query(query: TemporalQuery<R>): R {
            val result: Any = when (query) {
                TemporalQueries.chronology() -> IsoChronology
                TemporalQueries.zone(), TemporalQueries.zoneId() -> zone
                else -> return super<TemporalAccessor>.query(query)
            }
            @Suppress("UNCHECKED_CAST")
            return result as R
        }
    }
}
