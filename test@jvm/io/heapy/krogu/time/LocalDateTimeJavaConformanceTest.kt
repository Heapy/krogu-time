package io.heapy.krogu.time

import java.time.LocalDateTime as JavaLocalDateTime
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeJavaConformanceTest {
    @Test
    fun foundationBehaviorMatchesJavaTime() {
        val cases = listOf(
            LocalDateTime.MIN,
            LocalDateTime.of(-1, 1, 1, 0, 0),
            LocalDateTime.of(1970, 1, 1, 0, 0),
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
            LocalDateTime.MAX,
        )

        cases.forEach { dateTime ->
            val javaDateTime = JavaLocalDateTime.of(
                dateTime.year,
                dateTime.monthValue,
                dateTime.dayOfMonth,
                dateTime.hour,
                dateTime.minute,
                dateTime.second,
                dateTime.nano,
            )
            assertEquals(javaDateTime.year, dateTime.year)
            assertEquals(javaDateTime.monthValue, dateTime.monthValue)
            assertEquals(javaDateTime.month.value, dateTime.month.value)
            assertEquals(javaDateTime.dayOfMonth, dateTime.dayOfMonth)
            assertEquals(javaDateTime.dayOfYear, dateTime.dayOfYear)
            assertEquals(javaDateTime.dayOfWeek.value, dateTime.dayOfWeek.value)
            assertEquals(javaDateTime.hour, dateTime.hour)
            assertEquals(javaDateTime.minute, dateTime.minute)
            assertEquals(javaDateTime.second, dateTime.second)
            assertEquals(javaDateTime.nano, dateTime.nano)
            assertEquals(javaDateTime.toString(), dateTime.toString())
            assertEquals(javaDateTime.hashCode(), dateTime.hashCode())
        }

        cases.forEach { first ->
            val javaFirst = JavaLocalDateTime.of(
                first.year,
                first.monthValue,
                first.dayOfMonth,
                first.hour,
                first.minute,
                first.second,
                first.nano,
            )
            cases.forEach { second ->
                val javaSecond = JavaLocalDateTime.of(
                    second.year,
                    second.monthValue,
                    second.dayOfMonth,
                    second.hour,
                    second.minute,
                    second.second,
                    second.nano,
                )
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
                assertEquals(javaFirst.isEqual(javaSecond), first.isEqual(second))
            }
        }
    }

    @Test
    fun defaultIsoParsingMatchesJavaTime() {
        val inputs = listOf(
            "",
            "2024-02-29",
            "2024-02-29T",
            "2024-02-29T00:00",
            "2024-02-29t00:00",
            "2024-02-29T13:14:15",
            "2024-02-29T13:14:15.",
            "2024-02-29T13:14:15.1",
            "2024-02-29T13:14:15.123456789",
            "-0001-01-01T01:02",
            "+10000-01-01T23:59",
            "+999999999-12-31T23:59:59.999999999",
            "2024-02-29 01:02",
            "2024-02-29T1:02",
            "2024-02-29T01:2",
            "2024-02-29T01:02Z",
            "2024-02-29T01:02:03.1234567890",
            "2023-02-29T01:02",
            "2024-13-01T01:02",
            "2024-02-29T24:00",
            "2024-02-29T23:60",
            "2024-02-29T23:59:60",
            "2024-02-29TT01:02",
            "2024-02-29T01:02T",
            "+12345678901-01-01T01:02",
            "２０２４-０２-２９T０１:０２",
        )

        inputs.forEach { input ->
            val javaResult = runCatching { JavaLocalDateTime.parse(input).toString() }
            val kotlinResult = runCatching { LocalDateTime.parse(input).toString() }
            assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), input)
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                input,
            )
            val javaErrorIndex = (javaResult.exceptionOrNull() as? JavaDateTimeParseException)
                ?.errorIndex
            val kotlinErrorIndex = (kotlinResult.exceptionOrNull()
                as? io.heapy.krogu.time.format.DateTimeParseException)?.errorIndex
            assertEquals(javaErrorIndex, kotlinErrorIndex, input)
        }
    }

    @Test
    fun temporalFieldBehaviorMatchesJavaTime() {
        val javaDateTime = JavaLocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val dateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)

        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            val context = field.toString()
            assertEquals(javaDateTime.isSupported(javaField), dateTime.isSupported(field), context)
            assertSameOutcome(
                javaOperation = { javaDateTime.range(javaField).toString() },
                kotlinOperation = { dateTime.range(field).toString() },
                context = context,
            )
            assertSameOutcome(
                javaOperation = { javaDateTime.get(javaField) },
                kotlinOperation = { dateTime.get(field) },
                context = context,
            )
            assertSameOutcome(
                javaOperation = { javaDateTime.getLong(javaField) },
                kotlinOperation = { dateTime.getLong(field) },
                context = context,
            )
        }
    }

    @Test
    fun fieldReplacementAndArithmeticMatchJavaTime() {
        val dateTimes = listOf(
            LocalDateTime.of(-1, 1, 1, 0, 0),
            LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1),
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
            LocalDateTime.of(999_999_998, 12, 31, 23, 59, 59, 999_999_999),
        )
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)

        dateTimes.forEach { dateTime ->
            val javaDateTime = dateTime.toJava()
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                listOf(field.range.minimum, 0L, 1L, field.range.maximum).distinct().forEach { value ->
                    assertSameOutcome(
                        javaOperation = { javaDateTime.with(javaField, value).toString() },
                        kotlinOperation = { dateTime.with(field, value).toString() },
                        context = "time=$dateTime field=$field value=$value",
                    )
                }
            }
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaDateTime.isSupported(javaUnit), dateTime.isSupported(unit), unit.toString())
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaDateTime.plus(amount, javaUnit).toString() },
                        kotlinOperation = { dateTime.plus(amount, unit).toString() },
                        context = "plus time=$dateTime unit=$unit amount=$amount",
                    )
                    assertSameOutcome(
                        javaOperation = { javaDateTime.minus(amount, javaUnit).toString() },
                        kotlinOperation = { dateTime.minus(amount, unit).toString() },
                        context = "minus time=$dateTime unit=$unit amount=$amount",
                    )
                }
                assertSameOutcome(
                    javaOperation = { javaDateTime.truncatedTo(javaUnit).toString() },
                    kotlinOperation = { dateTime.truncatedTo(unit).toString() },
                    context = "truncate time=$dateTime unit=$unit",
                )
            }
        }
    }

    @Test
    fun completeUnitsUntilMatchJavaTime() {
        val dateTimes = listOf(
            LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999),
            LocalDateTime.of(1970, 1, 1, 0, 0),
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
            LocalDateTime.of(2025, 3, 30, 12, 13, 14, 987_654_321),
        )
        dateTimes.forEach { start ->
            dateTimes.forEach { end ->
                ChronoUnit.entries.forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertSameOutcome(
                        javaOperation = { start.toJava().until(end.toJava(), javaUnit) },
                        kotlinOperation = { start.until(end, unit) },
                        context = "start=$start end=$end unit=$unit",
                    )
                }
            }
        }
    }

    private fun LocalDateTime.toJava(): JavaLocalDateTime = JavaLocalDateTime.of(
        year,
        monthValue,
        dayOfMonth,
        hour,
        minute,
        second,
        nano,
    )

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
