package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import java.time.chrono.ThaiBuddhistChronology as JavaThaiBuddhistChronology
import java.time.chrono.ThaiBuddhistDate as JavaThaiBuddhistDate
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ThaiBuddhistChronologyJavaConformanceTest {
    @Test
    fun datesFieldsRangesArithmeticAndPeriodsMatchJavaTime() {
        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(
                JavaThaiBuddhistChronology.INSTANCE.range(javaField).toString(),
                ThaiBuddhistChronology.range(field).toString(),
                field.name,
            )
        }

        listOf(
            intArrayOf(-999_999_456, 1, 1),
            intArrayOf(-1, 12, 31),
            intArrayOf(0, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(2_567, 2, 29),
            intArrayOf(1_000_000_542, 12, 31),
        ).forEach { (year, month, day) ->
            val expected = JavaThaiBuddhistDate.of(year, month, day)
            val actual = ThaiBuddhistDate.of(year, month, day)
            assertEquals(expected.toString(), actual.toString())
            assertEquals(expected.toEpochDay(), actual.toEpochDay())
            assertEquals(expected.hashCode(), actual.hashCode())
            ChronoField.entries.filter(ChronoField::isDateBased).forEach fieldLoop@{ field ->
                val javaField = JavaChronoField.valueOf(field.name)
                if (!expected.isSupported(javaField)) return@fieldLoop
                assertEquals(expected.getLong(javaField), actual.getLong(field), "$actual $field")
                assertEquals(expected.range(javaField).toString(), actual.range(field).toString(), "$actual $field")
            }
        }

        val expected = JavaThaiBuddhistDate.of(2_567, 2, 29)
        val actual = ThaiBuddhistDate.from(LocalDate.of(2024, 2, 29))
        listOf(-400L, -1L, 0L, 1L, 400L).forEach { amount ->
            ChronoUnit.entries.filter(ChronoUnit::isDateBased).filterNot { it === ChronoUnit.FOREVER }
                .forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertEquals(
                        javaOutcome { expected.plus(amount, javaUnit).toString() },
                        groguOutcome { actual.plus(amount, unit).toString() },
                        "$amount $unit",
                    )
                }
        }

        val expectedEnd = JavaThaiBuddhistDate.of(2_569, 1, 30)
        val actualEnd = ThaiBuddhistDate.of(2_569, 1, 30)
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

    private fun javaOutcome(block: () -> String): String =
        runCatching(block).fold(
            onSuccess = { it },
            onFailure = { it.javaClass.name },
        )

    private fun groguOutcome(block: () -> String): String =
        runCatching(block).fold(
            onSuccess = { it },
            onFailure = {
                if (it is io.heapy.grogu.time.DateTimeException) {
                    "java.time.DateTimeException"
                } else {
                    it.javaClass.name
                }
            },
        )
}
