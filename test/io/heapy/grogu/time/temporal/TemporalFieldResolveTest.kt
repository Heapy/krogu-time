package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.format.ResolverStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TemporalFieldResolveTest {
    @Test
    fun defaultsToNoResolutionWithoutChangingFields() {
        val field = NonResolvingField
        val values = mutableMapOf<TemporalField, Long>(field to 42)

        assertNull(field.resolve(values, LocalDate.EPOCH, ResolverStyle.SMART))
        assertEquals(mapOf<TemporalField, Long>(field to 42L), values)
    }

    @Test
    fun resolvesContinuousDayFieldsThroughTheParsedChronology() {
        val cases = listOf(
            JulianFields.JULIAN_DAY to 2_440_588L,
            JulianFields.MODIFIED_JULIAN_DAY to 40_587L,
            JulianFields.RATA_DIE to 719_163L,
        )

        cases.forEach { (field, value) ->
            val values = mutableMapOf<TemporalField, Long>(field to value)
            val resolved = field.resolve(values, LocalDate.EPOCH, ResolverStyle.SMART)

            assertEquals(LocalDate.EPOCH, LocalDate.from(requireNotNull(resolved)), field.toString())
            assertFalse(field in values)
        }
    }

    @Test
    fun resolvesIsoQuarterAndWeekFieldSets() {
        val quarterValues = mutableMapOf<TemporalField, Long>(
            ChronoField.YEAR to 2024,
            IsoFields.QUARTER_OF_YEAR to 1,
            IsoFields.DAY_OF_QUARTER to 60,
        )
        val quarterDate = IsoFields.DAY_OF_QUARTER.resolve(
            quarterValues,
            LocalDate.EPOCH,
            ResolverStyle.STRICT,
        )
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.from(requireNotNull(quarterDate)))
        assertEquals(emptyMap(), quarterValues)

        val weekValues = mutableMapOf<TemporalField, Long>(
            IsoFields.WEEK_BASED_YEAR to 2020,
            IsoFields.WEEK_OF_WEEK_BASED_YEAR to 53,
            ChronoField.DAY_OF_WEEK to 5,
        )
        val weekDate = IsoFields.WEEK_OF_WEEK_BASED_YEAR.resolve(
            weekValues,
            LocalDate.EPOCH,
            ResolverStyle.STRICT,
        )
        assertEquals(LocalDate.of(2021, 1, 1), LocalDate.from(requireNotNull(weekDate)))
        assertEquals(emptyMap(), weekValues)
    }

    private object NonResolvingField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(0, 100)
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = false

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = 0

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal
    }
}
