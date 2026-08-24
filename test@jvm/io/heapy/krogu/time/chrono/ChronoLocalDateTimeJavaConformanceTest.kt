package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.ValueRange
import java.time.ZoneOffset as JavaZoneOffset
import java.time.chrono.MinguoDate as JavaMinguoDate
import java.time.temporal.ChronoUnit as JavaChronoUnit
import java.time.temporal.Temporal as JavaTemporal
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalField as JavaTemporalField
import java.time.temporal.TemporalUnit as JavaTemporalUnit
import java.time.temporal.ValueRange as JavaValueRange
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoLocalDateTimeJavaConformanceTest {
    @Test
    fun isoGenericDateTimeBehaviorMatchesJavaTime() {
        val javaDateTime: java.time.chrono.ChronoLocalDateTime<*> =
            java.time.LocalDateTime.of(2024, 2, 29, 23, 59, 59, 123_456_789)
        val dateTime: ChronoLocalDateTime<*> =
            LocalDateTime.of(2024, 2, 29, 23, 59, 59, 123_456_789)

        assertEquals(javaDateTime.chronology.toString(), dateTime.chronology.toString())
        assertEquals(javaDateTime.toLocalDate().toString(), dateTime.toLocalDate().toString())
        assertEquals(javaDateTime.toLocalTime().toString(), dateTime.toLocalTime().toString())
        assertEquals(
            javaDateTime.plus(2, JavaChronoUnit.NANOS).toString(),
            dateTime.plus(2, ChronoUnit.NANOS).toString(),
        )
        assertEquals(
            javaDateTime.toEpochSecond(JavaZoneOffset.ofHours(2)),
            dateTime.toEpochSecond(ZoneOffset.ofHours(2)),
        )
        assertEquals(
            javaDateTime.toInstant(JavaZoneOffset.ofHours(2)).toString(),
            dateTime.toInstant(ZoneOffset.ofHours(2)).toString(),
        )

        val javaOther: java.time.chrono.ChronoLocalDateTime<*> =
            java.time.LocalDateTime.of(2024, 3, 1, 0, 0)
        val other: ChronoLocalDateTime<*> = LocalDateTime.of(2024, 3, 1, 0, 0)
        assertEquals(javaDateTime.compareTo(javaOther), dateTime.compareTo(other))
        assertEquals(javaDateTime.isBefore(javaOther), dateTime.isBefore(other))
        assertEquals(javaDateTime.isAfter(javaOther), dateTime.isAfter(other))
        assertEquals(javaDateTime.isEqual(javaOther), dateTime.isEqual(other))
        assertEquals(
            java.time.chrono.ChronoLocalDateTime.timeLineOrder().compare(javaDateTime, javaOther),
            ChronoLocalDateTime.timeLineOrder().compare(dateTime, other),
        )
    }

    @Test
    fun genericFactoriesMatchJavaTime() {
        val javaSource = java.time.LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val source = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)

        assertEquals(
            java.time.chrono.Chronology.of("ISO").localDateTime(javaSource).toString(),
            Chronology.of("ISO").localDateTime(source).toString(),
        )
        assertEquals(
            java.time.chrono.ChronoLocalDateTime.from(javaSource).toString(),
            ChronoLocalDateTime.from(source).toString(),
        )
    }

    @Test
    fun crossChronologyLocalDateTimeAdjustmentMatchesJavaTime() {
        val javaDateTime = JavaMinguoDate.of(113, 3, 30).atTime(java.time.LocalTime.NOON)
        val dateTime = MinguoDate.of(113, 3, 30).atTime(LocalTime.NOON)
        val javaReplacement = java.time.LocalDateTime.of(2024, 4, 15, 1, 2, 3, 4)
        val replacement = LocalDateTime.of(2024, 4, 15, 1, 2, 3, 4)

        assertEquals(
            javaDateTime.with(javaReplacement).toString(),
            dateTime.with(replacement).toString(),
        )
    }

    @Test
    fun implementationCustomFieldIntValidationMatchesJavaTime() {
        val javaDateTime = JavaMinguoDate.of(113, 2, 29).atTime(java.time.LocalTime.NOON)
        val dateTime = MinguoDate.of(113, 2, 29).atTime(LocalTime.NOON)
        val javaResult = runCatching { javaDateTime.get(JavaWideRangeField) }
        val kotlinResult = runCatching { dateTime.get(WideRangeField) }

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
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        override val isDateBased: Boolean = false
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean =
            temporal is ChronoLocalDateTime<*>

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = 113

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideDateTime"
    }

    private object JavaWideRangeField : JavaTemporalField {
        override fun getBaseUnit(): JavaTemporalUnit = JavaChronoUnit.DAYS

        override fun getRangeUnit(): JavaTemporalUnit = JavaChronoUnit.FOREVER

        override fun range(): JavaValueRange = JavaValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)

        override fun isDateBased(): Boolean = false

        override fun isTimeBased(): Boolean = false

        override fun isSupportedBy(temporal: JavaTemporalAccessor): Boolean =
            temporal is java.time.chrono.ChronoLocalDateTime<*>

        override fun rangeRefinedBy(temporal: JavaTemporalAccessor): JavaValueRange = range()

        override fun getFrom(temporal: JavaTemporalAccessor): Long = 113

        override fun <R : JavaTemporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideDateTime"
    }
}
