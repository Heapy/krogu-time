package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
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

    @Test
    fun temporalFieldBehaviorMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -719_893L,
            -1L,
            0L,
            11_016L,
            19_782L,
            365_241_780_471L,
        )

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "date=$date field=$field"
                assertEquals(javaDate.isSupported(javaField), date.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaDate.range(javaField).toString() },
                    kotlinOperation = { date.range(field).toString() },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaDate.get(javaField) },
                    kotlinOperation = { date.get(field) },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaDate.getLong(javaField) },
                    kotlinOperation = { date.getLong(field) },
                    context = context,
                )
            }
            assertEquals(javaDate.era.value, date.era.value)
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)

        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), context)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }
}
