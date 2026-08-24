package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    @Test
    fun isoLocalDateTimeAdjustersPreserveTheTargetChronology() {
        val dateTime = MinguoDate.of(113, 3, 30).atTime(LocalTime.NOON)
        val replacement = LocalDateTime.of(2024, 4, 15, 1, 2, 3, 4)

        assertEquals(
            MinguoDate.of(113, 4, 15).atTime(LocalTime.of(1, 2, 3, 4)),
            dateTime.with(replacement),
        )
    }

    @Test
    fun implementationCustomFieldsUseDirectIntValidation() {
        val dateTime = MinguoDate.of(113, 2, 29).atTime(LocalTime.NOON)
        val exception = assertFailsWith<DateTimeException> {
            dateTime.get(WideRangeField)
        }
        assertFalse(exception is UnsupportedTemporalTypeException)
        assertEquals(
            "Invalid value for WideDateTime " +
                "(valid values -9223372036854775808 - 9223372036854775807): 113",
            exception.message,
        )
    }

    @Test
    fun covariantDefaultsReturnLocalDateTimesFromTheSameChronology() {
        val delegate = MinguoDate.of(113, 1, 31).atTime(LocalTime.NOON)
        val dateTime = DefaultMethodDateTime(delegate)
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
                LocalDateTime.of(2024, 1, 15, 12, 0)
            })
        }
    }

    private class DefaultMethodDateTime(
        private val delegate: ChronoLocalDateTime<MinguoDate>,
    ) : ChronoLocalDateTime<MinguoDate> by delegate {
        override fun with(adjuster: TemporalAdjuster): ChronoLocalDateTime<MinguoDate> =
            super<ChronoLocalDateTime>.with(adjuster)

        override fun plus(amount: TemporalAmount): ChronoLocalDateTime<MinguoDate> =
            super<ChronoLocalDateTime>.plus(amount)

        override fun minus(amount: TemporalAmount): ChronoLocalDateTime<MinguoDate> =
            super<ChronoLocalDateTime>.minus(amount)

        override fun minus(
            amountToSubtract: Long,
            unit: TemporalUnit,
        ): ChronoLocalDateTime<MinguoDate> =
            super<ChronoLocalDateTime>.minus(amountToSubtract, unit)
    }

    private object WideRangeField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        override val isDateBased: Boolean = false
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean =
            temporal is ChronoLocalDateTime<*>

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = 113

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideDateTime"
    }
}
