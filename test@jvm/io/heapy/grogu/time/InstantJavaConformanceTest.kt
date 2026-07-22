package io.heapy.grogu.time

import java.time.Instant as JavaInstant
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class InstantJavaConformanceTest {
    @Test
    fun defaultIsoParsingAndFormattingMatchJavaTime() {
        val inputs = listOf(
            "",
            "1970-01-01",
            "1970-01-01T00:00Z",
            "1970-01-01T00:00:00Z",
            "1970-01-01t00:00:00z",
            "1970-01-01T00:00:00.Z",
            "1970-01-01T00:00:00.1Z",
            "1970-01-01T00:00:00.123456789Z",
            "1970-01-01T00:00:00.1234567890Z",
            "1970-01-01T00:00:00,1Z",
            "1970-01-01T01:00:00+01:00",
            "1970-01-01T00:00:00+01:00:30",
            "1970-01-01T00:00:00+18:00",
            "1970-01-01T00:00:00+18:01",
            "1970-01-01T24:00:00Z",
            "1970-01-01T24:00:00.1Z",
            "1970-01-01T23:59:60Z",
            "1970-01-01T22:59:60Z",
            "-0001-01-01T00:00:00Z",
            "+010000-01-01T00:00:00Z",
            "-1000000000-01-01T00:00:00Z",
            "+1000000000-12-31T23:59:59.999999999Z",
            "+1000000001-01-01T00:00:00Z",
            "2024-02-29T12:34:56.123400000Z",
            "2023-02-29T12:34:56Z",
            "２０２４-０２-２９T１２:３４:５６Z",
        )
        inputs.forEach { input ->
            val javaResult = runCatching { JavaInstant.parse(input).toString() }
            val kotlinResult = runCatching { Instant.parse(input).toString() }
            assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), input)
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                input,
            )
            assertEquals(
                (javaResult.exceptionOrNull() as? JavaDateTimeParseException)?.errorIndex,
                (kotlinResult.exceptionOrNull()
                    as? io.heapy.grogu.time.format.DateTimeParseException)?.errorIndex,
                input,
            )
        }

        instants().forEach { instant -> assertEquals(instant.toJava().toString(), instant.toString()) }
    }

    @Test
    fun formattingMatchesJavaTimeAcrossTheSupportedTimeline() {
        val random = Random(0)
        repeat(2_000) {
            val instant = Instant.ofEpochSecond(
                random.nextLong(Instant.MIN.epochSecond, Instant.MAX.epochSecond),
                random.nextInt(1_000_000_000).toLong(),
            )
            val expected = instant.toJava().toString()
            assertEquals(expected, instant.toString())
            assertEquals(instant.snapshot(), Instant.parse(expected).snapshot())
        }
    }

    @Test
    fun factoriesFieldsAndOrderingMatchJavaTime() {
        val factoryInputs = listOf(
            Long.MIN_VALUE to Long.MIN_VALUE,
            Instant.MIN.epochSecond to -1L,
            -2L to 2_000_000_001L,
            -1L to -1L,
            0L to 0L,
            1L to 999_999_999L,
            Instant.MAX.epochSecond to 999_999_999L,
            Long.MAX_VALUE to Long.MAX_VALUE,
        )
        factoryInputs.forEach { (seconds, nanoAdjustment) ->
            assertSameOutcome(
                javaOperation = { JavaInstant.ofEpochSecond(seconds, nanoAdjustment).snapshot() },
                kotlinOperation = { Instant.ofEpochSecond(seconds, nanoAdjustment).snapshot() },
                context = "seconds=$seconds nanoAdjustment=$nanoAdjustment",
            )
        }

        val instants = instants()
        instants.forEach { instant ->
            val javaInstant = instant.toJava()
            assertEquals(javaInstant.epochSecond, instant.epochSecond)
            assertEquals(javaInstant.nano, instant.nano)
            assertEquals(javaInstant.hashCode(), instant.hashCode())
            assertSameOutcome(
                javaInstant::toEpochMilli,
                instant::toEpochMilli,
                instant.snapshot().toString(),
            )

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "instant=${instant.snapshot()} field=$field"
                assertEquals(javaInstant.isSupported(javaField), instant.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaInstant.range(javaField).toString() },
                    kotlinOperation = { instant.range(field).toString() },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaInstant.get(javaField) },
                    kotlinOperation = { instant.get(field) },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaInstant.getLong(javaField) },
                    kotlinOperation = { instant.getLong(field) },
                    context = context,
                )
            }
        }

        instants.forEach { first ->
            instants.forEach { second ->
                assertEquals(first.toJava().compareTo(second.toJava()), first.compareTo(second))
                assertEquals(first.toJava().isAfter(second.toJava()), first.isAfter(second))
                assertEquals(first.toJava().isBefore(second.toJava()), first.isBefore(second))
            }
        }
    }

    @Test
    fun fieldReplacementArithmeticAndTruncationMatchJavaTime() {
        val amounts = listOf(Long.MIN_VALUE, -1_000_001L, -1L, 0L, 1L, 1_000_001L, Long.MAX_VALUE)
        instants().forEach { instant ->
            val javaInstant = instant.toJava()
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                listOf(field.range.minimum, -1L, 0L, 1L, field.range.maximum).distinct().forEach { value ->
                    assertSameOutcome(
                        javaOperation = { javaInstant.with(javaField, value).snapshot() },
                        kotlinOperation = { instant.with(field, value).snapshot() },
                        context = "with instant=${instant.snapshot()} field=$field value=$value",
                    )
                }
            }
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaInstant.isSupported(javaUnit), instant.isSupported(unit), unit.toString())
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaInstant.plus(amount, javaUnit).snapshot() },
                        kotlinOperation = { instant.plus(amount, unit).snapshot() },
                        context = "plus instant=${instant.snapshot()} unit=$unit amount=$amount",
                    )
                    assertSameOutcome(
                        javaOperation = { javaInstant.minus(amount, javaUnit).snapshot() },
                        kotlinOperation = { instant.minus(amount, unit).snapshot() },
                        context = "minus instant=${instant.snapshot()} unit=$unit amount=$amount",
                    )
                }
                assertSameOutcome(
                    javaOperation = { javaInstant.truncatedTo(javaUnit).snapshot() },
                    kotlinOperation = { instant.truncatedTo(unit).snapshot() },
                    context = "truncate instant=${instant.snapshot()} unit=$unit",
                )
            }
        }
    }

    @Test
    fun completeUnitsAndDurationUntilMatchJavaTime() {
        val instants = instants()
        instants.forEach { start ->
            instants.forEach { end ->
                ChronoUnit.entries.forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertSameOutcome(
                        javaOperation = { start.toJava().until(end.toJava(), javaUnit) },
                        kotlinOperation = { start.until(end, unit) },
                        context = "start=${start.snapshot()} end=${end.snapshot()} unit=$unit",
                    )
                }
                assertSameOutcome(
                    javaOperation = {
                        java.time.Duration.between(start.toJava(), end.toJava())
                            .let { it.seconds to it.nano }
                    },
                    kotlinOperation = { start.until(end).let { it.seconds to it.nano } },
                    context = "duration start=${start.snapshot()} end=${end.snapshot()}",
                )
            }
        }
    }

    @Test
    fun intFieldAccessMatchesJavaValuesAndMessages() {
        val instant = Instant.ofEpochSecond(-2, 123_456_789)
        val javaInstant = instant.toJava()

        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            val javaResult = runCatching { javaInstant.get(javaField) }
            val kotlinResult = runCatching { instant.get(field) }
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

    private fun instants(): List<Instant> = listOf(
        Instant.MIN,
        Instant.ofEpochSecond(-86_401, 999_999_999),
        Instant.ofEpochSecond(-2, 123_456_789),
        Instant.EPOCH,
        Instant.ofEpochSecond(2, 987_654_321),
        Instant.ofEpochSecond(86_401, 1),
        Instant.MAX,
    )

    private fun Instant.toJava(): JavaInstant = JavaInstant.ofEpochSecond(epochSecond, nano.toLong())

    private fun Instant.snapshot(): Pair<Long, Int> = epochSecond to nano

    private fun JavaInstant.snapshot(): Pair<Long, Int> = epochSecond to nano

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
