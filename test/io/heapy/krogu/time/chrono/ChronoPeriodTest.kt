package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Period
import io.heapy.krogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChronoPeriodTest {
    @Test
    fun periodImplementsTheGenericChronologyPeriodContract() {
        val period: ChronoPeriod = Period.of(1, -2, 3)

        assertSame(IsoChronology, period.chronology)
        assertEquals(listOf(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS), period.units)
        assertEquals(1, period.get(ChronoUnit.YEARS))
        assertFalse(period.isZero)
        assertTrue(period.isNegative)
        assertEquals(Period.of(2, 0, 4), period.plus(Period.of(1, 2, 1)))
        assertEquals(Period.of(0, -4, 2), period.minus(Period.of(1, 2, 1)))
        assertEquals(Period.of(2, -4, 6), period.multipliedBy(2))
        assertEquals(Period.of(-1, 2, -3), period.negated())
        assertEquals(Period.of(0, 10, 3), period.normalized())
    }

    @Test
    fun calculatesAndAppliesChronologyAwareDatePeriods() {
        val start: ChronoLocalDate = LocalDate.of(2023, 1, 31)
        val end: ChronoLocalDate = LocalDate.of(2024, 3, 2)
        val period = Period.of(1, 1, 2)

        assertEquals(period, ChronoPeriod.between(start, end))
        assertEquals(period, start.until(end))
        assertEquals(end, period.addTo(start))
        assertEquals(start, period.subtractFrom(end))
        assertEquals(period, IsoChronology.period(1, 1, 2))
        val chronology: Chronology = IsoChronology
        assertEquals(period, chronology.period(1, 1, 2))
    }
}
