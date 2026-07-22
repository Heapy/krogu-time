package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.ValueRange
import java.time.MonthDay as JavaMonthDay
import java.time.Year as JavaYear
import java.time.temporal.ChronoUnit as JavaChronoUnit
import java.time.temporal.Temporal as JavaTemporal
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalField as JavaTemporalField
import java.time.temporal.TemporalUnit as JavaTemporalUnit
import java.time.temporal.ValueRange as JavaValueRange
import kotlin.test.Test
import kotlin.test.assertEquals

class PartialDateIntFieldJavaConformanceTest {
    @Test
    fun wideRangeCustomFieldValidationMatchesJavaTime() {
        assertSameOutcome(
            javaOperation = { JavaYear.of(2024).get(JavaWideRangeField) },
            kotlinOperation = { Year.of(2024).get(WideRangeField) },
        )
        assertSameOutcome(
            javaOperation = { JavaMonthDay.of(2, 29).get(JavaWideRangeField) },
            kotlinOperation = { MonthDay.of(2, 29).get(WideRangeField) },
        )
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
        assertEquals(
            javaResult.exceptionOrNull()?.message,
            kotlinResult.exceptionOrNull()?.message,
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

    private object JavaWideRangeField : JavaTemporalField {
        override fun getBaseUnit(): JavaTemporalUnit = JavaChronoUnit.DAYS

        override fun getRangeUnit(): JavaTemporalUnit = JavaChronoUnit.FOREVER

        override fun range(): JavaValueRange = JavaValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)

        override fun isDateBased(): Boolean = true

        override fun isTimeBased(): Boolean = false

        override fun isSupportedBy(temporal: JavaTemporalAccessor): Boolean =
            temporal is JavaYear || temporal is JavaMonthDay

        override fun rangeRefinedBy(temporal: JavaTemporalAccessor): JavaValueRange = range()

        override fun getFrom(temporal: JavaTemporalAccessor): Long = when (temporal) {
            is JavaYear -> temporal.value.toLong()
            is JavaMonthDay -> temporal.monthValue * 100L + temporal.dayOfMonth
            else -> throw java.time.DateTimeException("Unsupported temporal: $temporal")
        }

        override fun <R : JavaTemporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = "WideField"
    }
}
