package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
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

    @Test
    fun fieldReplacementAndArithmeticMatchJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -719_893L,
            -1L,
            0L,
            11_016L,
            19_782L,
            365_241_780_471L,
        )
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val current = runCatching { date.getLong(field) }.getOrDefault(0)
                listOf(Long.MIN_VALUE, -1L, 0L, 1L, 28L, 31L, current, Long.MAX_VALUE)
                    .distinct()
                    .forEach { newValue ->
                        assertSameOutcome(
                            javaOperation = { javaDate.with(javaField, newValue).toString() },
                            kotlinOperation = { date.with(field, newValue).toString() },
                            context = "date=$date field=$field newValue=$newValue",
                        )
                    }
            }

            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaDate.isSupported(javaUnit), date.isSupported(unit), "date=$date unit=$unit")
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaDate.plus(amount, javaUnit).toString() },
                        kotlinOperation = { date.plus(amount, unit).toString() },
                        context = "date=$date unit=$unit amount=$amount",
                    )
                    assertSameOutcome(
                        javaOperation = { javaDate.minus(amount, javaUnit).toString() },
                        kotlinOperation = { date.minus(amount, unit).toString() },
                        context = "date=$date unit=$unit amount=-($amount)",
                    )
                }
            }
        }

        epochDays.forEach { startEpochDay ->
            val javaStart = JavaLocalDate.ofEpochDay(startEpochDay)
            val start = LocalDate.ofEpochDay(startEpochDay)
            epochDays.forEach { endEpochDay ->
                val javaEnd = JavaLocalDate.ofEpochDay(endEpochDay)
                val end = LocalDate.ofEpochDay(endEpochDay)
                ChronoUnit.entries.forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertSameOutcome(
                        javaOperation = { javaStart.until(javaEnd, javaUnit) },
                        kotlinOperation = { start.until(end, unit) },
                        context = "start=$start end=$end unit=$unit",
                    )
                }
            }
        }
    }

    @Test
    fun timelineComparisonMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -1L,
            0L,
            1L,
            19_782L,
            365_241_780_471L,
        )

        epochDays.forEach { firstEpochDay ->
            val javaFirst = JavaLocalDate.ofEpochDay(firstEpochDay)
            val first = LocalDate.ofEpochDay(firstEpochDay)
            epochDays.forEach { secondEpochDay ->
                val javaSecond = JavaLocalDate.ofEpochDay(secondEpochDay)
                val second = LocalDate.ofEpochDay(secondEpochDay)
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
                assertEquals(javaFirst.isEqual(javaSecond), first.isEqual(second))
            }
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
