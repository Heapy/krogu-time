package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocalTimeTest {
    @Test
    fun validatesAndCreatesComponentTimes() {
        assertEquals(LocalTime.of(12, 34, 0, 0), LocalTime.of(12, 34))
        assertEquals(LocalTime.of(12, 34, 56, 0), LocalTime.of(12, 34, 56))
        assertFailsWith<DateTimeException> { LocalTime.of(-1, 0) }
        assertFailsWith<DateTimeException> { LocalTime.of(24, 0) }
        assertFailsWith<DateTimeException> { LocalTime.of(0, 60) }
        assertFailsWith<DateTimeException> { LocalTime.of(0, 0, 60) }
        assertFailsWith<DateTimeException> { LocalTime.of(0, 0, 0, 1_000_000_000) }
    }

    @Test
    fun convertsSecondsAndNanosecondsAcrossTheDay() {
        val cases = listOf(
            LocalTime.MIDNIGHT to 0L,
            LocalTime.of(1, 2, 3, 4) to 3_723_000_000_004L,
            LocalTime.NOON to 43_200_000_000_000L,
            LocalTime.MAX to 86_399_999_999_999L,
        )
        cases.forEach { (time, nanoOfDay) ->
            assertEquals(nanoOfDay, time.toNanoOfDay())
            assertEquals(time, LocalTime.ofNanoOfDay(nanoOfDay))
            assertEquals(nanoOfDay / 1_000_000_000, time.toSecondOfDay().toLong())
        }
        assertEquals(LocalTime.of(23, 59, 59), LocalTime.ofSecondOfDay(86_399))
        assertFailsWith<DateTimeException> { LocalTime.ofSecondOfDay(-1) }
        assertFailsWith<DateTimeException> { LocalTime.ofSecondOfDay(86_400) }
        assertFailsWith<DateTimeException> { LocalTime.ofNanoOfDay(-1) }
        assertFailsWith<DateTimeException> { LocalTime.ofNanoOfDay(86_400_000_000_000) }
    }

    @Test
    fun exposesComponentsConstantsAndTemporalConversion() {
        val time = LocalTime.of(12, 34, 56, 789)
        assertEquals(12, time.hour)
        assertEquals(34, time.minute)
        assertEquals(56, time.second)
        assertEquals(789, time.nano)
        assertEquals(LocalTime.of(0, 0), LocalTime.MIN)
        assertEquals(LocalTime.MIN, LocalTime.MIDNIGHT)
        assertEquals(LocalTime.of(12, 0), LocalTime.NOON)
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), LocalTime.MAX)
        assertEquals(time, LocalTime.from(time))
        assertEquals(time, LocalTime.from(NanoOfDayAccessor(time.toNanoOfDay())))
        assertFailsWith<DateTimeException> { LocalTime.from(NanoOfDayAccessor()) }
    }

    @Test
    fun exposesStandardTimeFields() {
        val time = LocalTime.of(13, 14, 15, 123_456_789)
        val expectedFields = mapOf(
            ChronoField.NANO_OF_SECOND to 123_456_789L,
            ChronoField.NANO_OF_DAY to 47_655_123_456_789L,
            ChronoField.MICRO_OF_SECOND to 123_456L,
            ChronoField.MICRO_OF_DAY to 47_655_123_456L,
            ChronoField.MILLI_OF_SECOND to 123L,
            ChronoField.MILLI_OF_DAY to 47_655_123L,
            ChronoField.SECOND_OF_MINUTE to 15L,
            ChronoField.SECOND_OF_DAY to 47_655L,
            ChronoField.MINUTE_OF_HOUR to 14L,
            ChronoField.MINUTE_OF_DAY to 794L,
            ChronoField.HOUR_OF_AMPM to 1L,
            ChronoField.CLOCK_HOUR_OF_AMPM to 1L,
            ChronoField.HOUR_OF_DAY to 13L,
            ChronoField.CLOCK_HOUR_OF_DAY to 13L,
            ChronoField.AMPM_OF_DAY to 1L,
        )

        ChronoField.entries.forEach { field ->
            assertEquals(field in expectedFields, time.isSupported(field), field.toString())
        }
        expectedFields.forEach { (field, value) ->
            assertEquals(value, time.getLong(field), field.toString())
        }
        assertEquals(12, LocalTime.MIDNIGHT.get(ChronoField.CLOCK_HOUR_OF_AMPM))
        assertEquals(24, LocalTime.MIDNIGHT.get(ChronoField.CLOCK_HOUR_OF_DAY))
        assertFailsWith<UnsupportedTemporalTypeException> {
            time.getLong(ChronoField.DAY_OF_MONTH)
        }
        assertFailsWith<UnsupportedTemporalTypeException> {
            time.get(ChronoField.NANO_OF_DAY)
        }
    }

    @Test
    fun formatsAndOrdersTimesWithinTheDay() {
        val cases = mapOf(
            LocalTime.of(1, 2) to "01:02",
            LocalTime.of(1, 2, 3) to "01:02:03",
            LocalTime.of(1, 2, 3, 1_000_000) to "01:02:03.001",
            LocalTime.of(1, 2, 3, 1_000) to "01:02:03.000001",
            LocalTime.of(1, 2, 3, 1) to "01:02:03.000000001",
        )
        cases.forEach { (time, text) -> assertEquals(text, time.toString()) }

        val earlier = LocalTime.of(12, 0)
        val later = LocalTime.of(12, 0, 0, 1)
        assertTrue(earlier < later)
        assertTrue(later.isAfter(earlier))
        assertTrue(earlier.isBefore(later))
    }

    private data class NanoOfDayAccessor(
        val nanoOfDay: Long? = null,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField): Boolean = field === ChronoField.NANO_OF_DAY

        override fun getLong(field: TemporalField): Long =
            nanoOfDay ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")
    }
}
