package io.heapy.krogu.time

import io.heapy.krogu.time.format.DateTimeParseException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalAdjuster
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
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
    fun convertsDateTimeAndOffsetToEpochSeconds() {
        assertEquals(
            0L,
            LocalTime.MIDNIGHT.toEpochSecond(LocalDate.EPOCH, ZoneOffset.UTC),
        )
        assertEquals(
            122_400L,
            LocalTime.NOON.toEpochSecond(
                LocalDate.of(1970, 1, 2),
                ZoneOffset.ofHours(2),
            ),
        )
        assertEquals(
            LocalDate.MIN.toEpochSecond(LocalTime.MIN, ZoneOffset.MAX),
            LocalTime.MIN.toEpochSecond(LocalDate.MIN, ZoneOffset.MAX),
        )
        assertEquals(
            LocalDate.MAX.toEpochSecond(LocalTime.MAX, ZoneOffset.MIN),
            LocalTime.MAX.toEpochSecond(LocalDate.MAX, ZoneOffset.MIN),
        )
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
        val nanoOfDayException = assertFailsWith<UnsupportedTemporalTypeException> {
            time.get(ChronoField.NANO_OF_DAY)
        }
        assertEquals(
            "Invalid field 'NanoOfDay' for get() method, use getLong() instead",
            nanoOfDayException.message,
        )
        val microOfDayException = assertFailsWith<UnsupportedTemporalTypeException> {
            time.get(ChronoField.MICRO_OF_DAY)
        }
        assertEquals(
            "Invalid field 'MicroOfDay' for get() method, use getLong() instead",
            microOfDayException.message,
        )
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

    @Test
    fun parsesStrictIsoLocalTimes() {
        val cases = mapOf(
            "00:00" to LocalTime.MIDNIGHT,
            "01:02" to LocalTime.of(1, 2),
            "01:02:03" to LocalTime.of(1, 2, 3),
            "01:02:03." to LocalTime.of(1, 2, 3),
            "01:02:03.1" to LocalTime.of(1, 2, 3, 100_000_000),
            "01:02:03.000001" to LocalTime.of(1, 2, 3, 1_000),
            "23:59:59.123456789" to LocalTime.of(23, 59, 59, 123_456_789),
        )
        cases.forEach { (text, expected) -> assertEquals(expected, LocalTime.parse(text), text) }

        val invalidInputs = mapOf(
            "" to 0,
            "1:02" to 0,
            "01:2" to 3,
            "01:02:" to 5,
            "01:02:3" to 5,
            "01:02:03.1234567890" to 18,
            "24:00" to 0,
            "23:60" to 0,
            "23:59:60" to 0,
            "23:59.1" to 5,
            "01:02Z" to 5,
            "01-02" to 2,
            "01:02:03,1" to 8,
            "０１:０２" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { LocalTime.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }

    @Test
    fun supportsTimeUnitsAndAdjustsComponentsAndFields() {
        val time = LocalTime.of(13, 14, 15, 123_456_789)

        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit.isTimeBased, time.isSupported(unit), unit.toString())
        }
        assertSame(time, time.withHour(13))
        assertEquals(LocalTime.of(2, 14, 15, 123_456_789), time.withHour(2))
        assertEquals(LocalTime.of(13, 2, 15, 123_456_789), time.withMinute(2))
        assertEquals(LocalTime.of(13, 14, 2, 123_456_789), time.withSecond(2))
        assertEquals(LocalTime.of(13, 14, 15, 2), time.withNano(2))

        val expected = mapOf(
            ChronoField.NANO_OF_SECOND to (987L to LocalTime.of(13, 14, 15, 987)),
            ChronoField.NANO_OF_DAY to (987L to LocalTime.ofNanoOfDay(987)),
            ChronoField.MICRO_OF_SECOND to (987L to LocalTime.of(13, 14, 15, 987_000)),
            ChronoField.MICRO_OF_DAY to (987L to LocalTime.ofNanoOfDay(987_000)),
            ChronoField.MILLI_OF_SECOND to (987L to LocalTime.of(13, 14, 15, 987_000_000)),
            ChronoField.MILLI_OF_DAY to (987L to LocalTime.ofNanoOfDay(987_000_000)),
            ChronoField.SECOND_OF_MINUTE to (2L to LocalTime.of(13, 14, 2, 123_456_789)),
            ChronoField.SECOND_OF_DAY to (987L to LocalTime.of(0, 16, 27, 123_456_789)),
            ChronoField.MINUTE_OF_HOUR to (2L to LocalTime.of(13, 2, 15, 123_456_789)),
            ChronoField.MINUTE_OF_DAY to (2L to LocalTime.of(0, 2, 15, 123_456_789)),
            ChronoField.HOUR_OF_AMPM to (2L to LocalTime.of(14, 14, 15, 123_456_789)),
            ChronoField.CLOCK_HOUR_OF_AMPM to (12L to LocalTime.of(12, 14, 15, 123_456_789)),
            ChronoField.HOUR_OF_DAY to (2L to LocalTime.of(2, 14, 15, 123_456_789)),
            ChronoField.CLOCK_HOUR_OF_DAY to (24L to LocalTime.of(0, 14, 15, 123_456_789)),
            ChronoField.AMPM_OF_DAY to (0L to LocalTime.of(1, 14, 15, 123_456_789)),
        )
        expected.forEach { (field, valueAndResult) ->
            val (value, result) = valueAndResult
            assertEquals(result, time.with(field, value), field.toString())
        }
        assertEquals(
            LocalTime.of(13, 14, 20, 123_456_789),
            time.with(TemporalAdjuster { it.with(ChronoField.SECOND_OF_MINUTE, 20) }),
        )
        assertFailsWith<DateTimeException> { time.withHour(24) }
        assertFailsWith<UnsupportedTemporalTypeException> {
            time.with(ChronoField.DAY_OF_MONTH, 1)
        }
    }

    @Test
    fun addsAndSubtractsWithMidnightWraparound() {
        val endOfDay = LocalTime.MAX
        assertEquals(LocalTime.MIDNIGHT, endOfDay.plusNanos(1))
        assertEquals(LocalTime.of(0, 0, 0, 999_999_999), endOfDay.plusSeconds(1))
        assertEquals(LocalTime.of(0, 0, 59, 999_999_999), endOfDay.plusMinutes(1))
        assertEquals(LocalTime.of(0, 59, 59, 999_999_999), endOfDay.plusHours(1))

        val time = LocalTime.of(1, 2, 3, 4)
        assertEquals(time.plusNanos(1), time.plus(1, ChronoUnit.NANOS))
        assertEquals(time.plusNanos(1_000), time.plus(1, ChronoUnit.MICROS))
        assertEquals(time.plusNanos(1_000_000), time.plus(1, ChronoUnit.MILLIS))
        assertEquals(time.plusSeconds(1), time.plus(1, ChronoUnit.SECONDS))
        assertEquals(time.plusMinutes(1), time.plus(1, ChronoUnit.MINUTES))
        assertEquals(time.plusHours(1), time.plus(1, ChronoUnit.HOURS))
        assertEquals(time.plusHours(12), time.plus(1, ChronoUnit.HALF_DAYS))
        assertEquals(time.plusHours(2), time.plus(1, TWO_HOUR_UNIT))
        assertEquals(time.plusSeconds(2).plusNanos(3), time.plus(Duration.ofSeconds(2, 3)))
        assertEquals(time.minusHours(2), time.minus(1, TWO_HOUR_UNIT))
        assertEquals(time.minusSeconds(2).minusNanos(3), time.minus(Duration.ofSeconds(2, 3)))

        assertEquals(time, time.plusHours(Long.MAX_VALUE).minusHours(Long.MAX_VALUE))
        assertEquals(time, time.minusNanos(Long.MIN_VALUE).plusNanos(Long.MIN_VALUE))
        assertFailsWith<UnsupportedTemporalTypeException> { time.plus(1, ChronoUnit.DAYS) }
    }

    @Test
    fun truncatesToUnitsThatDivideAStandardDay() {
        val time = LocalTime.of(13, 14, 15, 987_654_321)
        assertSame(time, time.truncatedTo(ChronoUnit.NANOS))
        assertEquals(LocalTime.of(13, 14, 15, 987_654_000), time.truncatedTo(ChronoUnit.MICROS))
        assertEquals(LocalTime.of(13, 14, 15, 987_000_000), time.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(LocalTime.of(13, 14, 15), time.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(LocalTime.of(13, 14), time.truncatedTo(ChronoUnit.MINUTES))
        assertEquals(LocalTime.of(13, 0), time.truncatedTo(ChronoUnit.HOURS))
        assertEquals(LocalTime.of(12, 0), time.truncatedTo(ChronoUnit.HALF_DAYS))
        assertEquals(LocalTime.MIDNIGHT, time.truncatedTo(ChronoUnit.DAYS))
        assertEquals(LocalTime.of(12, 0), time.truncatedTo(TWO_HOUR_UNIT))
        assertFailsWith<UnsupportedTemporalTypeException> { time.truncatedTo(ChronoUnit.WEEKS) }
        assertFailsWith<UnsupportedTemporalTypeException> { time.truncatedTo(SEVEN_MINUTE_UNIT) }
    }

    @Test
    fun measuresCompleteTimeUnitsAndAdjustsAnotherTemporal() {
        val start = LocalTime.of(10, 30, 40, 500_000_000)
        val end = LocalTime.of(12, 31, 42, 750_000_000)
        val expected = mapOf(
            ChronoUnit.NANOS to 7_262_250_000_000L,
            ChronoUnit.MICROS to 7_262_250_000L,
            ChronoUnit.MILLIS to 7_262_250L,
            ChronoUnit.SECONDS to 7_262L,
            ChronoUnit.MINUTES to 121L,
            ChronoUnit.HOURS to 2L,
            ChronoUnit.HALF_DAYS to 0L,
        )
        expected.forEach { (unit, amount) ->
            assertEquals(amount, start.until(end, unit), unit.toString())
            assertEquals(-amount, end.until(start, unit), "reverse $unit")
        }
        assertEquals(1L, start.until(end, TWO_HOUR_UNIT))
        assertFailsWith<UnsupportedTemporalTypeException> { start.until(end, ChronoUnit.DAYS) }
        assertEquals(
            NanoOfDayTemporal(start.toNanoOfDay()),
            start.adjustInto(NanoOfDayTemporal()),
        )
    }

    private data class NanoOfDayAccessor(
        val nanoOfDay: Long? = null,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.NANO_OF_DAY

        override fun getLong(field: TemporalField): Long =
            nanoOfDay ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")
    }

    private data class NanoOfDayTemporal(
        val nanoOfDay: Long? = null,
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.NANO_OF_DAY

        override fun isSupported(unit: TemporalUnit?): Boolean = false

        override fun getLong(field: TemporalField): Long =
            nanoOfDay ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (field === ChronoField.NANO_OF_DAY) copy(nanoOfDay = newValue) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }

    private companion object {
        val TWO_HOUR_UNIT: TemporalUnit = exactUnit("TwoHours", Duration.ofHours(2))
        val SEVEN_MINUTE_UNIT: TemporalUnit = exactUnit("SevenMinutes", Duration.ofMinutes(7))

        fun exactUnit(name: String, duration: Duration): TemporalUnit = object : TemporalUnit {
            override val duration: Duration = duration
            override val isDurationEstimated: Boolean = false
            override val isDateBased: Boolean = false
            override val isTimeBased: Boolean = true

            override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
                @Suppress("UNCHECKED_CAST")
                return temporal.plus(amount * duration.toNanos(), ChronoUnit.NANOS) as R
            }

            override fun between(
                temporal1Inclusive: Temporal,
                temporal2Exclusive: Temporal,
            ): Long = temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.NANOS) / duration.toNanos()

            override fun toString(): String = name
        }
    }
}
