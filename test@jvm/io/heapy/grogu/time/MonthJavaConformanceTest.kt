package io.heapy.grogu.time

import java.time.Month as JavaMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthJavaConformanceTest {
    @Test
    fun coreBehaviorMatchesJavaTime() {
        val amounts = listOf(
            Long.MIN_VALUE,
            -10_000L,
            -13L,
            -12L,
            -1L,
            0L,
            1L,
            12L,
            13L,
            10_000L,
            Long.MAX_VALUE,
        )

        Month.entries.forEach { month ->
            val javaMonth = JavaMonth.valueOf(month.name)
            assertEquals(javaMonth.value, month.value)
            assertEquals(javaMonth.minLength(), month.minLength())
            assertEquals(javaMonth.maxLength(), month.maxLength())
            assertEquals(javaMonth.firstMonthOfQuarter().name, month.firstMonthOfQuarter().name)

            listOf(false, true).forEach { leapYear ->
                assertEquals(javaMonth.length(leapYear), month.length(leapYear))
                assertEquals(javaMonth.firstDayOfYear(leapYear), month.firstDayOfYear(leapYear))
            }

            amounts.forEach { amount ->
                assertEquals(javaMonth.plus(amount).name, month.plus(amount).name)
                assertEquals(javaMonth.minus(amount).name, month.minus(amount).name)
            }
        }
    }
}
