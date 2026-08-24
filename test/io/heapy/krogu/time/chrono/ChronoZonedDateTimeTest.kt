package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.ZonedDateTime
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalQuery
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertEquals(7_200, zoned.get(ChronoField.OFFSET_SECONDS))
        assertEquals(2024, zoned.get(ChronoField.YEAR))
        val exception = assertFailsWith<UnsupportedTemporalTypeException> {
            zoned.get(ChronoField.INSTANT_SECONDS)
        }
        assertEquals(
            "Invalid field 'InstantSeconds' for get() method, use getLong() instead",
            exception.message,
        )
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

    @Test
    fun localDateTimeAdjustersUseTheSharedTimelineAcrossChronologies() {
        val base = MinguoDate.of(113, 1, 1)
            .atTime(LocalTime.NOON)
            .atZone(ZoneOffset.UTC)
        val thaiDateTime = ThaiBuddhistDate.of(2567, 2, 29)
            .atTime(LocalTime.of(13, 14, 15, 123_456_789))

        val adjusted = base.with(thaiDateTime)

        assertSame(MinguoChronology, adjusted.chronology)
        assertEquals(MinguoDate.of(113, 2, 29), adjusted.date)
        assertEquals(LocalTime.of(13, 14, 15, 123_456_789), adjusted.time)
        assertSame(ZoneOffset.UTC, adjusted.zone)
    }

    @Test
    fun covariantDefaultsReturnZonedDateTimesFromTheSameChronology() {
        val delegate = MinguoDate.of(113, 1, 31)
            .atTime(LocalTime.NOON)
            .atZone(ZoneOffset.ofHours(2))
        val dateTime = DefaultMethodZonedDateTime(delegate)
        val period = MinguoChronology.period(0, 1, 1)
        val dayFifteen = TemporalAdjuster { temporal ->
            temporal.with(ChronoField.DAY_OF_MONTH, 15)
        }

        assertEquals(MinguoDate.of(113, 1, 15), dateTime.with(dayFifteen).date)
        assertEquals(MinguoDate.of(113, 3, 1), dateTime.plus(period).date)
        assertEquals(MinguoDate.of(112, 12, 30), dateTime.minus(period).date)
        assertEquals(
            MinguoDate.of(113, 1, 30),
            dateTime.minus(1, ChronoUnit.DAYS).date,
        )
        assertFailsWith<ClassCastException> {
            dateTime.with(TemporalAdjuster {
                ZonedDateTime.of(
                    LocalDateTime.of(2024, 1, 15, 12, 0),
                    ZoneOffset.UTC,
                )
            })
        }
    }

    private class DefaultMethodZonedDateTime(
        private val delegate: ChronoZonedDateTime<MinguoDate>,
    ) : ChronoZonedDateTime<MinguoDate> by delegate {
        override fun with(adjuster: TemporalAdjuster): ChronoZonedDateTime<MinguoDate> =
            super<ChronoZonedDateTime>.with(adjuster)

        override fun plus(amount: TemporalAmount): ChronoZonedDateTime<MinguoDate> =
            super<ChronoZonedDateTime>.plus(amount)

        override fun minus(amount: TemporalAmount): ChronoZonedDateTime<MinguoDate> =
            super<ChronoZonedDateTime>.minus(amount)

        override fun minus(
            amountToSubtract: Long,
            unit: TemporalUnit,
        ): ChronoZonedDateTime<MinguoDate> =
            super<ChronoZonedDateTime>.minus(amountToSubtract, unit)
    }

    private class LocalZonedAccessor(
        private val date: LocalDate,
        private val time: LocalTime,
        private val zone: ZoneOffset,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField?): Boolean =
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
