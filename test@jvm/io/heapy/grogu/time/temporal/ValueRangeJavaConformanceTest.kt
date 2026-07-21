package io.heapy.grogu.time.temporal

import java.time.temporal.ValueRange as JavaValueRange
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueRangeJavaConformanceTest {
    @Test
    fun observableBehaviorMatchesJavaTime() {
        val cases = listOf(
            ValueRange.of(-12, 12) to JavaValueRange.of(-12, 12),
            ValueRange.of(1, 28, 31) to JavaValueRange.of(1, 28, 31),
            ValueRange.of(1, 2, 28, 31) to JavaValueRange.of(1, 2, 28, 31),
            ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE) to
                JavaValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE),
        )

        cases.forEach { (range, javaRange) ->
            assertEquals(javaRange.isFixed, range.isFixed)
            assertEquals(javaRange.minimum, range.minimum)
            assertEquals(javaRange.largestMinimum, range.largestMinimum)
            assertEquals(javaRange.smallestMaximum, range.smallestMaximum)
            assertEquals(javaRange.maximum, range.maximum)
            assertEquals(javaRange.isIntValue, range.isIntValue)
            assertEquals(javaRange.toString(), range.toString())

            listOf(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE).forEach { value ->
                assertEquals(javaRange.isValidValue(value), range.isValidValue(value))
                assertEquals(javaRange.isValidIntValue(value), range.isValidIntValue(value))
            }
        }
    }
}
