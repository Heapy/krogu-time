package io.heapy.krogu.time

import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PartialDateIntFieldTest {
    @Test
    fun wideRangeCustomFieldsUseDirectIntValidation() {
        assertDirectValidation(Year.of(2024), 2024)
        assertDirectValidation(MonthDay.of(2, 29), 229)
    }

    private fun assertDirectValidation(temporal: TemporalAccessor, value: Long) {
        val exception = assertFailsWith<DateTimeException> {
            temporal.get(WideRangeField)
        }
        assertFalse(exception is UnsupportedTemporalTypeException)
        assertEquals(
            "Invalid value for WideField " +
                "(valid values -9223372036854775808 - 9223372036854775807): $value",
            exception.message,
        )
    }

    private object WideRangeField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean =
            temporal is Year || temporal is MonthDay

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = when (temporal) {
            is Year -> temporal.value.toLong()
            is MonthDay -> temporal.monthValue * 100L + temporal.dayOfMonth
            else -> throw DateTimeException("Unsupported temporal: $temporal")
        }

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideField"
    }
}
