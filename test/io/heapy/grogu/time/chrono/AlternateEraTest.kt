package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class AlternateEraTest {
    @Test
    fun erasExposeLocalizedDisplayNames() {
        assertEquals("Anno Domini", IsoEra.CE.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
        assertEquals("AD", IsoEra.CE.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
        assertEquals("A", IsoEra.CE.getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
        assertEquals("Reiwa", JapaneseEra.REIWA.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
        assertEquals("R", JapaneseEra.REIWA.getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
        assertEquals("AH", HijrahEra.AH.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
    }

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

    @Test
    fun japaneseErasExposeHistoricalValuesNamesAndDefensiveCopies() {
        val expected = listOf(
            JapaneseEra.MEIJI,
            JapaneseEra.TAISHO,
            JapaneseEra.SHOWA,
            JapaneseEra.HEISEI,
            JapaneseEra.REIWA,
        )
        assertEquals(listOf(-1, 0, 1, 2, 3), expected.map(Era::value))
        assertEquals(listOf("Meiji", "Taisho", "Showa", "Heisei", "Reiwa"), expected.map(Any::toString))
        assertEquals(expected, JapaneseEra.values().toList())
        assertNotSame(JapaneseEra.values(), JapaneseEra.values())
        expected.forEach { era ->
            assertSame(era, JapaneseEra.of(era.value))
            assertSame(era, JapaneseEra.valueOf(era.toString()))
            assertEquals("-1 - 3", era.range(ChronoField.ERA).toString())
        }
        assertFailsWith<DateTimeException> { JapaneseEra.of(4) }
        assertFailsWith<IllegalArgumentException> { JapaneseEra.valueOf("REIWA") }
    }
}
