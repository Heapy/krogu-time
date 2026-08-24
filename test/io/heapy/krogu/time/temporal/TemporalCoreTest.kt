package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemporalCoreTest {
    @Test
    fun accessorDefaultsDelegateToTheFieldAndQuery() {
        val temporal = SampleTemporal(4)

        assertTrue(temporal.isSupported(SAMPLE_FIELD))
        assertEquals(ValueRange.of(1, 12), temporal.range(SAMPLE_FIELD))
        assertEquals(4, temporal.get(SAMPLE_FIELD))
        assertEquals(4, temporal.getLong(SAMPLE_FIELD))
        assertEquals("value=4", temporal.query { "value=${it.get(SAMPLE_FIELD)}" })
    }

    @Test
    fun accessorGetRejectsFieldsThatDoNotFitInAnInt() {
        val wideField = object : SampleField(ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)) {
            override fun toString(): String = "WideField"
        }

        val error = assertFailsWith<UnsupportedTemporalTypeException> {
            SampleTemporal(4).get(wideField)
        }
        assertEquals(
            "Invalid field WideField for get() method, use getLong() instead",
            error.message,
        )
    }

    @Test
    fun temporalDefaultsDelegateToAdjustersAmountsAndUnits() {
        val temporal = SampleTemporal(4)
        val amount = SampleAmount(3)

        assertEquals(SampleTemporal(7), temporal.plus(amount))
        assertEquals(SampleTemporal(1), temporal.minus(amount))
        assertEquals(SampleTemporal(9), temporal.with { SampleTemporal(9) })
        assertEquals(SampleTemporal(8), temporal.with(SAMPLE_FIELD, 8))
        assertEquals(SampleTemporal(7), ChronoUnit.DAYS.addTo(temporal, 3))
        assertEquals(6, ChronoUnit.DAYS.between(temporal, SampleTemporal(10)))
    }

    @Test
    fun temporalUnitSupportProbeHandlesUnsupportedUnits() {
        assertTrue(ChronoUnit.DAYS.isSupportedBy(SampleTemporal(1)))
        assertFalse(ChronoUnit.HOURS.isSupportedBy(SampleTemporal(1)))
    }

    @Test
    fun temporalUnitSupportUsesJavaTimeTypeGuaranteesBeforeProbingArithmetic() {
        val dateUnit = RejectingUnit(isDateBased = true, isTimeBased = false)
        val timeUnit = RejectingUnit(isDateBased = false, isTimeBased = true)
        val neitherUnit = RejectingUnit(isDateBased = false, isTimeBased = false)
        val dateTime = LocalDateTime.of(2024, 6, 1, 12, 30)

        assertTrue(timeUnit.isSupportedBy(LocalTime.NOON))
        assertFalse(dateUnit.isSupportedBy(LocalTime.NOON))
        assertTrue(dateUnit.isSupportedBy(LocalDate.EPOCH))
        assertFalse(timeUnit.isSupportedBy(LocalDate.EPOCH))
        assertTrue(neitherUnit.isSupportedBy(dateTime))
        assertTrue(neitherUnit.isSupportedBy(ZonedDateTime.of(dateTime, ZoneOffset.UTC)))
    }

    private data class SampleTemporal(val value: Long) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === SAMPLE_FIELD

        override fun isSupported(unit: TemporalUnit?): Boolean = unit === ChronoUnit.DAYS

        override fun getLong(field: TemporalField): Long = if (isSupported(field)) {
            value
        } else {
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }

        override fun with(field: TemporalField, newValue: Long): Temporal = if (isSupported(field)) {
            copy(value = newValue)
        } else {
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal = if (isSupported(unit)) {
            copy(value = value + amountToAdd)
        } else {
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
            if (!isSupported(unit)) throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
            return (endExclusive as SampleTemporal).value - value
        }
    }

    private open class SampleField(
        override val range: ValueRange,
    ) : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.MONTHS
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = temporal.isSupported(this)

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = temporal.getLong(this)

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            @Suppress("UNCHECKED_CAST")
            return temporal.with(this, newValue) as R
        }
    }

    private class SampleAmount(private val days: Long) : TemporalAmount {
        override val units: List<TemporalUnit> = listOf(ChronoUnit.DAYS)

        override fun get(unit: TemporalUnit): Long = if (unit === ChronoUnit.DAYS) {
            days
        } else {
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }

        override fun addTo(temporal: Temporal): Temporal = temporal.plus(days, ChronoUnit.DAYS)

        override fun subtractFrom(temporal: Temporal): Temporal = temporal.minus(days, ChronoUnit.DAYS)
    }

    private class RejectingUnit(
        override val isDateBased: Boolean,
        override val isTimeBased: Boolean,
    ) : TemporalUnit {
        override val duration: Duration = Duration.ZERO
        override val isDurationEstimated: Boolean = false

        override fun <R : Temporal> addTo(temporal: R, amount: Long): R =
            throw UnsupportedTemporalTypeException("Rejected")

        override fun between(
            temporal1Inclusive: Temporal,
            temporal2Exclusive: Temporal,
        ): Long = throw UnsupportedTemporalTypeException("Rejected")
    }

    private companion object {
        val SAMPLE_FIELD: TemporalField = SampleField(ValueRange.of(1, 12))
    }
}
