package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InstantTest {
    @Test
    fun parsesAndFormatsIsoInstants() {
        val cases = mapOf(
            "1970-01-01T00:00:00Z" to Instant.EPOCH,
            "1970-01-01t00:00:00z" to Instant.EPOCH,
            "1969-12-31T23:59:59.999999999Z" to Instant.ofEpochSecond(-1, 999_999_999),
            "2024-02-29T12:34:56.1234Z" to Instant.ofEpochSecond(1_709_210_096, 123_400_000),
            "1970-01-01T01:00:00+01:00" to Instant.EPOCH,
            "1970-01-01T00:00:00+01:00:30" to Instant.ofEpochSecond(-3_630),
            "1970-01-01T24:00:00Z" to Instant.ofEpochSecond(86_400),
            "1970-01-01T23:59:60Z" to Instant.ofEpochSecond(86_399),
            "-1000000000-01-01T00:00:00Z" to Instant.MIN,
            "+1000000000-12-31T23:59:59.999999999Z" to Instant.MAX,
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, Instant.parse(text), text)
        }

        assertEquals("1970-01-01T00:00:00Z", Instant.EPOCH.toString())
        assertEquals("1969-12-31T23:59:59.999Z", Instant.ofEpochMilli(-1).toString())
        assertEquals("2024-02-29T12:34:56.123400Z", cases.getValue("2024-02-29T12:34:56.1234Z").toString())
        assertEquals("-1000000000-01-01T00:00:00Z", Instant.MIN.toString())
        assertEquals("+1000000000-12-31T23:59:59.999999999Z", Instant.MAX.toString())

        cases.values.forEach { instant -> assertEquals(instant, Instant.parse(instant.toString())) }
    }

    @Test
    fun rejectsMalformedOrOutOfRangeIsoInstants() {
        val invalidInputs = mapOf(
            "" to 0,
            "1970-01-01" to 10,
            "1970-01-01T00:00Z" to 16,
            "1970-01-01 00:00:00Z" to 10,
            "1970-01-01T00:00:00,1Z" to 19,
            "1970-01-01T00:00:00.1234567890Z" to 29,
            "1970-01-01T00:00:00+00" to 19,
            "1970-01-01T00:00:00+0000" to 19,
            "1970-01-01T24:00:00.1Z" to 0,
            "1970-01-01T22:59:60Z" to 0,
            "+9999-01-01T00:00:00Z" to 0,
            "+1000000001-01-01T00:00:00Z" to 0,
            "２０２４-０２-２９T１２:３４:５６Z" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { Instant.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }

    @Test
    fun createsNormalizedInstantsFromEpochValues() {
        assertEquals(Instant.ofEpochSecond(0), Instant.EPOCH)
        assertEquals(Instant.ofEpochSecond(0, 999_999_999), Instant.ofEpochSecond(1, -1))
        assertEquals(Instant.EPOCH, Instant.ofEpochSecond(-1, 1_000_000_000))
        assertEquals(Instant.ofEpochSecond(-1, 999_000_000), Instant.ofEpochMilli(-1))
        assertEquals(Instant.ofEpochSecond(1, 234_000_000), Instant.ofEpochMilli(1_234))

        assertEquals(-31_557_014_167_219_200L, Instant.MIN.epochSecond)
        assertEquals(0, Instant.MIN.nano)
        assertEquals(31_556_889_864_403_199L, Instant.MAX.epochSecond)
        assertEquals(999_999_999, Instant.MAX.nano)
        assertFailsWith<DateTimeException> {
            Instant.ofEpochSecond(Instant.MIN.epochSecond - 1)
        }
        assertFailsWith<DateTimeException> {
            Instant.ofEpochSecond(Instant.MAX.epochSecond + 1)
        }
    }

    @Test
    fun convertsFromTemporalAccessors() {
        val expected = Instant.ofEpochSecond(-2, 123_456_789)
        assertSame(expected, Instant.from(expected))
        assertEquals(expected, Instant.from(InstantAccessor(-2, 123_456_789)))
        assertFailsWith<DateTimeException> { Instant.from(InstantAccessor(epochSecond = -2)) }
        assertFailsWith<DateTimeException> { Instant.from(InstantAccessor(-2, 1_000_000_000)) }
    }

    @Test
    fun exposesOnlyInstantFieldsAndPreciseUnits() {
        val instant = Instant.ofEpochSecond(-2, 123_456_789)
        val values = mapOf(
            ChronoField.NANO_OF_SECOND to 123_456_789L,
            ChronoField.MICRO_OF_SECOND to 123_456L,
            ChronoField.MILLI_OF_SECOND to 123L,
            ChronoField.INSTANT_SECONDS to -2L,
        )
        ChronoField.entries.forEach { field ->
            assertEquals(field in values, instant.isSupported(field), field.toString())
            if (field in values) assertEquals(values.getValue(field), instant.getLong(field))
        }
        assertEquals(123_456_789, instant.get(ChronoField.NANO_OF_SECOND))
        assertEquals(123_456, instant.get(ChronoField.MICRO_OF_SECOND))
        assertEquals(123, instant.get(ChronoField.MILLI_OF_SECOND))
        val exception = assertFailsWith<UnsupportedTemporalTypeException> {
            instant.get(ChronoField.INSTANT_SECONDS)
        }
        assertEquals("Unsupported field: InstantSeconds", exception.message)

        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit <= ChronoUnit.DAYS, instant.isSupported(unit), unit.toString())
        }
    }

    @Test
    fun replacesFieldsAndTruncatesWithinTheUtcDay() {
        val instant = Instant.ofEpochSecond(-61, 987_654_321)
        assertEquals(Instant.ofEpochSecond(-61, 1), instant.with(ChronoField.NANO_OF_SECOND, 1))
        assertEquals(
            Instant.ofEpochSecond(-61, 123_000),
            instant.with(ChronoField.MICRO_OF_SECOND, 123),
        )
        assertEquals(
            Instant.ofEpochSecond(-61, 123_000_000),
            instant.with(ChronoField.MILLI_OF_SECOND, 123),
        )
        assertEquals(
            Instant.ofEpochSecond(5, 987_654_321),
            instant.with(ChronoField.INSTANT_SECONDS, 5),
        )
        assertEquals(Instant.ofEpochSecond(-61, 987_000_000), instant.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(Instant.ofEpochSecond(-61), instant.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(Instant.ofEpochSecond(-120), instant.truncatedTo(ChronoUnit.MINUTES))
        assertFailsWith<UnsupportedTemporalTypeException> { instant.truncatedTo(ChronoUnit.WEEKS) }
        assertFailsWith<UnsupportedTemporalTypeException> { instant.truncatedTo(ChronoUnit.MONTHS) }
    }

    @Test
    fun addsAndSubtractsPreciseAmounts() {
        val instant = Instant.ofEpochSecond(-2, 999_999_999)
        assertEquals(Instant.ofEpochSecond(-1), instant.plusNanos(1))
        assertEquals(Instant.ofEpochSecond(-1, 999_999_999), instant.plusSeconds(1))
        assertEquals(Instant.ofEpochSecond(-1, 999_999), instant.plusMillis(1))
        assertEquals(instant.plusSeconds(60), instant.plus(1, ChronoUnit.MINUTES))
        assertEquals(instant.plusSeconds(3_600), instant.plus(1, ChronoUnit.HOURS))
        assertEquals(instant.plusSeconds(43_200), instant.plus(1, ChronoUnit.HALF_DAYS))
        assertEquals(instant.plusSeconds(86_400), instant.plus(1, ChronoUnit.DAYS))
        assertEquals(
            instant.plusSeconds(2).plusNanos(3),
            instant.plus(Duration.ofSeconds(2, 3)),
        )
        assertEquals(instant, instant.plusNanos(Long.MIN_VALUE).minusNanos(Long.MIN_VALUE))
        assertFailsWith<UnsupportedTemporalTypeException> { instant.plus(1, ChronoUnit.WEEKS) }
        assertFailsWith<DateTimeException> { Instant.MAX.plusNanos(1) }
        assertFailsWith<DateTimeException> { Instant.MIN.minusNanos(1) }
    }

    @Test
    fun measuresCompleteUnitsAndDurationBetweenInstants() {
        val start = Instant.ofEpochSecond(-2, 900_000_999)
        val end = Instant.ofEpochSecond(61, 100_999_001)
        assertEquals(62_200_998_002L, start.until(end, ChronoUnit.NANOS))
        assertEquals(62_200_998L, start.until(end, ChronoUnit.MICROS))
        assertEquals(62_200L, start.until(end, ChronoUnit.MILLIS))
        assertEquals(62L, start.until(end, ChronoUnit.SECONDS))
        assertEquals(1L, start.until(end, ChronoUnit.MINUTES))
        assertEquals(0L, start.until(end, ChronoUnit.HOURS))
        assertEquals(-62L, end.until(start, ChronoUnit.SECONDS))
        assertEquals(Duration.ofSeconds(62, 200_998_002), start.until(end))
    }

    @Test
    fun convertsToEpochMillisAdjustsOrdersAndHashes() {
        assertEquals(-1L, Instant.ofEpochSecond(-1, 999_999_999).toEpochMilli())
        assertEquals(-1_001L, Instant.ofEpochSecond(-2, 999_999_999).toEpochMilli())
        assertEquals(1_234L, Instant.ofEpochSecond(1, 234_999_999).toEpochMilli())

        val instant = Instant.ofEpochSecond(5, 123_456_789)
        assertEquals(
            InstantRecordingTemporal(
                operations = listOf(
                    ChronoField.INSTANT_SECONDS to 5L,
                    ChronoField.NANO_OF_SECOND to 123_456_789L,
                ),
            ),
            instant.adjustInto(InstantRecordingTemporal()),
        )
        assertTrue(Instant.EPOCH < instant)
        assertTrue(instant.isAfter(Instant.EPOCH))
        assertTrue(Instant.EPOCH.isBefore(instant))
        assertEquals(instant, Instant.ofEpochSecond(5, 123_456_789))
        assertEquals(instant.hashCode(), Instant.ofEpochSecond(5, 123_456_789).hashCode())
    }

    private data class InstantAccessor(
        val epochSecond: Long? = null,
        val nano: Long? = null,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.INSTANT_SECONDS && epochSecond != null ||
                field === ChronoField.NANO_OF_SECOND && nano != null

        override fun getLong(field: TemporalField): Long = when (field) {
            ChronoField.INSTANT_SECONDS -> epochSecond
            ChronoField.NANO_OF_SECOND -> nano
            else -> null
        } ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")
    }

    private data class InstantRecordingTemporal(
        val operations: List<Pair<TemporalField, Long>> = emptyList(),
    ) : Temporal {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.INSTANT_SECONDS || field === ChronoField.NANO_OF_SECOND

        override fun isSupported(unit: TemporalUnit): Boolean = false

        override fun getLong(field: TemporalField): Long =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (isSupported(field)) copy(operations = operations + (field to newValue)) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
