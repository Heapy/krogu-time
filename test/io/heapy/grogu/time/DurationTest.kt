package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
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
    fun temporalAmountExposesSecondsAndNanoseconds() {
        val duration = Duration.ofSeconds(-2, 500_000_000)

        assertEquals(listOf(ChronoUnit.SECONDS, ChronoUnit.NANOS), duration.units)
        assertEquals(-2, duration.get(ChronoUnit.SECONDS))
        assertEquals(500_000_000, duration.get(ChronoUnit.NANOS))
        assertFailsWith<UnsupportedTemporalTypeException> {
            duration.get(ChronoUnit.MILLIS)
        }
    }

    @Test
    fun createsAndCalculatesUsingExactTemporalUnits() {
        assertEquals(Duration.ofNanos(3), Duration.of(3, ChronoUnit.NANOS))
        assertEquals(Duration.ofNanos(3_000), Duration.of(3, ChronoUnit.MICROS))
        assertEquals(Duration.ofMillis(3), Duration.of(3, ChronoUnit.MILLIS))
        assertEquals(Duration.ofSeconds(3), Duration.of(3, ChronoUnit.SECONDS))
        assertEquals(Duration.ofMinutes(3), Duration.of(3, ChronoUnit.MINUTES))
        assertEquals(Duration.ofHours(3), Duration.of(3, ChronoUnit.HOURS))
        assertEquals(Duration.ofHours(36), Duration.of(3, ChronoUnit.HALF_DAYS))
        assertEquals(Duration.ofDays(3), Duration.of(3, ChronoUnit.DAYS))

        val base = Duration.ofSeconds(1)
        assertEquals(Duration.ofMillis(1_001), base.plus(1, ChronoUnit.MILLIS))
        assertEquals(Duration.ofMillis(999), base.minus(1, ChronoUnit.MILLIS))
    }

    @Test
    fun rejectsEstimatedUnitsEvenForAZeroAmount() {
        val error = assertFailsWith<UnsupportedTemporalTypeException> {
            Duration.ZERO.plus(0, ChronoUnit.MONTHS)
        }
        assertEquals("Unit must not have an estimated duration", error.message)
    }

    @Test
    fun customExactUnitsUseTheirCompleteDuration() {
        assertEquals(Duration.ofMillis(4_500), Duration.of(3, ONE_AND_A_HALF_SECONDS))
        assertEquals(
            Duration.ofSeconds(6),
            Duration.ofMillis(1_500).plus(3, ONE_AND_A_HALF_SECONDS),
        )
    }

    @Test
    fun multiplicationUsesTheCompleteNormalizedValueAndDetectsOverflow() {
        val duration = Duration.ofMillis(1_500)

        assertSame(Duration.ZERO, duration.multipliedBy(0))
        assertSame(duration, duration.multipliedBy(1))
        assertEquals(Duration.ofMillis(4_500), duration.multipliedBy(3))
        assertEquals(Duration.ofMillis(-4_500), duration.multipliedBy(-3))
        assertEquals(Duration.ofNanos(Long.MAX_VALUE), Duration.ofNanos(1).multipliedBy(Long.MAX_VALUE))
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MAX_VALUE).multipliedBy(2)
        }
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MIN_VALUE).multipliedBy(-1)
        }
    }

    @Test
    fun scalarDivisionTruncatesTowardZeroAtNanosecondPrecision() {
        assertEquals(Duration.ofMillis(750), Duration.ofMillis(1_500).dividedBy(2))
        assertEquals(Duration.ofMillis(-750), Duration.ofMillis(-1_500).dividedBy(2))
        assertSame(Duration.ZERO, Duration.ofNanos(1).dividedBy(2))
        assertSame(Duration.ZERO, Duration.ofNanos(-1).dividedBy(2))
        assertEquals(
            Duration.ofSeconds(Long.MIN_VALUE / 2),
            Duration.ofSeconds(Long.MIN_VALUE).dividedBy(2),
        )
        assertFailsWith<ArithmeticException> { Duration.ofSeconds(1).dividedBy(0) }
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MIN_VALUE).dividedBy(-1)
        }
    }

    @Test
    fun durationDivisionReturnsTheWholeRatioAndDetectsOverflow() {
        assertEquals(2, Duration.ofMillis(5_500).dividedBy(Duration.ofSeconds(2)))
        assertEquals(-2, Duration.ofMillis(-5_500).dividedBy(Duration.ofSeconds(2)))
        assertEquals(-2, Duration.ofMillis(5_500).dividedBy(Duration.ofSeconds(-2)))
        assertEquals(3, Duration.ofSeconds(1).dividedBy(Duration.ofMillis(333)))
        assertFailsWith<ArithmeticException> { ONE_SECOND.dividedBy(Duration.ZERO) }
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MAX_VALUE).dividedBy(Duration.ofNanos(1))
        }
    }

    @Test
    fun truncatesTowardZeroToStandardAndCustomUnits() {
        val positive = Duration.ofSeconds(90_061, 987_654_321)

        assertSame(positive, positive.truncatedTo(ChronoUnit.NANOS))
        assertEquals(Duration.ofSeconds(90_061, 987_654_000), positive.truncatedTo(ChronoUnit.MICROS))
        assertEquals(Duration.ofSeconds(90_061, 987_000_000), positive.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(Duration.ofSeconds(90_061), positive.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(Duration.ofSeconds(90_060), positive.truncatedTo(ChronoUnit.MINUTES))
        assertEquals(Duration.ofSeconds(90_000), positive.truncatedTo(ChronoUnit.HOURS))
        assertEquals(Duration.ofSeconds(86_400), positive.truncatedTo(ChronoUnit.HALF_DAYS))
        assertEquals(Duration.ofSeconds(86_400), positive.truncatedTo(ChronoUnit.DAYS))
        assertEquals(Duration.ofSeconds(86_400), positive.truncatedTo(NINETY_MINUTES))

        val negative = Duration.ofMillis(-61_750)
        assertEquals(Duration.ofSeconds(-61), negative.truncatedTo(ChronoUnit.SECONDS))
        assertEquals(Duration.ofSeconds(-60), negative.truncatedTo(ChronoUnit.MINUTES))
    }

    @Test
    fun truncationRejectsUnitsThatAreTooLargeOrDoNotDivideTheDay() {
        val tooLarge = assertFailsWith<UnsupportedTemporalTypeException> {
            Duration.ofSeconds(1).truncatedTo(ChronoUnit.WEEKS)
        }
        assertEquals("Unit is too large to be used for truncation", tooLarge.message)

        val uneven = assertFailsWith<UnsupportedTemporalTypeException> {
            Duration.ofSeconds(1).truncatedTo(SEVEN_MINUTES)
        }
        assertEquals("Unit must divide into a standard day without remainder", uneven.message)
    }

    @Test
    fun createsDurationFromAnyTemporalAmount() {
        val amount = object : TemporalAmount {
            override val units: List<TemporalUnit> =
                listOf(ChronoUnit.MINUTES, ChronoUnit.SECONDS, ChronoUnit.NANOS)

            override fun get(unit: TemporalUnit): Long = when (unit) {
                ChronoUnit.MINUTES -> 2
                ChronoUnit.SECONDS -> 3
                ChronoUnit.NANOS -> 4
                else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
            }

            override fun addTo(temporal: Temporal): Temporal = error("Not used")

            override fun subtractFrom(temporal: Temporal): Temporal = error("Not used")
        }

        assertEquals(Duration.ofSeconds(123, 4), Duration.from(amount))
    }

    @Test
    fun addsToAndSubtractsFromTemporalsInSecondsThenNanosOrder() {
        val duration = Duration.ofSeconds(2, 3)

        assertEquals(
            RecordingTemporal(listOf(ChronoUnit.SECONDS to 2L, ChronoUnit.NANOS to 3L)),
            duration.addTo(RecordingTemporal()),
        )
        assertEquals(
            RecordingTemporal(listOf(ChronoUnit.SECONDS to -2L, ChronoUnit.NANOS to -3L)),
            duration.subtractFrom(RecordingTemporal()),
        )
        assertEquals(RecordingTemporal(), Duration.ZERO.addTo(RecordingTemporal()))
    }

    @Test
    fun convertsToWholeUnitsAndNormalizedComponents() {
        val positive = Duration.ofSeconds(90_061, 987_654_321)

        assertEquals(1, positive.toDays())
        assertEquals(25, positive.toHours())
        assertEquals(1_501, positive.toMinutes())
        assertEquals(90_061, positive.toSeconds())
        assertEquals(90_061_987, positive.toMillis())
        assertEquals(90_061_987_654_321, positive.toNanos())
        assertEquals(1, positive.toDaysPart())
        assertEquals(1, positive.toHoursPart())
        assertEquals(1, positive.toMinutesPart())
        assertEquals(1, positive.toSecondsPart())
        assertEquals(987, positive.toMillisPart())
        assertEquals(987_654_321, positive.toNanosPart())

        val negative = positive.negated()
        assertEquals(-1, negative.toDays())
        assertEquals(-25, negative.toHours())
        assertEquals(-1_501, negative.toMinutes())
        assertEquals(-90_062, negative.toSeconds())
        assertEquals(-90_061_987, negative.toMillis())
        assertEquals(-90_061_987_654_321, negative.toNanos())
        assertEquals(-1, negative.toDaysPart())
        assertEquals(-1, negative.toHoursPart())
        assertEquals(-1, negative.toMinutesPart())
        assertEquals(-2, negative.toSecondsPart())
        assertEquals(12, negative.toMillisPart())
        assertEquals(12_345_679, negative.toNanosPart())
    }

    @Test
    fun exactSubsecondConversionsDetectOverflow() {
        assertEquals(Long.MAX_VALUE, Duration.ofMillis(Long.MAX_VALUE).toMillis())
        assertEquals(Long.MIN_VALUE, Duration.ofMillis(Long.MIN_VALUE).toMillis())
        assertEquals(Long.MAX_VALUE, Duration.ofNanos(Long.MAX_VALUE).toNanos())
        assertEquals(Long.MIN_VALUE, Duration.ofNanos(Long.MIN_VALUE).toNanos())
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MAX_VALUE).toMillis()
        }
        assertFailsWith<ArithmeticException> {
            Duration.ofSeconds(Long.MIN_VALUE).toNanos()
        }
    }

    @Test
    fun parsesIsoDurationsIncludingJavaSignedExtensions() {
        assertEquals(Duration.ofSeconds(20, 345_000_000), Duration.parse("PT20.345S"))
        assertEquals(Duration.ofMinutes(15), Duration.parse("PT15M"))
        assertEquals(Duration.ofHours(10), Duration.parse("pt10h"))
        assertEquals(Duration.ofDays(2), Duration.parse("P2D"))
        assertEquals(
            Duration.ofDays(2).plusHours(3).plusMinutes(4),
            Duration.parse("P2DT3H4M"),
        )
        assertEquals(Duration.ofHours(-6).plusMinutes(3), Duration.parse("PT-6H3M"))
        assertEquals(Duration.ofHours(-6).minusMinutes(3), Duration.parse("-PT6H3M"))
        assertEquals(Duration.ofHours(6).minusMinutes(3), Duration.parse("-PT-6H+3M"))
        assertEquals(Duration.ofMillis(1_250), Duration.parse("PT+1,25S"))
        assertEquals(Duration.ofSeconds(1), Duration.parse("PT1.S"))
        assertEquals(Duration.ofMillis(-500), Duration.parse("PT-0.5S"))
    }

    @Test
    fun parseRejectsMissingSectionsMalformedFractionsAndOverflow() {
        val invalidInputs = listOf(
            "",
            "P",
            "PT",
            "P1DT",
            "1S",
            "P1H",
            "PT1D",
            "PT1.1234567890S",
            "PT１S",
            "PT9223372036854775808S",
            "P106751991167301D",
            "-PT-9223372036854775808S",
        )

        invalidInputs.forEach { input ->
            val error = assertFailsWith<DateTimeParseException> { Duration.parse(input) }
            assertEquals(input, error.parsedString)
            assertEquals(0, error.errorIndex)
        }
    }

    @Test
    fun calculatesBetweenTemporalsUsingNanosWhenWholeSecondsAreZero() {
        val start = BetweenTemporal(secondsUntil = 0, nanosUntil = 250_000_000, nanoOfSecond = 100)
        val end = BetweenTemporal(nanoOfSecond = 200)

        assertEquals(Duration.ofMillis(250), Duration.between(start, end))
    }

    @Test
    fun calculatesBetweenTemporalsWithNanoOfSecondCorrectionAndFallback() {
        assertEquals(
            Duration.ofMillis(1_200),
            Duration.between(
                BetweenTemporal(secondsUntil = 1, nanoOfSecond = 900_000_000),
                BetweenTemporal(nanoOfSecond = 100_000_000),
            ),
        )
        assertEquals(
            Duration.ofMillis(-1_200),
            Duration.between(
                BetweenTemporal(secondsUntil = -1, nanoOfSecond = 100_000_000),
                BetweenTemporal(nanoOfSecond = 900_000_000),
            ),
        )
        assertEquals(
            Duration.ofSeconds(3),
            Duration.between(
                BetweenTemporal(secondsUntil = 3, supportsNanoOfSecond = false),
                BetweenTemporal(nanoOfSecond = 100_000_000),
            ),
        )
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

    private data class RecordingTemporal(
        val operations: List<Pair<TemporalUnit, Long>> = emptyList(),
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean = false

        override fun isSupported(unit: TemporalUnit?): Boolean =
            unit === ChronoUnit.SECONDS || unit === ChronoUnit.NANOS

        override fun getLong(field: TemporalField): Long =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            copy(operations = operations + (unit to amountToAdd))

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }

    private data class BetweenTemporal(
        val secondsUntil: Long = 0,
        val nanosUntil: Long = 0,
        val nanoOfSecond: Long = 0,
        val supportsNanoOfSecond: Boolean = true,
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean =
            supportsNanoOfSecond && field === ChronoField.NANO_OF_SECOND

        override fun isSupported(unit: TemporalUnit?): Boolean =
            unit === ChronoUnit.SECONDS || unit === ChronoUnit.NANOS

        override fun getLong(field: TemporalField): Long =
            if (isSupported(field)) {
                nanoOfSecond
            } else {
                throw UnsupportedTemporalTypeException("Unsupported field: $field")
            }

        override fun with(field: TemporalField, newValue: Long): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long = when (unit) {
            ChronoUnit.SECONDS -> secondsUntil
            ChronoUnit.NANOS -> nanosUntil
            else -> throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        }
    }

    private companion object {
        val ONE_SECOND: Duration = Duration.ofSeconds(1)

        val ONE_AND_A_HALF_SECONDS: TemporalUnit = exactUnit(
            name = "OneAndAHalfSeconds",
            duration = Duration.ofMillis(1_500),
        )
        val NINETY_MINUTES: TemporalUnit = exactUnit(
            name = "NinetyMinutes",
            duration = Duration.ofMinutes(90),
        )
        val SEVEN_MINUTES: TemporalUnit = exactUnit(
            name = "SevenMinutes",
            duration = Duration.ofMinutes(7),
        )

        fun exactUnit(name: String, duration: Duration): TemporalUnit = object : TemporalUnit {
            override val duration: Duration = duration
            override val isDurationEstimated: Boolean = false
            override val isDateBased: Boolean = false
            override val isTimeBased: Boolean = false

            override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
                @Suppress("UNCHECKED_CAST")
                return temporal.plus(amount, this) as R
            }

            override fun between(
                temporal1Inclusive: Temporal,
                temporal2Exclusive: Temporal,
            ): Long = temporal1Inclusive.until(temporal2Exclusive, this)

            override fun toString(): String = name
        }
    }
}
