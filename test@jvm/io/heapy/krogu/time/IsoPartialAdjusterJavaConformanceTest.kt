package io.heapy.krogu.time

import io.heapy.krogu.time.chrono.HijrahDate
import java.time.MonthDay as JavaMonthDay
import java.time.Year as JavaYear
import java.time.YearMonth as JavaYearMonth
import java.time.chrono.HijrahDate as JavaHijrahDate
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoPartialAdjusterJavaConformanceTest {
    @Test
    fun nonIsoAdjustmentFailuresMatchJavaTime() {
        val javaTarget = JavaHijrahDate.of(1445, 9, 1)
        val target = HijrahDate.of(1445, 9, 1)
        val operations = listOf<Pair<() -> Any?, () -> Any?>>(
            { JavaYear.of(1445).adjustInto(javaTarget) } to
                { Year.of(1445).adjustInto(target) },
            { JavaYearMonth.of(1445, 9).adjustInto(javaTarget) } to
                { YearMonth.of(1445, 9).adjustInto(target) },
            { JavaMonthDay.of(2, 29).adjustInto(javaTarget) } to
                { MonthDay.of(2, 29).adjustInto(target) },
        )

        operations.forEach { (javaOperation, kotlinOperation) ->
            val javaResult = runCatching(javaOperation)
            val kotlinResult = runCatching(kotlinOperation)
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            )
        }
    }
}
