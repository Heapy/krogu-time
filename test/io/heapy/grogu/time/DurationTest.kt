package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DurationTest {
    @Test
    fun zeroIsCanonical() {
        assertSame(Duration.ZERO, Duration.ofSeconds(0))
        assertSame(Duration.ZERO, Duration.ofSeconds(0, 0))
        assertSame(Duration.ZERO, Duration.ofMillis(0))
        assertSame(Duration.ZERO, Duration.ofNanos(0))
        assertTrue(Duration.ZERO.isZero)
        assertFalse(Duration.ZERO.isPositive)
        assertFalse(Duration.ZERO.isNegative)
    }

    @Test
    fun factoriesUseStandardUnitLengths() {
        assertEquals(Duration.ofSeconds(172_800), Duration.ofDays(2))
        assertEquals(Duration.ofSeconds(7_200), Duration.ofHours(2))
        assertEquals(Duration.ofSeconds(120), Duration.ofMinutes(2))
        assertEquals(Duration.ofSeconds(1, 500_000_000), Duration.ofMillis(1_500))
        assertEquals(Duration.ofSeconds(1, 500_000_000), Duration.ofNanos(1_500_000_000))
    }

    @Test
    fun factoriesNormalizeNegativeFractions() {
        val minusOneMillisecond = Duration.ofMillis(-1)
        assertEquals(-1, minusOneMillisecond.seconds)
        assertEquals(999_000_000, minusOneMillisecond.nano)

        val minusOneNanosecond = Duration.ofNanos(-1)
        assertEquals(-1, minusOneNanosecond.seconds)
        assertEquals(999_999_999, minusOneNanosecond.nano)

        assertEquals(Duration.ofSeconds(3, 1), Duration.ofSeconds(2, 1_000_000_001))
        assertEquals(Duration.ofSeconds(3, 1), Duration.ofSeconds(4, -999_999_999))
    }

    @Test
    fun unitFactoriesDetectOverflow() {
        assertFailsWith<ArithmeticException> { Duration.ofDays(Long.MAX_VALUE) }
        assertFailsWith<ArithmeticException> { Duration.ofHours(Long.MIN_VALUE) }
        assertFailsWith<ArithmeticException> { Duration.ofMinutes(Long.MAX_VALUE) }
        assertFailsWith<ArithmeticException> { Duration.ofSeconds(Long.MAX_VALUE, 1_000_000_000) }
    }

    @Test
    fun replacementMethodsRetainTheOtherComponent() {
        val duration = Duration.ofSeconds(2, 3)

        assertEquals(Duration.ofSeconds(5, 3), duration.withSeconds(5))
        assertEquals(Duration.ofSeconds(2, 7), duration.withNanos(7))
        assertFailsWith<DateTimeException> { duration.withNanos(-1) }
        assertFailsWith<DateTimeException> { duration.withNanos(1_000_000_000) }
    }

    @Test
    fun signUsesTheCompleteNormalizedValue() {
        assertTrue(Duration.ofNanos(1).isPositive)
        assertFalse(Duration.ofNanos(1).isNegative)
        assertTrue(Duration.ofNanos(-1).isNegative)
        assertFalse(Duration.ofNanos(-1).isPositive)
    }

    @Test
    fun comparisonUsesSecondsThenNanoseconds() {
        val values = listOf(
            Duration.ofNanos(-1),
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofSeconds(1),
        )

        assertEquals(values, values.reversed().sorted())
    }

    @Test
    fun stringUsesIso8601SecondsRepresentation() {
        assertEquals("PT0S", Duration.ZERO.toString())
        assertEquals("PT48H", Duration.ofDays(2).toString())
        assertEquals("PT1H1M1S", Duration.ofSeconds(3_661).toString())
        assertEquals("PT1.5S", Duration.ofMillis(1_500).toString())
        assertEquals("PT-0.001S", Duration.ofMillis(-1).toString())
        assertEquals("PT-1.5S", Duration.ofMillis(-1_500).toString())
    }
}
