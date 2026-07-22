package io.heapy.grogu.time

import java.time.Instant as JavaInstant
import java.time.OffsetDateTime as JavaOffsetDateTime
import java.time.ZoneOffset as JavaZoneOffset
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetDateTimeJavaConformanceTest {
    @Test
    fun factoriesFieldsTextConversionsAndOrderingMatchJavaTime() {
        val inputs = listOf(
            "",
            "2024-02-29T13:14:15+05:30",
            "2024-02-29t13:14:15.1234+05:30",
            "2024-02-29T13:14:15Z",
            "2024-02-29T13:14+05",
            "2024-02-29T13:14:15+0530",
            "2024-02-29T24:00:00Z",
            "２０２４-０２-２９T１３:１４Z",
        )
        inputs.forEach { input ->
            assertSameOutcome(
                javaOperation = { JavaOffsetDateTime.parse(input).toString() },
                kotlinOperation = { OffsetDateTime.parse(input).toString() },
                context = input,
            )
        }

        val values = values()
        values.forEach { value ->
            val javaValue = value.toJava()
            assertEquals(javaValue.toString(), value.toString())
            assertEquals(
                javaValue.toLocalDate().atTime(javaValue.toOffsetTime()).toString(),
                value.date.atTime(value.toOffsetTime()).toString(),
            )
            assertEquals(
                javaValue.toOffsetTime().atDate(javaValue.toLocalDate()).toString(),
                value.toOffsetTime().atDate(value.date).toString(),
            )
            assertEquals(javaValue.toEpochSecond(), value.toEpochSecond())
            assertEquals(javaValue.toInstant().toString(), value.toInstant().toString())
            assertEquals(javaValue.hashCode(), value.hashCode())
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "value=$value field=$field"
                assertEquals(javaValue.isSupported(javaField), value.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaValue.getLong(javaField) },
                    kotlinOperation = { value.getLong(field) },
                    context = context,
                )
            }
            values.forEach { other ->
                assertEquals(javaValue.compareTo(other.toJava()), value.compareTo(other))
                assertEquals(javaValue.isAfter(other.toJava()), value.isAfter(other))
                assertEquals(javaValue.isBefore(other.toJava()), value.isBefore(other))
                assertEquals(javaValue.isEqual(other.toJava()), value.isEqual(other))
            }
        }
    }

    @Test
    fun instantFactoriesOffsetChangesArithmeticAndDifferencesMatchJavaTime() {
        val instants = listOf(
            Instant.ofEpochSecond(-1, 999_999_999),
            Instant.EPOCH,
            Instant.ofEpochSecond(1_709_210_096, 123_456_789),
        )
        val offsets = listOf(ZoneOffset.MIN, ZoneOffset.UTC, ZoneOffset.ofHoursMinutes(5, 30), ZoneOffset.MAX)
        instants.forEach { instant ->
            offsets.forEach { offset ->
                assertEquals(
                    JavaOffsetDateTime.ofInstant(
                        JavaInstant.ofEpochSecond(instant.epochSecond, instant.nano.toLong()),
                        JavaZoneOffset.ofTotalSeconds(offset.totalSeconds),
                    ).toString(),
                    OffsetDateTime.ofInstant(instant, offset).toString(),
                )
            }
        }

        val amounts = listOf(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE)
        values().forEach { value ->
            val javaValue = value.toJava()
            offsets.forEach { offset ->
                val javaOffset = JavaZoneOffset.ofTotalSeconds(offset.totalSeconds)
                assertSameOutcome(
                    javaOperation = { javaValue.withOffsetSameInstant(javaOffset).toString() },
                    kotlinOperation = { value.withOffsetSameInstant(offset).toString() },
                    context = "sameInstant value=$value offset=$offset",
                )
            }
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaValue.plus(amount, javaUnit).toString() },
                        kotlinOperation = { value.plus(amount, unit).toString() },
                        context = "plus value=$value unit=$unit amount=$amount",
                    )
                }
                values().forEach { end ->
                    assertSameOutcome(
                        javaOperation = { javaValue.until(end.toJava(), javaUnit) },
                        kotlinOperation = { value.until(end, unit) },
                        context = "until start=$value end=$end unit=$unit",
                    )
                }
            }
        }
    }

    @Test
    fun intFieldAccessMatchesJavaValuesAndMessages() {
        val value = OffsetDateTime.of(
            2024,
            2,
            29,
            13,
            14,
            15,
            123_456_789,
            ZoneOffset.ofHoursMinutes(5, 30),
        )
        val javaValue = value.toJava()

        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            val javaResult = runCatching { javaValue.get(javaField) }
            val kotlinResult = runCatching { value.get(field) }
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

    private fun values(): List<OffsetDateTime> = listOf(
        OffsetDateTime.MIN,
        OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC),
        OffsetDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30)),
        OffsetDateTime.of(2024, 2, 29, 8, 44, 15, 123_456_789, ZoneOffset.UTC),
        OffsetDateTime.MAX,
    )

    private fun OffsetDateTime.toJava(): JavaOffsetDateTime = JavaOffsetDateTime.of(
        year,
        monthValue,
        dayOfMonth,
        hour,
        minute,
        second,
        nano,
        JavaZoneOffset.ofTotalSeconds(offset.totalSeconds),
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
