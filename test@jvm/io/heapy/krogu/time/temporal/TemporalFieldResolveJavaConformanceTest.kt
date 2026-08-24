package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.format.ResolverStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalFieldResolveJavaConformanceTest {
    @Test
    fun julianAndIsoFieldResolutionMatchesJavaTime() {
        val javaJulianValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
            java.time.temporal.JulianFields.MODIFIED_JULIAN_DAY to 40_587,
        )
        val kroguJulianValues = mutableMapOf<TemporalField, Long>(
            JulianFields.MODIFIED_JULIAN_DAY to 40_587,
        )
        val javaJulian = java.time.temporal.JulianFields.MODIFIED_JULIAN_DAY.resolve(
            javaJulianValues,
            java.time.LocalDate.EPOCH,
            java.time.format.ResolverStyle.SMART,
        )
        val kroguJulian = JulianFields.MODIFIED_JULIAN_DAY.resolve(
            kroguJulianValues,
            LocalDate.EPOCH,
            ResolverStyle.SMART,
        )
        assertEquals(java.time.LocalDate.from(javaJulian).toString(), LocalDate.from(requireNotNull(kroguJulian)).toString())
        assertEquals(javaJulianValues.size, kroguJulianValues.size)

        val javaQuarterValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
            java.time.temporal.ChronoField.YEAR to 2024,
            java.time.temporal.IsoFields.QUARTER_OF_YEAR to 1,
            java.time.temporal.IsoFields.DAY_OF_QUARTER to 60,
        )
        val kroguQuarterValues = mutableMapOf<TemporalField, Long>(
            ChronoField.YEAR to 2024,
            IsoFields.QUARTER_OF_YEAR to 1,
            IsoFields.DAY_OF_QUARTER to 60,
        )
        val javaQuarter = java.time.temporal.IsoFields.DAY_OF_QUARTER.resolve(
            javaQuarterValues,
            java.time.LocalDate.EPOCH,
            java.time.format.ResolverStyle.STRICT,
        )
        val kroguQuarter = IsoFields.DAY_OF_QUARTER.resolve(
            kroguQuarterValues,
            LocalDate.EPOCH,
            ResolverStyle.STRICT,
        )
        assertEquals(java.time.LocalDate.from(javaQuarter).toString(), LocalDate.from(requireNotNull(kroguQuarter)).toString())
        assertEquals(javaQuarterValues.size, kroguQuarterValues.size)
    }
}
