package io.heapy.krogu.time.temporal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChronoFieldTest {
    @Test
    fun commonFieldRangesMatchIsoRules() {
        assertEquals(ValueRange.of(0, 999_999_999), ChronoField.NANO_OF_SECOND.range)
        assertEquals(ValueRange.of(1, 28, 31), ChronoField.DAY_OF_MONTH.range)
        assertEquals(ValueRange.of(1, 365, 366), ChronoField.DAY_OF_YEAR.range)
        assertEquals(ValueRange.of(1, 12), ChronoField.MONTH_OF_YEAR.range)
        assertEquals(ValueRange.of(-999_999_999, 999_999_999), ChronoField.YEAR.range)
        assertEquals(
            ValueRange.of(-31_557_014_167_219_200, 31_556_889_864_403_199),
            ChronoField.INSTANT_SECONDS.range,
        )
        assertEquals(ValueRange.of(-64_800, 64_800), ChronoField.OFFSET_SECONDS.range)
    }

    @Test
    fun fieldsAreClassifiedAsDateOrTimeBased() {
        ChronoField.entries.takeWhile { it != ChronoField.DAY_OF_WEEK }.forEach { field ->
            assertTrue(field.isTimeBased)
            assertFalse(field.isDateBased)
        }
        ChronoField.entries
            .dropWhile { it != ChronoField.DAY_OF_WEEK }
            .takeWhile { it != ChronoField.INSTANT_SECONDS }
            .forEach { field ->
                assertTrue(field.isDateBased)
                assertFalse(field.isTimeBased)
            }
        listOf(ChronoField.INSTANT_SECONDS, ChronoField.OFFSET_SECONDS).forEach { field ->
            assertFalse(field.isDateBased)
            assertFalse(field.isTimeBased)
        }
    }

    @Test
    fun validationUsesTheFieldNameAndRange() {
        assertEquals(12, ChronoField.MONTH_OF_YEAR.checkValidIntValue(12))
        assertEquals(12, ChronoField.MONTH_OF_YEAR.checkValidValue(12))

        val error = assertFailsWith<io.heapy.krogu.time.DateTimeException> {
            ChronoField.MONTH_OF_YEAR.checkValidValue(13)
        }
        assertEquals(
            "Invalid value for MonthOfYear (valid values 1 - 12): 13",
            error.message,
        )
    }

    @Test
    fun dispatchDelegatesToTheTemporal() {
        val temporal = FieldTemporal(7)

        assertTrue(ChronoField.DAY_OF_WEEK.isSupportedBy(temporal))
        assertEquals(7, ChronoField.DAY_OF_WEEK.getFrom(temporal))
        assertEquals(ValueRange.of(1, 7), ChronoField.DAY_OF_WEEK.rangeRefinedBy(temporal))
        assertEquals(FieldTemporal(3), ChronoField.DAY_OF_WEEK.adjustInto(temporal, 3))
    }

    @Test
    fun unsupportedStandardFieldFailsThroughAccessorDefault() {
        val error = assertFailsWith<UnsupportedTemporalTypeException> {
            FieldTemporal(7).range(ChronoField.MONTH_OF_YEAR)
        }
        assertEquals("Unsupported field: MonthOfYear", error.message)
    }

    private data class FieldTemporal(private val day: Long) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = field === ChronoField.DAY_OF_WEEK

        override fun isSupported(unit: TemporalUnit?): Boolean = unit === ChronoUnit.DAYS

        override fun getLong(field: TemporalField): Long = if (isSupported(field)) {
            day
        } else {
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }

        override fun with(field: TemporalField, newValue: Long): Temporal = if (isSupported(field)) {
            copy(day = newValue)
        } else {
            throw UnsupportedTemporalTypeException("Unsupported field: $field")
        }

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal = if (isSupported(unit)) {
            copy(day = day + amountToAdd)
        } else {
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            (endExclusive as FieldTemporal).day - day
    }
}
