package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.DateTimeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ValueRangeTest {
    @Test
    fun fixedRangeExposesItsBounds() {
        val range = ValueRange.of(1, 12)

        assertTrue(range.isFixed)
        assertEquals(1, range.minimum)
        assertEquals(1, range.largestMinimum)
        assertEquals(12, range.smallestMaximum)
        assertEquals(12, range.maximum)
        assertEquals("1 - 12", range.toString())
    }

    @Test
    fun variableRangesExposeAllBounds() {
        val variableMaximum = ValueRange.of(1, 28, 31)
        assertFalse(variableMaximum.isFixed)
        assertEquals("1 - 28/31", variableMaximum.toString())

        val fullyVariable = ValueRange.of(1, 2, 28, 31)
        assertFalse(fullyVariable.isFixed)
        assertEquals(1, fullyVariable.minimum)
        assertEquals(2, fullyVariable.largestMinimum)
        assertEquals(28, fullyVariable.smallestMaximum)
        assertEquals(31, fullyVariable.maximum)
        assertEquals("1/2 - 28/31", fullyVariable.toString())
    }

    @Test
    fun factoriesRejectInconsistentBounds() {
        assertFailsWith<IllegalArgumentException> { ValueRange.of(2, 1) }
        assertFailsWith<IllegalArgumentException> { ValueRange.of(2, 1, 3) }
        assertFailsWith<IllegalArgumentException> { ValueRange.of(2, 1, 3, 4) }
        assertFailsWith<IllegalArgumentException> { ValueRange.of(1, 2, 4, 3) }
        assertEquals("1/5 - 4/6", ValueRange.of(1, 5, 4, 6).toString())
        assertFailsWith<IllegalArgumentException> { ValueRange.of(5, 6, 4, 7) }
    }

    @Test
    fun validatesLongValuesAgainstOuterBounds() {
        val range = ValueRange.of(-2, 3)

        assertFalse(range.isValidValue(-3))
        assertTrue(range.isValidValue(-2))
        assertTrue(range.isValidValue(3))
        assertFalse(range.isValidValue(4))
        assertEquals(-2, range.checkValidValue(-2, null))

        val error = assertFailsWith<DateTimeException> {
            range.checkValidValue(4, NamedField("ExampleField"))
        }
        assertEquals("Invalid value for ExampleField (valid values -2 - 3): 4", error.message)
    }

    @Test
    fun intValidationRequiresTheEntireRangeToFitInAnInt() {
        val intRange = ValueRange.of(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        assertTrue(intRange.isIntValue)
        assertTrue(intRange.isValidIntValue(Int.MAX_VALUE.toLong()))
        assertEquals(Int.MIN_VALUE, intRange.checkValidIntValue(Int.MIN_VALUE.toLong(), null))

        val longRange = ValueRange.of(Int.MIN_VALUE.toLong() - 1, Int.MAX_VALUE.toLong())
        assertFalse(longRange.isIntValue)
        assertFalse(longRange.isValidIntValue(0))
        val error = assertFailsWith<DateTimeException> {
            longRange.checkValidIntValue(0, null)
        }
        assertEquals(
            "Invalid value (valid values -2147483649 - 2147483647): 0",
            error.message,
        )
    }

    @Test
    fun equalityAndHashCodeUseAllFourBounds() {
        val first = ValueRange.of(1, 2, 28, 31)
        val equal = ValueRange.of(1, 2, 28, 31)
        val different = ValueRange.of(1, 2, 29, 31)

        assertEquals(first, equal)
        assertEquals(first.hashCode(), equal.hashCode())
        assertNotEquals(first, different)
        assertFalse(first.equals("1/2 - 28/31"))
    }

    private class NamedField(private val name: String) : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        override val isDateBased: Boolean = false
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = false

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long =
            throw UnsupportedTemporalTypeException("Unsupported field: $name")

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = name
    }
}
