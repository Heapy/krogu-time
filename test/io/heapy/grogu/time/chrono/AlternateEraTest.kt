package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AlternateEraTest {
    @Test
    fun minguoAndThaiBuddhistErasUseTwoEraNumbering() {
        assertEquals(listOf(MinguoEra.BEFORE_ROC, MinguoEra.ROC), MinguoEra.entries)
        assertEquals(listOf(0, 1), MinguoEra.entries.map(MinguoEra::value))
        assertSame(MinguoEra.BEFORE_ROC, MinguoEra.of(0))
        assertSame(MinguoEra.ROC, MinguoEra.of(1))

        assertEquals(listOf(ThaiBuddhistEra.BEFORE_BE, ThaiBuddhistEra.BE), ThaiBuddhistEra.entries)
        assertEquals(listOf(0, 1), ThaiBuddhistEra.entries.map(ThaiBuddhistEra::value))
        assertSame(ThaiBuddhistEra.BEFORE_BE, ThaiBuddhistEra.of(0))
        assertSame(ThaiBuddhistEra.BE, ThaiBuddhistEra.of(1))

        assertFailsWith<DateTimeException> { MinguoEra.of(-1) }
        assertFailsWith<DateTimeException> { ThaiBuddhistEra.of(2) }
    }

    @Test
    fun hijrahHasOneEraWithARefinedRange() {
        assertEquals(listOf(HijrahEra.AH), HijrahEra.entries)
        assertEquals(1, HijrahEra.AH.value)
        assertSame(HijrahEra.AH, HijrahEra.of(1))
        assertEquals(ValueRange.of(1, 1), HijrahEra.AH.range(ChronoField.ERA))
        assertFailsWith<DateTimeException> { HijrahEra.of(0) }
    }
}
