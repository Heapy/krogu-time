package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsoEraTest {
    @Test
    fun valuesMapBceAndCeToIsoNumbers() {
        assertEquals(listOf(IsoEra.BCE, IsoEra.CE), IsoEra.entries)
        assertEquals(0, IsoEra.BCE.value)
        assertEquals(1, IsoEra.CE.value)
        assertEquals(IsoEra.BCE, IsoEra.of(0))
        assertEquals(IsoEra.CE, IsoEra.of(1))
        assertFailsWith<DateTimeException> { IsoEra.of(-1) }
        assertFailsWith<DateTimeException> { IsoEra.of(2) }
    }

    @Test
    fun eraDefaultsExposeOnlyTheEraChronoField() {
        assertTrue(IsoEra.CE.isSupported(ChronoField.ERA))
        assertFalse(IsoEra.CE.isSupported(ChronoField.YEAR_OF_ERA))
        assertEquals(ValueRange.of(0, 1), IsoEra.CE.range(ChronoField.ERA))
        assertEquals(1, IsoEra.CE.get(ChronoField.ERA))
        assertEquals(1, IsoEra.CE.getLong(ChronoField.ERA))
        assertFailsWith<UnsupportedTemporalTypeException> {
            IsoEra.CE.getLong(ChronoField.YEAR_OF_ERA)
        }
    }

    @Test
    fun adjustsTheEraFieldOnATemporal() {
        assertEquals(
            EraRecordingTemporal(IsoEra.BCE.value.toLong()),
            IsoEra.BCE.adjustInto(EraRecordingTemporal()),
        )
    }

    private data class EraRecordingTemporal(
        val era: Long? = null,
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.ERA

        override fun isSupported(unit: TemporalUnit?): Boolean = false

        override fun getLong(field: TemporalField): Long =
            era ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (field === ChronoField.ERA) {
                copy(era = newValue)
            } else {
                throw UnsupportedTemporalTypeException("Unsupported field: $field")
            }

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
