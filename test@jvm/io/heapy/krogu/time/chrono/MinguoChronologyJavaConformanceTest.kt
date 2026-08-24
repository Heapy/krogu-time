package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import java.time.chrono.MinguoChronology as JavaMinguoChronology
import java.time.chrono.MinguoDate as JavaMinguoDate
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class MinguoChronologyJavaConformanceTest {
    @Test
    fun datesFieldsRangesArithmeticAndPeriodsMatchJavaTime() {
        ChronoField.entries.filter(ChronoField::isDateBased).forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(
                JavaMinguoChronology.INSTANCE.range(javaField).toString(),
                MinguoChronology.range(field).toString(),
                field.name,
            )
        }

        listOf(
            intArrayOf(-1, 12, 31),
            intArrayOf(0, 1, 1),
            intArrayOf(1, 1, 1),
            intArrayOf(113, 2, 29),
            intArrayOf(999_998_088, 12, 31),
        ).forEach { (year, month, day) ->
            val expected = JavaMinguoDate.of(year, month, day)
            val actual = MinguoDate.of(year, month, day)
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

        val expected = JavaMinguoDate.of(113, 2, 29)
        val actual = MinguoDate.from(LocalDate.of(2024, 2, 29))
        listOf(-400L, -1L, 0L, 1L, 400L).forEach { amount ->
            ChronoUnit.entries.filter(ChronoUnit::isDateBased).filterNot { it === ChronoUnit.FOREVER }
                .forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertEquals(
                        expected.plus(amount, javaUnit).toString(),
                        actual.plus(amount, unit).toString(),
                        "$amount $unit",
                    )
                }
        }

        val expectedEnd = JavaMinguoDate.of(115, 1, 30)
        val actualEnd = MinguoDate.of(115, 1, 30)
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
}
