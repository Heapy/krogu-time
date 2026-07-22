package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.OffsetTime as JavaOffsetTime
import java.time.ZoneOffset as JavaZoneOffset
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetTimeJavaConformanceTest {
    @Test
    fun valueFieldsParsingAndOrderingMatchJavaTime() {
        val inputs = listOf(
            "",
            "00:00Z",
            "13:14:15+05:30",
            "13:14:15+0530",
            "13:14:15.1234-01:30:15",
            "24:00Z",
            "13:14:15",
            "１３:１４Z",
        )
        inputs.forEach { input ->
            assertSameOutcome(
                javaOperation = { JavaOffsetTime.parse(input).toString() },
                kotlinOperation = { OffsetTime.parse(input).toString() },
                context = input,
            )
        }

        val times = times()
        times.forEach { time ->
            val javaTime = time.toJava()
            assertEquals(javaTime.toString(), time.toString())
            assertEquals(javaTime.hashCode(), time.hashCode())
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "time=$time field=$field"
                assertEquals(javaTime.isSupported(javaField), time.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaTime.getLong(javaField) },
                    kotlinOperation = { time.getLong(field) },
                    context = context,
                )
            }
            times.forEach { other ->
                assertEquals(javaTime.compareTo(other.toJava()), time.compareTo(other))
                assertEquals(javaTime.isAfter(other.toJava()), time.isAfter(other))
                assertEquals(javaTime.isBefore(other.toJava()), time.isBefore(other))
                assertEquals(javaTime.isEqual(other.toJava()), time.isEqual(other))
            }
        }
    }

    @Test
    fun offsetChangesArithmeticAndDifferencesMatchJavaTime() {
        val amounts = listOf(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE)
        times().forEach { time ->
            val javaTime = time.toJava()
            listOf(ZoneOffset.MIN, ZoneOffset.UTC, ZoneOffset.ofHoursMinutes(5, 30), ZoneOffset.MAX)
                .forEach { offset ->
                    val javaOffset = JavaZoneOffset.ofTotalSeconds(offset.totalSeconds)
                    assertEquals(
                        javaTime.withOffsetSameLocal(javaOffset).toString(),
                        time.withOffsetSameLocal(offset).toString(),
                    )
                    assertEquals(
                        javaTime.withOffsetSameInstant(javaOffset).toString(),
                        time.withOffsetSameInstant(offset).toString(),
                    )
                }
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaTime.isSupported(javaUnit), time.isSupported(unit), unit.toString())
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaTime.plus(amount, javaUnit).toString() },
                        kotlinOperation = { time.plus(amount, unit).toString() },
                        context = "plus time=$time unit=$unit amount=$amount",
                    )
                }
                times().forEach { end ->
                    assertSameOutcome(
                        javaOperation = { javaTime.until(end.toJava(), javaUnit) },
                        kotlinOperation = { time.until(end, unit) },
                        context = "until start=$time end=$end unit=$unit",
                    )
                }
            }
            val date = LocalDate.of(2024, 2, 29)
            assertEquals(
                javaTime.toEpochSecond(JavaLocalDate.of(2024, 2, 29)),
                time.toEpochSecond(date),
            )
        }
    }

    private fun times(): List<OffsetTime> = listOf(
        OffsetTime.MIN,
        OffsetTime.of(0, 0, 0, 1, ZoneOffset.UTC),
        OffsetTime.of(12, 34, 56, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30)),
        OffsetTime.of(18, 4, 56, 123_456_789, ZoneOffset.ofHoursMinutes(-1, -30)),
        OffsetTime.MAX,
    )

    private fun OffsetTime.toJava(): JavaOffsetTime = JavaOffsetTime.of(
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
