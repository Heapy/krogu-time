package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalQueries
import java.time.chrono.HijrahEra as JavaHijrahEra
import java.time.chrono.JapaneseEra as JavaJapaneseEra
import java.time.chrono.MinguoEra as JavaMinguoEra
import java.time.chrono.ThaiBuddhistEra as JavaThaiBuddhistEra
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.TemporalQueries as JavaTemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class AlternateEraJavaConformanceTest {
    @Test
    fun valuesNamesFieldsAndRangesMatchJavaTime() {
        MinguoEra.entries.zip(JavaMinguoEra.entries).forEach { (actual, expected) ->
            assertPrecisionMatches(expected, actual)
            assertEquals(expected.name, actual.name)
            assertEquals(expected.value, actual.value)
            assertEquals(expected.getLong(JavaChronoField.ERA), actual.getLong(ChronoField.ERA))
            assertEquals(expected.range(JavaChronoField.ERA).toString(), actual.range(ChronoField.ERA).toString())
        }
        ThaiBuddhistEra.entries.zip(JavaThaiBuddhistEra.entries).forEach { (actual, expected) ->
            assertPrecisionMatches(expected, actual)
            assertEquals(expected.name, actual.name)
            assertEquals(expected.value, actual.value)
            assertEquals(expected.getLong(JavaChronoField.ERA), actual.getLong(ChronoField.ERA))
            assertEquals(expected.range(JavaChronoField.ERA).toString(), actual.range(ChronoField.ERA).toString())
        }
        assertEquals(JavaHijrahEra.AH.name, HijrahEra.AH.name)
        assertPrecisionMatches(JavaHijrahEra.AH, HijrahEra.AH)
        assertEquals(JavaHijrahEra.AH.value, HijrahEra.AH.value)
        assertEquals(
            JavaHijrahEra.AH.range(JavaChronoField.ERA).toString(),
            HijrahEra.AH.range(ChronoField.ERA).toString(),
        )
        JavaJapaneseEra.values().zip(JapaneseEra.values()).forEach { (expected, actual) ->
            assertPrecisionMatches(expected, actual)
            assertEquals(expected.value, actual.value)
            assertEquals(expected.toString(), actual.toString())
            assertEquals(expected.getLong(JavaChronoField.ERA), actual.getLong(ChronoField.ERA))
            assertEquals(expected.range(JavaChronoField.ERA).toString(), actual.range(ChronoField.ERA).toString())
        }
    }

    private fun assertPrecisionMatches(expected: java.time.chrono.Era, actual: Era) {
        assertEquals(
            expected.query(JavaTemporalQueries.precision()).toString(),
            actual.query(TemporalQueries.precision()).toString(),
        )
    }
}
