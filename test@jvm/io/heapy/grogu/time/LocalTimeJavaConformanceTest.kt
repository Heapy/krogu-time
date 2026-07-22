package io.heapy.grogu.time

import java.time.LocalTime as JavaLocalTime
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeJavaConformanceTest {
    @Test
    fun foundationBehaviorMatchesJavaTime() {
        val nanoOfDays = listOf(
            0L,
            1L,
            999L,
            1_000L,
            999_999L,
            1_000_000L,
            999_999_999L,
            1_000_000_000L,
            3_723_000_000_004L,
            43_200_000_000_000L,
            86_399_999_999_999L,
        )

        nanoOfDays.forEach { nanoOfDay ->
            val javaTime = JavaLocalTime.ofNanoOfDay(nanoOfDay)
            val time = LocalTime.ofNanoOfDay(nanoOfDay)
            assertEquals(javaTime.hour, time.hour)
            assertEquals(javaTime.minute, time.minute)
            assertEquals(javaTime.second, time.second)
            assertEquals(javaTime.nano, time.nano)
            assertEquals(javaTime.toSecondOfDay(), time.toSecondOfDay())
            assertEquals(javaTime.toNanoOfDay(), time.toNanoOfDay())
            assertEquals(javaTime.toString(), time.toString())
            assertEquals(javaTime.hashCode(), time.hashCode())
        }

        nanoOfDays.forEach { firstNano ->
            val javaFirst = JavaLocalTime.ofNanoOfDay(firstNano)
            val first = LocalTime.ofNanoOfDay(firstNano)
            nanoOfDays.forEach { secondNano ->
                val javaSecond = JavaLocalTime.ofNanoOfDay(secondNano)
                val second = LocalTime.ofNanoOfDay(secondNano)
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
            }
        }
    }

    @Test
    fun factoryValidationMatchesJavaTime() {
        val values = listOf(-1, 0, 1, 23, 24, 59, 60, 999_999_999, 1_000_000_000)
        values.forEach { hour ->
            values.forEach { minute ->
                assertSameOutcome(
                    javaOperation = { JavaLocalTime.of(hour, minute).toString() },
                    kotlinOperation = { LocalTime.of(hour, minute).toString() },
                    context = "hour=$hour minute=$minute",
                )
            }
        }
        listOf(-1L, 0L, 1L, 86_399L, 86_400L).forEach { secondOfDay ->
            assertSameOutcome(
                javaOperation = { JavaLocalTime.ofSecondOfDay(secondOfDay).toString() },
                kotlinOperation = { LocalTime.ofSecondOfDay(secondOfDay).toString() },
                context = "secondOfDay=$secondOfDay",
            )
        }
        listOf(-1L, 0L, 1L, 86_399_999_999_999L, 86_400_000_000_000L).forEach { nanoOfDay ->
            assertSameOutcome(
                javaOperation = { JavaLocalTime.ofNanoOfDay(nanoOfDay).toString() },
                kotlinOperation = { LocalTime.ofNanoOfDay(nanoOfDay).toString() },
                context = "nanoOfDay=$nanoOfDay",
            )
        }
    }

    @Test
    fun temporalFieldBehaviorMatchesJavaTime() {
        val nanoOfDays = listOf(
            0L,
            1L,
            3_723_000_000_004L,
            43_200_000_000_000L,
            86_399_999_999_999L,
        )

        nanoOfDays.forEach { nanoOfDay ->
            val javaTime = JavaLocalTime.ofNanoOfDay(nanoOfDay)
            val time = LocalTime.ofNanoOfDay(nanoOfDay)
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "time=$time field=$field"
                assertEquals(javaTime.isSupported(javaField), time.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaTime.range(javaField).toString() },
                    kotlinOperation = { time.range(field).toString() },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaTime.get(javaField) },
                    kotlinOperation = { time.get(field) },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaTime.getLong(javaField) },
                    kotlinOperation = { time.getLong(field) },
                    context = context,
                )
            }
        }
    }

    @Test
    fun fieldReplacementAndTemporalArithmeticMatchJavaTime() {
        val nanoOfDays = listOf(
            0L,
            1L,
            3_723_000_000_004L,
            47_655_123_456_789L,
            86_399_999_999_999L,
        )

        nanoOfDays.forEach { nanoOfDay ->
            val javaTime = JavaLocalTime.ofNanoOfDay(nanoOfDay)
            val time = LocalTime.ofNanoOfDay(nanoOfDay)
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val values = listOf(
                    field.range.minimum,
                    field.range.maximum,
                    0L,
                    1L,
                    field.range.maximum + 1,
                ).distinct()
                values.forEach { value ->
                    assertSameOutcome(
                        javaOperation = { javaTime.with(javaField, value).toString() },
                        kotlinOperation = { time.with(field, value).toString() },
                        context = "time=$time field=$field value=$value",
                    )
                }
            }
        }

        val amounts = listOf(
            Long.MIN_VALUE,
            -86_400_000_000_001L,
            -1L,
            0L,
            1L,
            86_400_000_000_001L,
            Long.MAX_VALUE,
        )
        nanoOfDays.forEach { nanoOfDay ->
            val javaTime = JavaLocalTime.ofNanoOfDay(nanoOfDay)
            val time = LocalTime.ofNanoOfDay(nanoOfDay)
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                val supportContext = "time=$time unit=$unit"
                assertEquals(javaTime.isSupported(javaUnit), time.isSupported(unit), supportContext)
                amounts.forEach { amount ->
                    val context = "$supportContext amount=$amount"
                    assertSameOutcome(
                        javaOperation = { javaTime.plus(amount, javaUnit).toString() },
                        kotlinOperation = { time.plus(amount, unit).toString() },
                        context = "plus $context",
                    )
                    assertSameOutcome(
                        javaOperation = { javaTime.minus(amount, javaUnit).toString() },
                        kotlinOperation = { time.minus(amount, unit).toString() },
                        context = "minus $context",
                    )
                }
                assertSameOutcome(
                    javaOperation = { javaTime.truncatedTo(javaUnit).toString() },
                    kotlinOperation = { time.truncatedTo(unit).toString() },
                    context = "truncate $supportContext",
                )
            }
        }
    }

    @Test
    fun completeUnitsUntilMatchJavaTime() {
        val nanoOfDays = listOf(
            0L,
            1L,
            3_723_000_000_004L,
            47_655_123_456_789L,
            86_399_999_999_999L,
        )

        nanoOfDays.forEach { startNanoOfDay ->
            val javaStart = JavaLocalTime.ofNanoOfDay(startNanoOfDay)
            val start = LocalTime.ofNanoOfDay(startNanoOfDay)
            nanoOfDays.forEach { endNanoOfDay ->
                val javaEnd = JavaLocalTime.ofNanoOfDay(endNanoOfDay)
                val end = LocalTime.ofNanoOfDay(endNanoOfDay)
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
    fun defaultIsoParsingMatchesJavaTime() {
        val inputs = listOf(
            "",
            "1:02",
            "01:2",
            "01:02",
            "01:02:",
            "01:02:3",
            "01:02:03",
            "01:02:03.",
            "01:02:03.1",
            "01:02:03.000001",
            "01:02:03.123456789",
            "01:02:03.1234567890",
            "24:00",
            "23:60",
            "23:59:60",
            "23:59.1",
            "01:02Z",
            " 01:02",
            "01:02 ",
            "01-02",
            "01:020",
            "01:02:03,1",
            "０１:０２",
        )

        inputs.forEach { input ->
            val javaResult = runCatching { JavaLocalTime.parse(input).toString() }
            val kotlinResult = runCatching { LocalTime.parse(input).toString() }
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
