package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import java.time.chrono.HijrahChronology as JavaHijrahChronology
import java.time.chrono.HijrahDate as JavaHijrahDate
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class HijrahChronologyJavaConformanceTest {
    @Test
    fun chronologyRangesAndEveryConfiguredMonthMatchJavaTime() {
        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(
                JavaHijrahChronology.INSTANCE.range(javaField).toString(),
                HijrahChronology.range(field).toString(),
                field.name,
            )
        }

        for (year in 1_300..1_600) {
            for (month in 1..12) {
                val expectedFirst = JavaHijrahDate.of(year, month, 1)
                val actualFirst = HijrahDate.of(year, month, 1)
                assertDateMatches(expectedFirst, actualFirst)

                val lastDay = expectedFirst.lengthOfMonth()
                val expectedLast = JavaHijrahDate.of(year, month, lastDay)
                val actualLast = HijrahDate.of(year, month, lastDay)
                assertDateMatches(expectedLast, actualLast)
            }
        }
    }

    @Test
    fun factoriesArithmeticAdjustmentsAndPeriodsMatchJavaTime() {
        val expected = JavaHijrahDate.from(java.time.LocalDate.of(2024, 2, 29))
        val actual = HijrahDate.from(LocalDate.of(2024, 2, 29))
        assertDateMatches(expected, actual)

        listOf(-400L, -1L, 0L, 1L, 400L).forEach { amount ->
            ChronoUnit.entries.filter(ChronoUnit::isDateBased).filterNot { it === ChronoUnit.FOREVER }
                .forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertEquals(
                        javaOutcome { expected.plus(amount, javaUnit).toString() },
                        kroguOutcome { actual.plus(amount, unit).toString() },
                        "$amount $unit",
                    )
                }
        }

        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            listOf(field.range.minimum, expected.getLong(javaField), field.range.maximum).forEach { value ->
                assertEquals(
                    javaOutcome { expected.with(javaField, value).toString() },
                    kroguOutcome { actual.with(field, value).toString() },
                    "$field $value",
                )
            }
        }

        val expectedEnd = JavaHijrahDate.of(1_446, 8, 21)
        val actualEnd = HijrahDate.of(1_446, 8, 21)
        assertEquals(expected.until(expectedEnd).toString(), actual.until(actualEnd).toString())
        ChronoUnit.entries.filter(ChronoUnit::isDateBased).filterNot { it === ChronoUnit.FOREVER }
            .forEach { unit ->
                assertEquals(
                    expected.until(expectedEnd, JavaChronoUnit.valueOf(unit.name)),
                    actual.until(actualEnd, unit),
                    unit.name,
                )
            }
    }

    private fun assertDateMatches(expected: JavaHijrahDate, actual: HijrahDate) {
        assertEquals(expected.toString(), actual.toString())
        assertEquals(expected.toEpochDay(), actual.toEpochDay(), expected.toString())
        assertEquals(expected.hashCode(), actual.hashCode(), expected.toString())
        assertEquals(expected.lengthOfMonth(), actual.lengthOfMonth(), expected.toString())
        assertEquals(expected.lengthOfYear(), actual.lengthOfYear(), expected.toString())
        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(expected.getLong(javaField), actual.getLong(field), "$actual $field")
            assertEquals(expected.range(javaField).toString(), actual.range(field).toString(), "$actual $field")
        }
    }

    private fun javaOutcome(block: () -> String): String =
        runCatching(block).fold(
            onSuccess = { it },
            onFailure = { it.javaClass.name },
        )

    private fun kroguOutcome(block: () -> String): String =
        runCatching(block).fold(
            onSuccess = { it },
            onFailure = {
                when (it) {
                    is io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException ->
                        "java.time.temporal.UnsupportedTemporalTypeException"
                    is io.heapy.krogu.time.DateTimeException -> "java.time.DateTimeException"
                    else -> it.javaClass.name
                }
            },
        )
}
