package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import java.time.chrono.JapaneseChronology as JavaJapaneseChronology
import java.time.chrono.JapaneseDate as JavaJapaneseDate
import java.time.chrono.JapaneseEra as JavaJapaneseEra
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class JapaneseChronologyJavaConformanceTest {
    @Test
    fun chronologyDatesTransitionsFieldsAndArithmeticMatchJavaTime() {
        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(
                javaOutcome { JavaJapaneseChronology.INSTANCE.range(javaField).toString() },
                kroguOutcome { JapaneseChronology.range(field).toString() },
                field.name,
            )
        }

        listOf(
            intArrayOf(1873, 1, 1),
            intArrayOf(1912, 7, 29),
            intArrayOf(1912, 7, 30),
            intArrayOf(1926, 12, 24),
            intArrayOf(1926, 12, 25),
            intArrayOf(1989, 1, 7),
            intArrayOf(1989, 1, 8),
            intArrayOf(2019, 4, 30),
            intArrayOf(2019, 5, 1),
            intArrayOf(2024, 2, 29),
        ).forEach { (year, month, day) ->
            val expected = JavaJapaneseDate.of(year, month, day)
            val actual = JapaneseDate.of(year, month, day)
            assertEquals(expected.toString(), actual.toString())
            assertEquals(expected.toEpochDay(), actual.toEpochDay())
            assertEquals(expected.hashCode(), actual.hashCode())
            assertEquals(expected.lengthOfMonth(), actual.lengthOfMonth())
            assertEquals(expected.lengthOfYear(), actual.lengthOfYear())
            ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                assertEquals(expected.isSupported(javaField), actual.isSupported(field), "$actual $field support")
                if (expected.isSupported(javaField)) {
                    assertEquals(expected.getLong(javaField), actual.getLong(field), "$actual $field")
                    assertEquals(expected.range(javaField).toString(), actual.range(field).toString(), "$actual $field")
                }
            }
        }

        val expected = JavaJapaneseDate.of(2019, 4, 30)
        val actual = JapaneseDate.from(LocalDate.of(2019, 4, 30))
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

        JavaJapaneseEra.values().zip(JapaneseEra.values()).forEach { (javaEra, era) ->
            listOf(1, 2, 10).forEach { yearOfEra ->
                assertEquals(
                    javaOutcome { JavaJapaneseChronology.INSTANCE.prolepticYear(javaEra, yearOfEra).toString() },
                    kroguOutcome { JapaneseChronology.prolepticYear(era, yearOfEra).toString() },
                    "$era $yearOfEra",
                )
            }
        }

        assertEquals(
            JavaJapaneseChronology.INSTANCE.dateYearDay(JavaJapaneseEra.HEISEI, 1, 1).toString(),
            JapaneseChronology.dateYearDay(JapaneseEra.HEISEI, 1, 1).toString(),
        )
        val expectedEnd = JavaJapaneseDate.of(2021, 1, 30)
        val actualEnd = JapaneseDate.of(2021, 1, 30)
        assertEquals(expected.until(expectedEnd).toString(), actual.until(actualEnd).toString())
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
