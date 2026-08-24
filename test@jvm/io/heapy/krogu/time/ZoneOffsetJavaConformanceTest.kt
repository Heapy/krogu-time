package io.heapy.krogu.time

import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.ValueRange
import java.time.ZoneOffset as JavaZoneOffset
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import java.time.temporal.Temporal as JavaTemporal
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalField as JavaTemporalField
import java.time.temporal.TemporalUnit as JavaTemporalUnit
import java.time.temporal.ValueRange as JavaValueRange
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

    @Test
    fun customFieldIntValidationMatchesJavaTime() {
        val javaResult = runCatching {
            JavaZoneOffset.ofHoursMinutesSeconds(1, 2, 3).get(JavaWideRangeField)
        }
        val kotlinResult = runCatching {
            ZoneOffset.ofHoursMinutesSeconds(1, 2, 3).get(WideRangeField)
        }
        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
        assertEquals(
            javaResult.exceptionOrNull()?.message,
            kotlinResult.exceptionOrNull()?.message,
        )
    }

    private object WideRangeField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.SECONDS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        override val isDateBased: Boolean = false
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = temporal is ZoneOffset

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long =
            (temporal as ZoneOffset).totalSeconds.toLong()

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideOffset"
    }

    private object JavaWideRangeField : JavaTemporalField {
        override fun getBaseUnit(): JavaTemporalUnit = JavaChronoUnit.SECONDS

        override fun getRangeUnit(): JavaTemporalUnit = JavaChronoUnit.FOREVER

        override fun range(): JavaValueRange = JavaValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)

        override fun isDateBased(): Boolean = false

        override fun isTimeBased(): Boolean = false

        override fun isSupportedBy(temporal: JavaTemporalAccessor): Boolean =
            temporal is JavaZoneOffset

        override fun rangeRefinedBy(temporal: JavaTemporalAccessor): JavaValueRange = range()

        override fun getFrom(temporal: JavaTemporalAccessor): Long =
            (temporal as JavaZoneOffset).totalSeconds.toLong()

        override fun <R : JavaTemporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideOffset"
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
