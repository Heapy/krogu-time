package io.heapy.grogu.time

import java.time.Year as JavaYear
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class YearJavaConformanceTest {
    @Test
    fun coreValueFieldAndArithmeticBehaviorMatchesJavaTime() {
        val values = listOf(Year.MIN_VALUE, -400, -1, 0, 1, 1900, 2000, 2024, Year.MAX_VALUE)
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)
        val units = listOf(
            ChronoUnit.YEARS,
            ChronoUnit.DECADES,
            ChronoUnit.CENTURIES,
            ChronoUnit.MILLENNIA,
            ChronoUnit.ERAS,
            ChronoUnit.MONTHS,
        )

        values.forEach { value ->
            val year = Year.of(value)
            val javaYear = JavaYear.of(value)
            assertEquals(javaYear.isLeap, year.isLeap)
            assertEquals(javaYear.length(), year.length)
            assertEquals(javaYear.toString(), year.toString())

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                assertEquals(javaYear.isSupported(javaField), year.isSupported(field))
                assertSameOutcome(
                    javaOperation = { javaYear.range(javaField).toString() },
                    kotlinOperation = { year.range(field).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaYear.getLong(javaField) },
                    kotlinOperation = { year.getLong(field) },
                )
            }

            units.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaYear.isSupported(javaUnit), year.isSupported(unit))
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaYear.plus(amount, javaUnit).toString() },
                        kotlinOperation = { year.plus(amount, unit).toString() },
                        context = "year=$value unit=$unit amount=$amount",
                    )
                }
            }
        }
    }

    @Test
    fun staticLeapYearRuleMatchesJavaTime() {
        listOf(Long.MIN_VALUE, -400L, -1L, 0L, 1L, 1900L, 2000L, Long.MAX_VALUE).forEach { year ->
            assertEquals(JavaYear.isLeap(year), Year.isLeap(year))
        }
    }

    @Test
    fun parsingAndAtDayMatchJavaTime() {
        val inputs = listOf(
            "",
            "1",
            "001",
            "0000",
            "2024",
            "-0001",
            "+10000",
            "-999999999",
            "+999999999",
            "02024",
            "+2024",
            "-001",
            "-0000",
            "10000",
            "+00000",
            "+1000000000",
            "-1000000000",
            "+5294967295",
            "+12345678901",
            "-12345678901",
            "2024x",
            "２０２４",
        )
        inputs.forEach { input ->
            val javaResult = runCatching { JavaYear.parse(input).toString() }
            val kotlinResult = runCatching { Year.parse(input).toString() }
            assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), input)
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                input,
            )
            val javaErrorIndex = (javaResult.exceptionOrNull() as? JavaDateTimeParseException)
                ?.errorIndex
            val kotlinErrorIndex = (kotlinResult.exceptionOrNull()
                as? io.heapy.grogu.time.format.DateTimeParseException)?.errorIndex
            assertEquals(javaErrorIndex, kotlinErrorIndex, input)
        }

        listOf(Year.MIN_VALUE, -1, 0, 1900, 2000, 2023, 2024, Year.MAX_VALUE)
            .forEach { value ->
                val javaYear = JavaYear.of(value)
                val year = Year.of(value)
                listOf(-1, 0, 1, 59, 60, 365, 366, 367).forEach { day ->
                    assertSameOutcome(
                        javaOperation = { javaYear.atDay(day).toString() },
                        kotlinOperation = { year.atDay(day).toString() },
                        context = "$value day $day",
                    )
                }
            }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String? = null,
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
