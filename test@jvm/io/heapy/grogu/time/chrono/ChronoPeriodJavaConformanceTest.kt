package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.Period
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoPeriodJavaConformanceTest {
    @Test
    fun isoPeriodGenericBehaviorMatchesJavaTime() {
        val javaPeriod: java.time.chrono.ChronoPeriod = java.time.Period.of(1, -2, 3)
        val period: ChronoPeriod = Period.of(1, -2, 3)

        assertEquals(javaPeriod.chronology.toString(), period.chronology.toString())
        assertEquals(javaPeriod.isZero, period.isZero)
        assertEquals(javaPeriod.isNegative, period.isNegative)
        assertEquals(
            javaPeriod.plus(java.time.Period.of(2, 4, 6)).toString(),
            period.plus(Period.of(2, 4, 6)).toString(),
        )
        assertEquals(javaPeriod.multipliedBy(-2).toString(), period.multipliedBy(-2).toString())
        assertEquals(javaPeriod.normalized().toString(), period.normalized().toString())

        val javaStart: java.time.chrono.ChronoLocalDate = java.time.LocalDate.of(2023, 1, 31)
        val javaEnd: java.time.chrono.ChronoLocalDate = java.time.LocalDate.of(2024, 3, 2)
        val start: ChronoLocalDate = LocalDate.of(2023, 1, 31)
        val end: ChronoLocalDate = LocalDate.of(2024, 3, 2)
        assertEquals(
            java.time.chrono.ChronoPeriod.between(javaStart, javaEnd).toString(),
            ChronoPeriod.between(start, end).toString(),
        )
    }
}
