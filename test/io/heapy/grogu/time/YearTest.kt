package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YearTest {
    @Test
    fun convertsNonIsoTemporalsThroughTheIsoEpochDay() {
        val hijrahDate = HijrahDate.of(1445, 9, 1)
        val isoDate = LocalDate.ofEpochDay(hijrahDate.toEpochDay())

        assertEquals(Year.of(isoDate.year), Year.from(hijrahDate))
    }

    @Test
    fun validatesTheSupportedYearRange() {
        assertEquals(Year.MIN_VALUE, Year.of(Year.MIN_VALUE).value)
        assertEquals(Year.MAX_VALUE, Year.of(Year.MAX_VALUE).value)
        assertFailsWith<DateTimeException> { Year.of(Year.MIN_VALUE - 1) }
        assertFailsWith<DateTimeException> { Year.of(Year.MAX_VALUE + 1) }
    }

    @Test
    fun appliesTheProlepticGregorianLeapYearRules() {
        assertTrue(Year.isLeap(2000))
        assertFalse(Year.isLeap(1900))
        assertTrue(Year.isLeap(2004))
        assertFalse(Year.isLeap(2001))
        assertTrue(Year.isLeap(0))
        assertTrue(Year.of(2024).isLeap)
        assertEquals(366, Year.of(2024).length)
        assertEquals(365, Year.of(2023).length)
    }

    @Test
    fun parsesDefaultIsoYearsAndCreatesDatesFromDays() {
        val cases = mapOf(
            "0" to Year.of(0),
            "1" to Year.of(1),
            "001" to Year.of(1),
            "0000" to Year.of(0),
            "2024" to Year.of(2024),
            "-0001" to Year.of(-1),
            "-001" to Year.of(-1),
            "-0000" to Year.of(0),
            "+2024" to Year.of(2024),
            "02024" to Year.of(2024),
            "10000" to Year.of(10_000),
            "+10000" to Year.of(10_000),
            "-999999999" to Year.of(Year.MIN_VALUE),
            "+999999999" to Year.of(Year.MAX_VALUE),
        )
        cases.forEach { (text, expected) -> assertEquals(expected, Year.parse(text), text) }

        assertEquals(LocalDate.of(2024, 2, 29), Year.of(2024).atDay(60))
        assertEquals(LocalDate.of(2024, 12, 31), Year.of(2024).atDay(366))
        assertEquals(LocalDate.of(2023, 12, 31), Year.of(2023).atDay(365))
        assertFailsWith<DateTimeException> { Year.of(2024).atDay(0) }
        assertFailsWith<DateTimeException> { Year.of(2023).atDay(366) }

        val invalidInputs = mapOf(
            "" to 0,
            "+" to 1,
            "-" to 1,
            "+1000000000" to 10,
            "+12345678901" to 10,
            "2024x" to 4,
            "２０２４" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { Year.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }

    @Test
    fun exposesYearEraFieldsAndRefinedRanges() {
        val ce = Year.of(2024)
        assertEquals(2024, ce.get(ChronoField.YEAR))
        assertEquals(2024, ce.get(ChronoField.YEAR_OF_ERA))
        assertEquals(1, ce.get(ChronoField.ERA))
        assertEquals(ValueRange.of(1, Year.MAX_VALUE.toLong()), ce.range(ChronoField.YEAR_OF_ERA))

        val bce = Year.of(-1)
        assertEquals(-1, bce.get(ChronoField.YEAR))
        assertEquals(2, bce.get(ChronoField.YEAR_OF_ERA))
        assertEquals(0, bce.get(ChronoField.ERA))
        assertEquals(
            ValueRange.of(1, Year.MAX_VALUE.toLong() + 1),
            bce.range(ChronoField.YEAR_OF_ERA),
        )
        assertFalse(ce.isSupported(ChronoField.MONTH_OF_YEAR))
        assertFailsWith<UnsupportedTemporalTypeException> {
            ce.getLong(ChronoField.MONTH_OF_YEAR)
        }
        assertFailsWith<UnsupportedTemporalTypeException> {
            ce.with(ChronoField.EPOCH_DAY, 0)
        }
        assertFailsWith<DateTimeException> {
            ce.with(ChronoField.EPOCH_DAY, Long.MIN_VALUE)
        }
    }

    @Test
    fun supportsYearScaleArithmeticAndEraChanges() {
        val year = Year.of(2024)
        assertEquals(Year.of(2025), year.plusYears(1))
        assertEquals(Year.of(2034), year.plus(1, ChronoUnit.DECADES))
        assertEquals(Year.of(1924), year.minus(1, ChronoUnit.CENTURIES))
        assertEquals(Year.of(-2023), year.with(ChronoField.ERA, 0))
        assertEquals(Year.of(-2023), year.minus(1, ChronoUnit.ERAS))
        assertEquals(24, year.until(Year.of(2048), ChronoUnit.YEARS))
        assertEquals(2, year.until(Year.of(2048), ChronoUnit.DECADES))
        assertFailsWith<DateTimeException> { Year.of(Year.MAX_VALUE).plusYears(1) }
        assertFailsWith<UnsupportedTemporalTypeException> {
            year.plus(1, ChronoUnit.MONTHS)
        }
    }

    @Test
    fun adjustsAndOrdersYears() {
        assertEquals(
            YearRecordingTemporal(2024),
            Year.of(2024).adjustInto(YearRecordingTemporal()),
        )
        assertFailsWith<DateTimeException> {
            Year.of(1445).adjustInto(HijrahDate.of(1445, 9, 1))
        }
        assertTrue(Year.of(2024).isAfter(Year.of(2023)))
        assertTrue(Year.of(2023).isBefore(Year.of(2024)))
        assertEquals("-1", Year.of(-1).toString())
    }

    private data class YearRecordingTemporal(
        val year: Long? = null,
    ) : Temporal {
        override fun isSupported(field: TemporalField): Boolean = field === ChronoField.YEAR

        override fun isSupported(unit: TemporalUnit): Boolean = false

        override fun getLong(field: TemporalField): Long =
            year ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (field === ChronoField.YEAR) copy(year = newValue) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
