package io.heapy.grogu.time

import java.time.ZoneOffset as JavaZoneOffset
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneOffsetJavaConformanceTest {
    @Test
    fun parsingAndFormattingMatchJavaTime() {
        val inputs = listOf(
            "Z",
            "z",
            "",
            "+1",
            "+01",
            "-01",
            "+0130",
            "+01:30",
            "+013015",
            "+01:30:15",
            "+00:00:01",
            "-18:00",
            "+18:00",
            "+18:00:01",
            "+19:00",
            "+1:00",
            "+010",
            "+01:3",
            "UTC",
        )
        inputs.forEach { input ->
            assertSameOutcome(
                javaOperation = { JavaZoneOffset.of(input).let { it.totalSeconds to it.id } },
                kotlinOperation = { ZoneOffset.of(input).let { it.totalSeconds to it.id } },
                context = input,
            )
        }
    }

    @Test
    fun componentFactoriesAndValidationMatchJavaTime() {
        val values = listOf(-60, -19, -18, -1, 0, 1, 18, 19, 60)
        values.forEach { hours ->
            assertSameOutcome(
                javaOperation = { JavaZoneOffset.ofHours(hours).snapshot() },
                kotlinOperation = { ZoneOffset.ofHours(hours).snapshot() },
                context = "hours=$hours",
            )
            values.forEach { minutes ->
                assertSameOutcome(
                    javaOperation = { JavaZoneOffset.ofHoursMinutes(hours, minutes).snapshot() },
                    kotlinOperation = { ZoneOffset.ofHoursMinutes(hours, minutes).snapshot() },
                    context = "hours=$hours minutes=$minutes",
                )
                values.forEach { seconds ->
                    assertSameOutcome(
                        javaOperation = {
                            JavaZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds).snapshot()
                        },
                        kotlinOperation = {
                            ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds).snapshot()
                        },
                        context = "hours=$hours minutes=$minutes seconds=$seconds",
                    )
                }
            }
        }
    }

    @Test
    fun valueFieldAndOrderingBehaviorMatchesJavaTime() {
        val totalSecondsValues = listOf(
            -64_801,
            -64_800,
            -19_800,
            -1,
            0,
            1,
            19_800,
            64_800,
            64_801,
        )
        totalSecondsValues.forEach { totalSeconds ->
            assertSameOutcome(
                javaOperation = { JavaZoneOffset.ofTotalSeconds(totalSeconds).snapshot() },
                kotlinOperation = { ZoneOffset.ofTotalSeconds(totalSeconds).snapshot() },
                context = "totalSeconds=$totalSeconds",
            )
        }

        val offsets = totalSecondsValues.filter { it in -64_800..64_800 }
            .map(ZoneOffset::ofTotalSeconds)
        offsets.forEach { offset ->
            val javaOffset = JavaZoneOffset.ofTotalSeconds(offset.totalSeconds)
            assertEquals(javaOffset.hashCode(), offset.hashCode())
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "offset=$offset field=$field"
                assertEquals(javaOffset.isSupported(javaField), offset.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaOffset.getLong(javaField) },
                    kotlinOperation = { offset.getLong(field) },
                    context = context,
                )
            }
            offsets.forEach { other ->
                assertEquals(
                    javaOffset.compareTo(JavaZoneOffset.ofTotalSeconds(other.totalSeconds)),
                    offset.compareTo(other),
                )
            }
        }
    }

    private fun ZoneOffset.snapshot(): Pair<Int, String> = totalSeconds to id

    private fun JavaZoneOffset.snapshot(): Pair<Int, String> = totalSeconds to id

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
