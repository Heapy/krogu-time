package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodTest {
    @Test
    fun createsCanonicalDateBasedAmounts() {
        assertEquals(Period.ZERO, Period.of(0, 0, 0))
        assertEquals(Period.of(2, 0, 0), Period.ofYears(2))
        assertEquals(Period.of(0, 3, 0), Period.ofMonths(3))
        assertEquals(Period.of(0, 0, 28), Period.ofWeeks(4))
        assertEquals(Period.of(0, 0, 5), Period.ofDays(5))
        assertFailsWith<ArithmeticException> { Period.ofWeeks(Int.MAX_VALUE) }

        val period = Period.of(1, -2, 3)
        assertEquals(1, period.years)
        assertEquals(-2, period.months)
        assertEquals(3, period.days)
        assertFalse(period.isZero)
        assertTrue(period.isNegative)
        assertTrue(Period.ZERO.isZero)
        assertFalse(Period.ZERO.isNegative)
    }

    @Test
    fun exposesOnlyYearsMonthsAndDaysAsTemporalUnits() {
        val period = Period.of(1, 2, 3)
        assertEquals(
            listOf(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS),
            period.units,
        )
        assertEquals(1, period.get(ChronoUnit.YEARS))
        assertEquals(2, period.get(ChronoUnit.MONTHS))
        assertEquals(3, period.get(ChronoUnit.DAYS))
        assertFailsWith<UnsupportedTemporalTypeException> { period.get(ChronoUnit.WEEKS) }
        assertEquals(period, Period.from(period))
        assertEquals(period, Period.from(ThreePartAmount))
        assertFailsWith<DateTimeException> { Period.from(Duration.ofDays(1)) }
    }

    @Test
    fun replacesAddsAndSubtractsComponentsWithoutImplicitNormalization() {
        val period = Period.of(1, 2, 3)
        assertEquals(Period.of(4, 2, 3), period.withYears(4))
        assertEquals(Period.of(1, 5, 3), period.withMonths(5))
        assertEquals(Period.of(1, 2, 6), period.withDays(6))
        assertEquals(Period.of(5, 7, 9), period.plus(Period.of(4, 5, 6)))
        assertEquals(Period.of(-3, -3, -3), period.minus(Period.of(4, 5, 6)))
        assertEquals(Period.of(3, 2, 3), period.plusYears(2))
        assertEquals(Period.of(1, 4, 3), period.plusMonths(2))
        assertEquals(Period.of(1, 2, 5), period.plusDays(2))
        assertEquals(Period.of(-1, 2, 3), period.minusYears(2))
        assertEquals(Period.of(1, 0, 3), period.minusMonths(2))
        assertEquals(Period.of(1, 2, 1), period.minusDays(2))
        assertFailsWith<ArithmeticException> { Period.ofYears(Int.MAX_VALUE).plusYears(1) }
        assertFailsWith<ArithmeticException> { Period.ofDays(Int.MIN_VALUE).minusDays(1) }
    }

    @Test
    fun scalesNormalizesAndFormatsComponents() {
        val period = Period.of(1, -25, 3)
        assertEquals(Period.of(2, -50, 6), period.multipliedBy(2))
        assertEquals(Period.of(-1, 25, -3), period.negated())
        assertEquals(Period.of(-1, -1, 3), period.normalized())
        assertEquals(-13, period.toTotalMonths())
        assertEquals("P1Y-25M3D", period.toString())
        assertEquals("P0D", Period.ZERO.toString())
        assertFailsWith<ArithmeticException> { Period.ofYears(Int.MIN_VALUE).negated() }
    }

    @Test
    fun addsToAndSubtractsFromTemporalsUsingCombinedMonthsThenDays() {
        val date = LocalDate.of(2024, 1, 31)
        val period = Period.of(1, 1, 1)
        assertEquals(LocalDate.of(2025, 3, 1), period.addTo(date))
        assertEquals(LocalDate.of(2022, 12, 30), period.subtractFrom(date))
        assertEquals(LocalDate.of(2025, 3, 1), date.plus(period))
        assertEquals(LocalDate.of(2022, 12, 30), date.minus(period))
    }

    private object ThreePartAmount : TemporalAmount {
        override val units: List<TemporalUnit> =
            listOf(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS)

        override fun get(unit: TemporalUnit): Long = when (unit) {
            ChronoUnit.YEARS -> 1
            ChronoUnit.MONTHS -> 2
            ChronoUnit.DAYS -> 3
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }

        override fun addTo(temporal: Temporal): Temporal =
            temporal.plus(1, ChronoUnit.YEARS)
                .plus(2, ChronoUnit.MONTHS)
                .plus(3, ChronoUnit.DAYS)

        override fun subtractFrom(temporal: Temporal): Temporal =
            temporal.minus(1, ChronoUnit.YEARS)
                .minus(2, ChronoUnit.MONTHS)
                .minus(3, ChronoUnit.DAYS)
    }
}
