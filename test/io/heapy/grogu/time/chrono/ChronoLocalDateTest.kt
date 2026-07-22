package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Clock
import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.IsoFields
import io.heapy.grogu.time.temporal.JulianFields
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChronoLocalDateTest {
    @Test
    fun localDateImplementsTheGenericChronologyDateContract() {
        val date: ChronoLocalDate = LocalDate.of(2024, 2, 29)

        assertSame(IsoChronology, date.chronology)
        assertSame(IsoEra.CE, date.era)
        assertTrue(date.isLeapYear)
        assertEquals(29, date.lengthOfMonth())
        assertEquals(366, date.lengthOfYear())
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), date.toEpochDay())
        assertTrue(date.isSupported(ChronoField.EPOCH_DAY))
        assertTrue(date.isSupported(ChronoUnit.DAYS))
        assertSame(IsoChronology, date.query(TemporalQueries.chronology()))
        assertEquals(ChronoUnit.DAYS, date.query(TemporalQueries.precision()))
        assertNull(date.query(TemporalQueries.localTime()))
        assertNull(date.query(TemporalQueries.zone()))
    }

    @Test
    fun convertsAdjustsAndOrdersDatesOnTheSharedEpochTimeline() {
        val first: ChronoLocalDate = LocalDate.of(2024, 1, 1)
        val second: ChronoLocalDate = LocalDate.of(2024, 1, 2)

        assertSame(first, ChronoLocalDate.from(first))
        assertEquals(
            first,
            ChronoLocalDate.from(LocalDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.NOON)),
        )
        assertFailsWith<DateTimeException> { ChronoLocalDate.from(LocalTime.NOON) }
        assertEquals(second, first.plus(1, ChronoUnit.DAYS))
        assertEquals(first, second.minus(1, ChronoUnit.DAYS))
        assertEquals(second, first.with(ChronoField.DAY_OF_MONTH, 2))
        assertEquals(first, first.adjustInto(LocalDate.of(2000, 6, 15)))
        assertTrue(second.isAfter(first))
        assertTrue(first.isBefore(second))
        assertTrue(first.isEqual(LocalDate.of(2024, 1, 1)))
        assertFalse(first.isEqual(second))
        assertTrue(first < second)
        assertEquals(
            listOf(first, second),
            listOf(second, first).sortedWith(ChronoLocalDate.timeLineOrder()),
        )
    }

    @Test
    fun chronologyFactoriesExposeGenericDates() {
        val chronology: Chronology = IsoChronology
        val fixed = Clock.fixed(
            Instant.parse("2024-02-29T23:30:00Z"),
            ZoneOffset.ofHours(2),
        )

        assertEquals(LocalDate.of(2024, 2, 29), chronology.date(2024, 2, 29))
        assertEquals(LocalDate.of(0, 1, 1), chronology.date(IsoEra.BCE, 1, 1, 1))
        assertEquals(LocalDate.of(2024, 2, 29), chronology.dateYearDay(2024, 60))
        assertEquals(LocalDate.of(1970, 1, 1), chronology.dateEpochDay(0))
        assertEquals(LocalDate.of(2024, 2, 29), chronology.date(LocalDate.of(2024, 2, 29)))
        assertEquals(LocalDate.of(2024, 3, 1), chronology.dateNow(fixed))
    }

    @Test
    fun defaultAtTimeFactoryCombinesCustomDatesWithLocalTime() {
        val date = DefaultMethodDate(MinguoDate.of(113, 2, 29))
        val time = LocalTime.of(12, 30, 45, 123_456_789)
        val dateTime = date.atTime(time)

        assertSame(date, dateTime.date)
        assertSame(time, dateTime.time)
    }

    @Test
    fun covariantDefaultsReturnDatesFromTheSameChronology() {
        val date = DefaultMethodDate(MinguoDate.of(113, 1, 31))
        val period = MinguoChronology.period(0, 1, 1)
        val julianDay = date.getLong(JulianFields.JULIAN_DAY)
        val dayFifteen = TemporalAdjuster { temporal ->
            temporal.with(ChronoField.DAY_OF_MONTH, 15)
        }

        assertEquals(MinguoDate.of(113, 1, 15), date.with(dayFifteen))
        assertEquals(
            MinguoDate.of(113, 2, 1),
            date.with(JulianFields.JULIAN_DAY, julianDay + 1),
        )
        assertEquals(MinguoDate.of(113, 3, 1), date.plus(period))
        assertEquals(MinguoDate.of(113, 4, 30), date.plus(1, IsoFields.QUARTER_YEARS))
        assertEquals(MinguoDate.of(112, 12, 30), date.minus(period))
        assertEquals(MinguoDate.of(113, 1, 30), date.minus(1, ChronoUnit.DAYS))
        assertFailsWith<ClassCastException> {
            date.with(TemporalAdjuster { LocalDate.EPOCH })
        }
    }

    private class DefaultMethodDate(
        private val delegate: MinguoDate,
    ) : ChronoLocalDate by delegate {
        override fun with(adjuster: TemporalAdjuster): ChronoLocalDate =
            super<ChronoLocalDate>.with(adjuster)

        override fun with(field: TemporalField, newValue: Long): ChronoLocalDate =
            if (field is ChronoField) {
                delegate.with(field, newValue)
            } else {
                super<ChronoLocalDate>.with(field, newValue)
            }

        override fun plus(amount: TemporalAmount): ChronoLocalDate =
            super<ChronoLocalDate>.plus(amount)

        override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoLocalDate =
            if (unit is ChronoUnit) {
                delegate.plus(amountToAdd, unit)
            } else {
                super<ChronoLocalDate>.plus(amountToAdd, unit)
            }

        override fun minus(amount: TemporalAmount): ChronoLocalDate =
            super<ChronoLocalDate>.minus(amount)

        override fun minus(amountToSubtract: Long, unit: TemporalUnit): ChronoLocalDate =
            super<ChronoLocalDate>.minus(amountToSubtract, unit)

        override fun atTime(localTime: LocalTime): ChronoLocalDateTime<*> =
            super<ChronoLocalDate>.atTime(localTime)
    }
}
