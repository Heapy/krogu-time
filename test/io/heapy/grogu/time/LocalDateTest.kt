package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.IsoEra
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalDateTest {
    @Test
    fun validatesCalendarDates() {
        assertEquals(LocalDate.of(2024, Month.FEBRUARY, 29), LocalDate.of(2024, 2, 29))
        assertFailsWith<DateTimeException> { LocalDate.of(2023, 2, 29) }
        assertFailsWith<DateTimeException> { LocalDate.of(2024, 4, 31) }
        assertFailsWith<DateTimeException> { LocalDate.of(2024, 13, 1) }
        assertFailsWith<DateTimeException> { LocalDate.of(Year.MAX_VALUE + 1, 1, 1) }
    }

    @Test
    fun parsesStrictIsoLocalDates() {
        val cases = mapOf(
            "0000-01-01" to LocalDate.of(0, 1, 1),
            "2024-02-29" to LocalDate.of(2024, 2, 29),
            "-0001-01-01" to LocalDate.of(-1, 1, 1),
            "+10000-01-01" to LocalDate.of(10_000, 1, 1),
            "-999999999-01-01" to LocalDate.MIN,
            "+999999999-12-31" to LocalDate.MAX,
        )
        cases.forEach { (text, expected) -> assertEquals(expected, LocalDate.parse(text), text) }

        val invalidInputs = mapOf(
            "" to 0,
            "2024-2-29" to 5,
            "2024-02-9" to 8,
            "2024/02/29" to 4,
            "2024-02/29" to 7,
            "2023-02-29" to 0,
            "2024-13-01" to 0,
            "2024-02-30" to 0,
            "999-01-01" to 0,
            "+2024-01-01" to 0,
            "10000-01-01" to 0,
            "2024-02-29Z" to 10,
            " 2024-02-29" to 0,
            "2024-02-29 " to 10,
            "２０２４-０２-２９" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) { LocalDate.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }

    @Test
    fun createsDatesFromDayOfYear() {
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.ofYearDay(2024, 60))
        assertEquals(LocalDate.of(2023, 12, 31), LocalDate.ofYearDay(2023, 365))
        assertFailsWith<DateTimeException> { LocalDate.ofYearDay(2023, 366) }
        assertFailsWith<DateTimeException> { LocalDate.ofYearDay(2024, 0) }
    }

    @Test
    fun convertsEpochDaysAcrossTheFullSupportedRange() {
        val known = listOf(
            LocalDate.of(1969, 12, 31) to -1L,
            LocalDate.of(1970, 1, 1) to 0L,
            LocalDate.of(2000, 2, 29) to 11_016L,
            LocalDate.of(2024, 2, 29) to 19_782L,
        )
        known.forEach { (date, epochDay) ->
            assertEquals(epochDay, date.toEpochDay())
            assertEquals(date, LocalDate.ofEpochDay(epochDay))
        }
        assertEquals(LocalDate.MIN, LocalDate.ofEpochDay(LocalDate.MIN.toEpochDay()))
        assertEquals(LocalDate.MAX, LocalDate.ofEpochDay(LocalDate.MAX.toEpochDay()))
    }

    @Test
    fun convertsBetweenInstantsDatesTimesAndOffsets() {
        val instant = Instant.parse("2024-02-29T23:30:00Z")

        assertEquals(
            LocalDate.of(2024, 3, 1),
            LocalDate.ofInstant(instant, ZoneOffset.ofHours(1)),
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.ofInstant(instant, ZoneOffset.ofHours(-1)),
        )
        assertEquals(
            LocalDate.of(2024, 3, 1),
            LocalDate.ofInstant(instant, ZoneId.of("Europe/Paris")),
        )
        assertEquals(
            -3_600L,
            LocalDate.EPOCH.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.ofHours(1)),
        )
        assertEquals(
            instant.epochSecond,
            LocalDate.of(2024, 2, 29).toEpochSecond(
                LocalTime.of(23, 30),
                ZoneOffset.UTC,
            ),
        )
    }

    @Test
    fun producesLazyExclusiveDateSequences() {
        val start = LocalDate.of(2024, 2, 27)
        val end = LocalDate.of(2024, 3, 2)

        assertEquals(
            listOf(
                LocalDate.of(2024, 2, 27),
                LocalDate.of(2024, 2, 28),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1),
            ),
            start.datesUntil(end).toList(),
        )
        assertEquals(emptyList(), start.datesUntil(start).toList())
        assertEquals(
            listOf(LocalDate.MAX.minusDays(1)),
            LocalDate.MAX.minusDays(1).datesUntil(LocalDate.MAX).toList(),
        )
        assertEquals(
            listOf(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1)),
            LocalDate.EPOCH.datesUntil(LocalDate.MAX).take(2).toList(),
        )
        assertFailsWith<IllegalArgumentException> {
            end.datesUntil(start)
        }
    }

    @Test
    fun producesCalendarAwareSteppedDateSequences() {
        assertEquals(
            listOf(
                LocalDate.of(2015, 1, 31),
                LocalDate.of(2015, 2, 28),
                LocalDate.of(2015, 3, 31),
                LocalDate.of(2015, 4, 30),
            ),
            LocalDate.of(2015, 1, 31)
                .datesUntil(LocalDate.of(2015, 5, 1), Period.ofMonths(1))
                .toList(),
        )
        assertEquals(
            listOf(
                LocalDate.of(2015, 5, 31),
                LocalDate.of(2015, 4, 30),
                LocalDate.of(2015, 3, 31),
                LocalDate.of(2015, 2, 28),
                LocalDate.of(2015, 1, 31),
            ),
            LocalDate.of(2015, 5, 31)
                .datesUntil(LocalDate.of(2015, 1, 1), Period.ofMonths(-1))
                .toList(),
        )
        assertEquals(
            listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)),
            LocalDate.of(2024, 1, 1)
                .datesUntil(LocalDate.of(2024, 1, 3), Period.of(1, -12, 1))
                .toList(),
        )

        assertFailsWith<IllegalArgumentException> {
            LocalDate.EPOCH.datesUntil(LocalDate.EPOCH, Period.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            LocalDate.EPOCH.datesUntil(LocalDate.EPOCH.plusDays(1), Period.of(0, 1, -1))
        }
        assertFailsWith<IllegalArgumentException> {
            LocalDate.EPOCH.datesUntil(LocalDate.EPOCH.minusDays(1), Period.ofDays(1))
        }
        assertFailsWith<IllegalArgumentException> {
            LocalDate.EPOCH.datesUntil(LocalDate.EPOCH.plusDays(1), Period.ofDays(-1))
        }
    }

    @Test
    fun exposesCalendarProperties() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(2024, leapDay.year)
        assertEquals(Month.FEBRUARY, leapDay.month)
        assertEquals(2, leapDay.monthValue)
        assertEquals(29, leapDay.dayOfMonth)
        assertEquals(60, leapDay.dayOfYear)
        assertEquals(DayOfWeek.THURSDAY, leapDay.dayOfWeek)
        assertTrue(leapDay.isLeapYear)
        assertEquals(29, leapDay.lengthOfMonth())
        assertEquals(366, leapDay.lengthOfYear())

        val commonDate = LocalDate.of(2023, 2, 28)
        assertFalse(commonDate.isLeapYear)
        assertEquals(28, commonDate.lengthOfMonth())
        assertEquals(365, commonDate.lengthOfYear())
    }

    @Test
    fun exposesStandardDateFieldsAndRefinedRanges() {
        val date = LocalDate.of(2024, 2, 29)
        val expectedFields = mapOf(
            ChronoField.DAY_OF_WEEK to 4L,
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH to 1L,
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR to 4L,
            ChronoField.DAY_OF_MONTH to 29L,
            ChronoField.DAY_OF_YEAR to 60L,
            ChronoField.EPOCH_DAY to 19_782L,
            ChronoField.ALIGNED_WEEK_OF_MONTH to 5L,
            ChronoField.ALIGNED_WEEK_OF_YEAR to 9L,
            ChronoField.MONTH_OF_YEAR to 2L,
            ChronoField.PROLEPTIC_MONTH to 24_289L,
            ChronoField.YEAR_OF_ERA to 2_024L,
            ChronoField.YEAR to 2_024L,
            ChronoField.ERA to 1L,
        )

        ChronoField.entries.forEach { field ->
            assertEquals(field in expectedFields, date.isSupported(field), field.toString())
        }
        expectedFields.forEach { (field, value) ->
            assertEquals(value, date.getLong(field), field.toString())
        }
        assertEquals(ValueRange.of(1, 29), date.range(ChronoField.DAY_OF_MONTH))
        assertEquals(ValueRange.of(1, 366), date.range(ChronoField.DAY_OF_YEAR))
        assertEquals(ValueRange.of(1, 5), date.range(ChronoField.ALIGNED_WEEK_OF_MONTH))
        assertEquals(
            ValueRange.of(1, Year.MAX_VALUE.toLong()),
            date.range(ChronoField.YEAR_OF_ERA),
        )
        assertEquals(IsoEra.CE, date.era)

        val bce = LocalDate.of(-1, 3, 1)
        assertEquals(-10L, bce.getLong(ChronoField.PROLEPTIC_MONTH))
        assertEquals(2L, bce.getLong(ChronoField.YEAR_OF_ERA))
        assertEquals(0L, bce.getLong(ChronoField.ERA))
        assertEquals(IsoEra.BCE, bce.era)
        assertEquals(
            ValueRange.of(1, Year.MAX_VALUE.toLong() + 1),
            bce.range(ChronoField.YEAR_OF_ERA),
        )

        val commonFebruary = LocalDate.of(2023, 2, 1)
        assertEquals(
            ValueRange.of(1, 4),
            commonFebruary.range(ChronoField.ALIGNED_WEEK_OF_MONTH),
        )
        assertFailsWith<UnsupportedTemporalTypeException> {
            date.getLong(ChronoField.HOUR_OF_DAY)
        }
        val epochDayException = assertFailsWith<UnsupportedTemporalTypeException> {
            date.get(ChronoField.EPOCH_DAY)
        }
        assertEquals(
            "Invalid field 'EpochDay' for get() method, use getLong() instead",
            epochDayException.message,
        )
        val prolepticMonthException = assertFailsWith<UnsupportedTemporalTypeException> {
            date.get(ChronoField.PROLEPTIC_MONTH)
        }
        assertEquals(
            "Invalid field 'ProlepticMonth' for get() method, use getLong() instead",
            prolepticMonthException.message,
        )
    }

    @Test
    fun delegatesCustomFieldsAndAdjustsOtherTemporalsByEpochDay() {
        val date = LocalDate.of(2024, 2, 29)
        assertTrue(date.isSupported(NextYearField))
        assertEquals(NextYearField.range, date.range(NextYearField))
        assertEquals(2_025L, date.getLong(NextYearField))
        assertEquals(
            EpochDayRecordingTemporal(19_782),
            date.adjustInto(EpochDayRecordingTemporal()),
        )
    }

    @Test
    fun replacesDateComponentsAndTemporalFields() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2023, 2, 28), leapDay.withYear(2023))
        assertEquals(LocalDate.of(2024, 1, 29), leapDay.withMonth(1))
        assertEquals(LocalDate.of(2024, 2, 28), leapDay.withDayOfMonth(28))
        assertEquals(LocalDate.of(2024, 12, 31), leapDay.withDayOfYear(366))
        assertFailsWith<DateTimeException> { leapDay.withDayOfMonth(30) }
        assertFailsWith<DateTimeException> { LocalDate.of(2023, 1, 1).withDayOfYear(366) }

        assertEquals(
            LocalDate.of(2024, 2, 26),
            leapDay.with(ChronoField.DAY_OF_WEEK, 1),
        )
        assertEquals(
            LocalDate.of(2024, 2, 1),
            leapDay.with(ChronoField.ALIGNED_WEEK_OF_MONTH, 1),
        )
        assertEquals(
            LocalDate.of(1970, 1, 1),
            leapDay.with(ChronoField.EPOCH_DAY, 0),
        )
        assertEquals(
            LocalDate.of(2024, 3, 29),
            leapDay.with(ChronoField.PROLEPTIC_MONTH, 24_290),
        )
        assertEquals(
            LocalDate.of(-2023, 2, 28),
            leapDay.with(ChronoField.ERA, 0),
        )
        assertEquals(LocalDate.of(2025, 2, 28), leapDay.with(NextYearField, 2_026))
    }

    @Test
    fun supportsDateUnitsAndCalendarArithmetic() {
        val date = LocalDate.of(2024, 1, 31)
        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit.isDateBased, date.isSupported(unit), unit.toString())
        }

        assertEquals(LocalDate.of(2025, 1, 31), date.plusYears(1))
        assertEquals(LocalDate.of(2024, 2, 29), date.plusMonths(1))
        assertEquals(LocalDate.of(2024, 2, 7), date.plusWeeks(1))
        assertEquals(LocalDate.of(2024, 2, 1), date.plusDays(1))
        assertEquals(LocalDate.of(2023, 12, 31), date.minusMonths(1))
        assertEquals(LocalDate.of(2024, 1, 24), date.minusWeeks(1))
        assertEquals(LocalDate.of(2024, 1, 30), date.minusDays(1))
        assertEquals(LocalDate.of(2034, 1, 31), date.plus(1, ChronoUnit.DECADES))
        assertEquals(LocalDate.of(-2023, 1, 31), date.minus(1, ChronoUnit.ERAS))
        assertFailsWith<UnsupportedTemporalTypeException> {
            date.plus(1, ChronoUnit.HOURS)
        }
        assertFailsWith<DateTimeException> { LocalDate.MAX.plusDays(1) }
        assertFailsWith<ArithmeticException> { date.plusWeeks(Long.MAX_VALUE) }

        assertEquals(LocalDate.of(2024, 2, 2), date.plus(TwoDaysAmount))
        assertEquals(LocalDate.of(2024, 1, 29), date.minus(TwoDaysAmount))
        assertTrue(date.isSupported(TwoDayUnit))
        assertEquals(LocalDate.of(2024, 2, 4), date.plus(2, TwoDayUnit))
    }

    @Test
    fun calculatesCompleteUnitsUntilOtherDates() {
        val start = LocalDate.of(2024, 1, 31)
        assertEquals(60, start.until(LocalDate.of(2024, 3, 31), ChronoUnit.DAYS))
        assertEquals(8, start.until(LocalDate.of(2024, 3, 31), ChronoUnit.WEEKS))
        assertEquals(1, start.until(LocalDate.of(2024, 3, 30), ChronoUnit.MONTHS))
        assertEquals(2, start.until(LocalDate.of(2024, 3, 31), ChronoUnit.MONTHS))
        assertEquals(1, start.until(LocalDate.of(2025, 1, 31), ChronoUnit.YEARS))
        assertEquals(-1, start.until(LocalDate.of(2023, 1, 31), ChronoUnit.YEARS))
        assertEquals(30, start.until(LocalDate.of(2024, 3, 31), TwoDayUnit))
        assertFailsWith<UnsupportedTemporalTypeException> {
            start.until(LocalDate.of(2024, 2, 1), ChronoUnit.HOURS)
        }

        assertEquals(start, LocalDate.from(start))
        assertEquals(
            LocalDate.of(2024, 1, 31),
            LocalDate.from(EpochDayRecordingTemporal(start.toEpochDay())),
        )
    }

    @Test
    fun formatsAndOrdersIsoDates() {
        assertEquals("2024-02-29", LocalDate.of(2024, 2, 29).toString())
        assertEquals("0000-01-01", LocalDate.of(0, 1, 1).toString())
        assertEquals("-0001-01-01", LocalDate.of(-1, 1, 1).toString())
        assertEquals("+10000-01-01", LocalDate.of(10_000, 1, 1).toString())
        assertTrue(LocalDate.of(2024, 1, 1) < LocalDate.of(2024, 1, 2))
        assertTrue(LocalDate.of(2024, 1, 2).isAfter(LocalDate.of(2024, 1, 1)))
        assertTrue(LocalDate.of(2024, 1, 1).isBefore(LocalDate.of(2024, 1, 2)))
        assertTrue(LocalDate.of(2024, 1, 1).isEqual(LocalDate.of(2024, 1, 1)))
        assertEquals(LocalDate.of(1970, 1, 1), LocalDate.EPOCH)
    }

    private object NextYearField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.YEARS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(
            Year.MIN_VALUE.toLong() + 1,
            Year.MAX_VALUE.toLong() + 1,
        )
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean =
            temporal is LocalDate

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long =
            temporal.getLong(ChronoField.YEAR) + 1

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            @Suppress("UNCHECKED_CAST")
            return temporal.with(ChronoField.YEAR, newValue - 1) as R
        }
    }

    private object TwoDaysAmount : TemporalAmount {
        override val units: List<TemporalUnit> = listOf(ChronoUnit.DAYS)

        override fun get(unit: TemporalUnit): Long =
            if (unit === ChronoUnit.DAYS) 2 else
                throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun addTo(temporal: Temporal): Temporal = temporal.plus(2, ChronoUnit.DAYS)

        override fun subtractFrom(temporal: Temporal): Temporal = temporal.minus(2, ChronoUnit.DAYS)
    }

    private object TwoDayUnit : TemporalUnit {
        override val duration: Duration = Duration.ofDays(2)
        override val isDurationEstimated: Boolean = true
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: Temporal): Boolean = temporal is LocalDate

        override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
            @Suppress("UNCHECKED_CAST")
            return temporal.plus(amount * 2, ChronoUnit.DAYS) as R
        }

        override fun between(
            temporal1Inclusive: Temporal,
            temporal2Exclusive: Temporal,
        ): Long = temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.DAYS) / 2
    }

    private data class EpochDayRecordingTemporal(
        val epochDay: Long? = null,
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.EPOCH_DAY

        override fun isSupported(unit: TemporalUnit?): Boolean = false

        override fun getLong(field: TemporalField): Long =
            epochDay ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (field === ChronoField.EPOCH_DAY) copy(epochDay = newValue) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
