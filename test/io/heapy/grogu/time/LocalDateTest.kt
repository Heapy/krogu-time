package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.IsoEra
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
        assertFailsWith<UnsupportedTemporalTypeException> {
            date.get(ChronoField.EPOCH_DAY)
        }
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
    fun formatsAndOrdersIsoDates() {
        assertEquals("2024-02-29", LocalDate.of(2024, 2, 29).toString())
        assertEquals("0000-01-01", LocalDate.of(0, 1, 1).toString())
        assertEquals("-0001-01-01", LocalDate.of(-1, 1, 1).toString())
        assertEquals("+10000-01-01", LocalDate.of(10_000, 1, 1).toString())
        assertTrue(LocalDate.of(2024, 1, 1) < LocalDate.of(2024, 1, 2))
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

        override fun isSupportedBy(temporal: io.heapy.grogu.time.temporal.TemporalAccessor): Boolean =
            temporal is LocalDate

        override fun rangeRefinedBy(
            temporal: io.heapy.grogu.time.temporal.TemporalAccessor,
        ): ValueRange = range

        override fun getFrom(temporal: io.heapy.grogu.time.temporal.TemporalAccessor): Long =
            temporal.getLong(ChronoField.YEAR) + 1

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            @Suppress("UNCHECKED_CAST")
            return temporal.with(ChronoField.YEAR, newValue - 1) as R
        }
    }

    private data class EpochDayRecordingTemporal(
        val epochDay: Long? = null,
    ) : Temporal {
        override fun isSupported(field: TemporalField): Boolean = field === ChronoField.EPOCH_DAY

        override fun isSupported(unit: TemporalUnit): Boolean = false

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
