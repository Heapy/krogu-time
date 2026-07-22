package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.temporal.ChronoField
import java.time.chrono.HijrahEra as JavaHijrahEra
import java.time.chrono.MinguoEra as JavaMinguoEra
import java.time.chrono.ThaiBuddhistEra as JavaThaiBuddhistEra
import java.time.temporal.ChronoField as JavaChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class AlternateEraJavaConformanceTest {
    @Test
    fun valuesNamesFieldsAndRangesMatchJavaTime() {
        MinguoEra.entries.zip(JavaMinguoEra.entries).forEach { (actual, expected) ->
            assertEquals(expected.name, actual.name)
            assertEquals(expected.value, actual.value)
            assertEquals(expected.getLong(JavaChronoField.ERA), actual.getLong(ChronoField.ERA))
            assertEquals(expected.range(JavaChronoField.ERA).toString(), actual.range(ChronoField.ERA).toString())
        }
        ThaiBuddhistEra.entries.zip(JavaThaiBuddhistEra.entries).forEach { (actual, expected) ->
            assertEquals(expected.name, actual.name)
            assertEquals(expected.value, actual.value)
            assertEquals(expected.getLong(JavaChronoField.ERA), actual.getLong(ChronoField.ERA))
            assertEquals(expected.range(JavaChronoField.ERA).toString(), actual.range(ChronoField.ERA).toString())
        }
        assertEquals(JavaHijrahEra.AH.name, HijrahEra.AH.name)
        assertEquals(JavaHijrahEra.AH.value, HijrahEra.AH.value)
        assertEquals(
            JavaHijrahEra.AH.range(JavaChronoField.ERA).toString(),
            HijrahEra.AH.range(ChronoField.ERA).toString(),
        )
    }
}
