package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.YearMonth as JavaYearMonth
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class YearMonthJavaConformanceTest {
    private val values = listOf(
        YearMonth.of(Year.MIN_VALUE, 1),
        YearMonth.of(-1, 12),
        YearMonth.of(0, 1),
        YearMonth.of(1900, 2),
        YearMonth.of(2000, 2),
        YearMonth.of(2024, 2),
        YearMonth.of(Year.MAX_VALUE, 12),
    )

    @Test
    fun valueFieldAndCalendarBehaviorMatchesJavaTime() {
        values.forEach { yearMonth ->
            val javaYearMonth = yearMonth.toJava()
            assertEquals(javaYearMonth.year, yearMonth.year)
            assertEquals(javaYearMonth.monthValue, yearMonth.monthValue)
            assertEquals(javaYearMonth.month.value, yearMonth.month.value)
            assertEquals(javaYearMonth.isLeapYear, yearMonth.isLeapYear)
            assertEquals(javaYearMonth.lengthOfMonth(), yearMonth.lengthOfMonth())
            assertEquals(javaYearMonth.lengthOfYear(), yearMonth.lengthOfYear())
            assertEquals(javaYearMonth.toString(), yearMonth.toString())
            assertEquals(javaYearMonth.hashCode(), yearMonth.hashCode())

            listOf(-1, 0, 1, 28, 29, 30, 31, 32).forEach { day ->
                assertEquals(javaYearMonth.isValidDay(day), yearMonth.isValidDay(day))
                assertSameOutcome(
                    javaOperation = { javaYearMonth.atDay(day).toString() },
                    kotlinOperation = { yearMonth.atDay(day).toString() },
                    context = "$yearMonth day=$day",
                )
            }
            assertEquals(javaYearMonth.atEndOfMonth().toString(), yearMonth.atEndOfMonth().toString())

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                assertEquals(javaYearMonth.isSupported(javaField), yearMonth.isSupported(field), field.toString())
                assertSameOutcome(
                    javaOperation = { javaYearMonth.range(javaField).toString() },
                    kotlinOperation = { yearMonth.range(field).toString() },
                    context = "$yearMonth field=$field range",
                )
                assertSameOutcome(
                    javaOperation = { javaYearMonth.getLong(javaField) },
                    kotlinOperation = { yearMonth.getLong(field) },
                    context = "$yearMonth field=$field getLong",
                )
                val javaResult = runCatching { javaYearMonth.get(javaField) }
                val kotlinResult = runCatching { yearMonth.get(field) }
                assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), field.toString())
                assertEquals(
                    javaResult.exceptionOrNull()?.javaClass?.simpleName,
                    kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                    field.toString(),
                )
                assertEquals(
                    javaResult.exceptionOrNull()?.message,
                    kotlinResult.exceptionOrNull()?.message,
                    field.toString(),
                )
            }
        }
    }

    @Test
    fun replacementArithmeticAndDifferencesMatchJavaTime() {
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)
        values.forEach { yearMonth ->
            val javaYearMonth = yearMonth.toJava()
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val candidates = listOf(field.range.minimum, 0, 1, field.range.maximum).distinct()
                candidates.forEach { value ->
                    assertSameOutcome(
                        javaOperation = { javaYearMonth.with(javaField, value).toString() },
                        kotlinOperation = { yearMonth.with(field, value).toString() },
                        context = "$yearMonth field=$field value=$value",
                    )
                }
            }
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaYearMonth.isSupported(javaUnit), yearMonth.isSupported(unit), unit.toString())
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaYearMonth.plus(amount, javaUnit).toString() },
                        kotlinOperation = { yearMonth.plus(amount, unit).toString() },
                        context = "plus $yearMonth unit=$unit amount=$amount",
                    )
                    assertSameOutcome(
                        javaOperation = { javaYearMonth.minus(amount, javaUnit).toString() },
                        kotlinOperation = { yearMonth.minus(amount, unit).toString() },
                        context = "minus $yearMonth unit=$unit amount=$amount",
                    )
                }
            }
        }

        values.forEach { start ->
            values.forEach { end ->
                ChronoUnit.entries.forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertSameOutcome(
                        javaOperation = { start.toJava().until(end.toJava(), javaUnit) },
                        kotlinOperation = { start.until(end, unit) },
                        context = "$start until $end unit=$unit",
                    )
                }
            }
        }
    }

    @Test
    fun adjustmentComparisonAndYearIntegrationMatchJavaTime() {
        values.forEach { yearMonth ->
            val javaTarget = JavaLocalDate.of(2023, 1, 31)
            val target = LocalDate.of(2023, 1, 31)
            assertEquals(
                yearMonth.toJava().adjustInto(javaTarget).toString(),
                yearMonth.adjustInto(target).toString(),
            )
            assertEquals(
                java.time.Year.of(yearMonth.year).atMonth(yearMonth.monthValue).toString(),
                Year.of(yearMonth.year).atMonth(yearMonth.monthValue).toString(),
            )
        }

        values.forEach { first ->
            values.forEach { second ->
                assertEquals(first.toJava().compareTo(second.toJava()), first.compareTo(second))
                assertEquals(first.toJava().isAfter(second.toJava()), first.isAfter(second))
                assertEquals(first.toJava().isBefore(second.toJava()), first.isBefore(second))
            }
        }
    }

    @Test
    fun defaultParsingMatchesJavaTime() {
        val inputs = listOf(
            "",
            "0000-01",
            "2024-02",
            "-0001-12",
            "+10000-03",
            "-999999999-01",
            "+999999999-12",
            "2024",
            "2024-2",
            "2024/02",
            "2024-00",
            "2024-13",
            "+2024-02",
            "10000-02",
            "+1000000000-02",
            "+12345678901-02",
            "2024-02Z",
            "２０２４-０２",
        )
        inputs.forEach { input ->
            val javaResult = runCatching { JavaYearMonth.parse(input).toString() }
            val kotlinResult = runCatching { YearMonth.parse(input).toString() }
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
    }

    private fun YearMonth.toJava(): JavaYearMonth = JavaYearMonth.of(year, monthValue)

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
