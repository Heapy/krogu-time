package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.MonthDay as JavaMonthDay
import java.time.Year as JavaYear
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthDayJavaConformanceTest {
    @Test
    fun valueFieldAndReplacementBehaviorMatchesJavaTime() {
        val values = listOf(
            MonthDay.of(1, 1),
            MonthDay.of(2, 28),
            MonthDay.of(2, 29),
            MonthDay.of(4, 30),
            MonthDay.of(12, 31),
        )
        values.forEach { monthDay ->
            val javaMonthDay = JavaMonthDay.of(monthDay.monthValue, monthDay.dayOfMonth)
            assertEquals(javaMonthDay.monthValue, monthDay.monthValue)
            assertEquals(javaMonthDay.month.value, monthDay.month.value)
            assertEquals(javaMonthDay.dayOfMonth, monthDay.dayOfMonth)
            assertEquals(javaMonthDay.toString(), monthDay.toString())
            assertEquals(javaMonthDay.hashCode(), monthDay.hashCode())

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                assertEquals(javaMonthDay.isSupported(javaField), monthDay.isSupported(field), field.toString())
                assertSameOutcome(
                    javaOperation = { javaMonthDay.range(javaField).toString() },
                    kotlinOperation = { monthDay.range(field).toString() },
                    context = "$monthDay field=$field",
                )
                assertSameOutcome(
                    javaOperation = { javaMonthDay.getLong(javaField) },
                    kotlinOperation = { monthDay.getLong(field) },
                    context = "$monthDay field=$field",
                )
            }

            (0..13).forEach { month ->
                assertSameOutcome(
                    javaOperation = { javaMonthDay.withMonth(month).toString() },
                    kotlinOperation = { monthDay.withMonth(month).toString() },
                    context = "$monthDay month=$month",
                )
            }
            listOf(0, 1, 28, 29, 30, 31, 32).forEach { day ->
                assertSameOutcome(
                    javaOperation = { javaMonthDay.withDayOfMonth(day).toString() },
                    kotlinOperation = { monthDay.withDayOfMonth(day).toString() },
                    context = "$monthDay day=$day",
                )
            }
        }

        values.forEach { first ->
            values.forEach { second ->
                val javaFirst = JavaMonthDay.of(first.monthValue, first.dayOfMonth)
                val javaSecond = JavaMonthDay.of(second.monthValue, second.dayOfMonth)
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
            }
        }
    }

    @Test
    fun yearValidationDateProductionAndAdjustmentMatchJavaTime() {
        val monthDays = listOf(MonthDay.of(1, 31), MonthDay.of(2, 28), MonthDay.of(2, 29))
        val years = listOf(Year.MIN_VALUE, -1, 0, 1900, 2000, 2023, 2024, Year.MAX_VALUE)
        monthDays.forEach { monthDay ->
            val javaMonthDay = JavaMonthDay.of(monthDay.monthValue, monthDay.dayOfMonth)
            years.forEach { year ->
                assertEquals(javaMonthDay.isValidYear(year), monthDay.isValidYear(year))
                assertEquals(javaMonthDay.atYear(year).toString(), monthDay.atYear(year).toString())
                assertEquals(
                    JavaYear.of(year).isValidMonthDay(javaMonthDay),
                    Year.of(year).isValidMonthDay(monthDay),
                )
                assertEquals(
                    JavaYear.of(year).atMonthDay(javaMonthDay).toString(),
                    Year.of(year).atMonthDay(monthDay).toString(),
                )
            }
        }

        val targetDates = listOf(
            LocalDate.of(2023, 1, 31),
            LocalDate.of(2024, 12, 1),
        )
        monthDays.forEach { monthDay ->
            val javaMonthDay = JavaMonthDay.of(monthDay.monthValue, monthDay.dayOfMonth)
            targetDates.forEach { target ->
                val javaTarget = JavaLocalDate.of(target.year, target.monthValue, target.dayOfMonth)
                assertEquals(
                    javaMonthDay.adjustInto(javaTarget).toString(),
                    monthDay.adjustInto(target).toString(),
                )
            }
        }
    }

    @Test
    fun defaultParsingMatchesJavaTime() {
        val inputs = listOf(
            "",
            "--01-01",
            "--02-29",
            "--12-31",
            "-02-29",
            "02-29",
            "--2-29",
            "--02-9",
            "--02/29",
            "--02-30",
            "--04-31",
            "--13-01",
            "--00-01",
            "--02-29Z",
            "--０２-２９",
        )
        inputs.forEach { input ->
            val javaResult = runCatching { JavaMonthDay.parse(input).toString() }
            val kotlinResult = runCatching { MonthDay.parse(input).toString() }
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
