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
    fun additionNormalizesNanosecondsAndSupportsEveryFixedUnit() {
        assertEquals(
            Duration.ofSeconds(3, 100_000_000),
            Duration.ofSeconds(1, 600_000_000) + Duration.ofMillis(1_500),
        )
        assertEquals(Duration.ofDays(2), Duration.ofDays(1).plusDays(1))
        assertEquals(Duration.ofHours(2), Duration.ofHours(1).plusHours(1))
        assertEquals(Duration.ofMinutes(2), Duration.ofMinutes(1).plusMinutes(1))
        assertEquals(Duration.ofSeconds(2), Duration.ofSeconds(1).plusSeconds(1))
        assertEquals(Duration.ofMillis(2), Duration.ofMillis(1).plusMillis(1))
        assertEquals(Duration.ofNanos(2), Duration.ofNanos(1).plusNanos(1))
    }

    @Test
    fun subtractionHandlesLongMinValueWithoutNegatingIt() {
        assertSame(
            Duration.ZERO,
            Duration.ofSeconds(Long.MIN_VALUE).minusSeconds(Long.MIN_VALUE),
        )
        assertEquals(
            Duration.ofNanos(Long.MAX_VALUE).plusNanos(1),
            Duration.ZERO.minusNanos(Long.MIN_VALUE),
        )
        assertFailsWith<ArithmeticException> {
            Duration.ZERO - Duration.ofSeconds(Long.MIN_VALUE)
        }
    }

    @Test
    fun arithmeticDetectsOverflow() {
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MAX_VALUE).plusSeconds(1)
        }
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MIN_VALUE).minusSeconds(1)
        }
        assertFailsWith<ArithmeticException> {
            Duration.ZERO.plusDays(Long.MAX_VALUE)
        }
    }

    @Test
    fun negatedAndAbsoluteSwapOrRemoveTheSign() {
        val positive = Duration.ofSeconds(1, 500_000_000)
        val negative = Duration.ofSeconds(-2, 500_000_000)

        assertEquals(negative, positive.negated())
        assertEquals(positive, negative.negated())
        assertSame(positive, positive.absoluteValue())
        assertEquals(positive, negative.absoluteValue())
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MIN_VALUE).negated()
        }
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
