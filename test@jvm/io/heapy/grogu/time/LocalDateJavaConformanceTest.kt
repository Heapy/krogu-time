package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateJavaConformanceTest {
    @Test
    fun valueAndEpochBehaviorMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -1_000_000L,
            -719_528L,
            -1L,
            0L,
            1L,
            11_016L,
            19_782L,
            1_000_000L,
            365_241_780_471L,
        )

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)
            assertEquals(javaDate.year, date.year)
            assertEquals(javaDate.monthValue, date.monthValue)
            assertEquals(javaDate.dayOfMonth, date.dayOfMonth)
            assertEquals(javaDate.dayOfYear, date.dayOfYear)
            assertEquals(javaDate.dayOfWeek.value, date.dayOfWeek.value)
            assertEquals(javaDate.isLeapYear, date.isLeapYear)
            assertEquals(javaDate.lengthOfMonth(), date.lengthOfMonth())
            assertEquals(javaDate.lengthOfYear(), date.lengthOfYear())
            assertEquals(javaDate.toEpochDay(), date.toEpochDay())
            assertEquals(javaDate.toString(), date.toString())
        }
    }
}
