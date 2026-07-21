package io.heapy.grogu.time

import java.time.DayOfWeek as JavaDayOfWeek
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DayOfWeekJavaConformanceTest {
    @Test
    fun coreBehaviorMatchesJavaTime() {
        val amounts = listOf(
            Long.MIN_VALUE,
            -10_000L,
            -8L,
            -7L,
            -1L,
            0L,
            1L,
            7L,
            8L,
            10_000L,
            Long.MAX_VALUE,
        )

        DayOfWeek.entries.forEach { day ->
            val javaDay = JavaDayOfWeek.valueOf(day.name)
            assertEquals(javaDay.value, day.value)
            assertEquals(
                javaDay.isSupported(JavaChronoField.DAY_OF_WEEK),
                day.isSupported(ChronoField.DAY_OF_WEEK),
            )
            assertEquals(
                javaDay.getLong(JavaChronoField.DAY_OF_WEEK),
                day.getLong(ChronoField.DAY_OF_WEEK),
            )

            amounts.forEach { amount ->
                assertEquals(javaDay.plus(amount).name, day.plus(amount).name)
                assertEquals(javaDay.minus(amount).name, day.minus(amount).name)
            }
        }
    }
}
