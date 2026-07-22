package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import java.time.LocalTime as JavaLocalTime
import java.time.MonthDay as JavaMonthDay
import java.time.Year as JavaYear
import java.time.YearMonth as JavaYearMonth
import java.time.chrono.HijrahDate as JavaHijrahDate
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoPartialConversionJavaConformanceTest {
    @Test
    fun nonIsoTemporalConversionMatchesJavaTime() {
        listOf(
            Triple(1400, 1, 1),
            Triple(1445, 9, 1),
            Triple(1500, 12, 30),
        ).forEach { (year, month, day) ->
            val javaDate = JavaHijrahDate.of(year, month, day)
            val date = HijrahDate.of(year, month, day)

            assertEquals(JavaYear.from(javaDate).toString(), Year.from(date).toString())
            assertEquals(JavaYearMonth.from(javaDate).toString(), YearMonth.from(date).toString())
            assertEquals(JavaMonthDay.from(javaDate).toString(), MonthDay.from(date).toString())
        }
    }

    @Test
    fun unsupportedTemporalFailuresMatchJavaTime() {
        val operations = listOf<Pair<() -> Any?, () -> Any?>>(
            { JavaYear.from(JavaLocalTime.NOON) } to { Year.from(LocalTime.NOON) },
            { JavaYearMonth.from(JavaLocalTime.NOON) } to { YearMonth.from(LocalTime.NOON) },
            { JavaMonthDay.from(JavaLocalTime.NOON) } to { MonthDay.from(LocalTime.NOON) },
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
