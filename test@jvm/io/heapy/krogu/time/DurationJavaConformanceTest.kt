package io.heapy.krogu.time

import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import java.time.temporal.Temporal as JavaTemporal
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationJavaConformanceTest {
    @Test
    fun normalizedRepresentationAndFormattingMatchJavaTime() {
        val cases = listOf(
            Duration.ofSeconds(Long.MIN_VALUE) to JavaDuration.ofSeconds(Long.MIN_VALUE),
            Duration.ofSeconds(-3_661, 1) to JavaDuration.ofSeconds(-3_661, 1),
            Duration.ofMillis(-1_500) to JavaDuration.ofMillis(-1_500),
            Duration.ofNanos(-1) to JavaDuration.ofNanos(-1),
            Duration.ZERO to JavaDuration.ZERO,
            Duration.ofNanos(1) to JavaDuration.ofNanos(1),
            Duration.ofMillis(1_500) to JavaDuration.ofMillis(1_500),
            Duration.ofSeconds(3_661, 123_400_000) to JavaDuration.ofSeconds(3_661, 123_400_000),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999) to
                JavaDuration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )

        cases.forEach { (duration, javaDuration) ->
            assertEquals(javaDuration.seconds, duration.seconds)
            assertEquals(javaDuration.nano, duration.nano)
            assertEquals(javaDuration.isZero, duration.isZero)
            assertEquals(javaDuration.isPositive, duration.isPositive)
            assertEquals(javaDuration.isNegative, duration.isNegative)
            assertEquals(javaDuration.toString(), duration.toString())
        }
    }

    @Test
    fun additionAndSubtractionMatchJavaTime() {
        val durations = listOf(
            Duration.ZERO,
            Duration.ofNanos(-1),
            Duration.ofSeconds(-10, 500_000_000),
            Duration.ofSeconds(10, 500_000_000),
            Duration.ofSeconds(Long.MAX_VALUE - 1, 999_999_999),
        )
        val amounts = listOf(Long.MIN_VALUE, -10_000L, -1L, 0L, 1L, 10_000L, Long.MAX_VALUE)

        durations.forEach { duration ->
            val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())
            amounts.forEach { amount ->
                assertSameOutcome(
                    javaOperation = { javaDuration.plusNanos(amount).toString() },
                    kotlinOperation = { duration.plusNanos(amount).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaDuration.minusNanos(amount).toString() },
                    kotlinOperation = { duration.minusNanos(amount).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaDuration.plusSeconds(amount).toString() },
                    kotlinOperation = { duration.plusSeconds(amount).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaDuration.minusSeconds(amount).toString() },
                    kotlinOperation = { duration.minusSeconds(amount).toString() },
                )
            }
        }
    }

    @Test
    fun standardTemporalUnitBehaviorMatchesJavaTime() {
        val units = listOf(
            ChronoUnit.NANOS,
            ChronoUnit.MICROS,
            ChronoUnit.MILLIS,
            ChronoUnit.SECONDS,
            ChronoUnit.MINUTES,
            ChronoUnit.HOURS,
            ChronoUnit.HALF_DAYS,
            ChronoUnit.DAYS,
            ChronoUnit.WEEKS,
            ChronoUnit.MONTHS,
        )
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)
        val duration = Duration.ofSeconds(-2, 500_000_000)
        val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())

        units.forEach { unit ->
            val javaUnit = JavaChronoUnit.valueOf(unit.name)
            amounts.forEach { amount ->
                assertSameOutcome(
                    javaOperation = { JavaDuration.of(amount, javaUnit).toString() },
                    kotlinOperation = { Duration.of(amount, unit).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaDuration.plus(amount, javaUnit).toString() },
                    kotlinOperation = { duration.plus(amount, unit).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaDuration.minus(amount, javaUnit).toString() },
                    kotlinOperation = { duration.minus(amount, unit).toString() },
                )
            }
        }
    }

    @Test
    fun multiplicationMatchesJavaTime() {
        val durations = listOf(
            Duration.ZERO,
            Duration.ofNanos(-1),
            Duration.ofMillis(-1_500),
            Duration.ofSeconds(-2, 1),
            Duration.ofMillis(1_500),
            Duration.ofSeconds(1, 999_999_999),
            Duration.ofSeconds(Long.MIN_VALUE),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )
        val multiplicands = listOf(
            Long.MIN_VALUE,
            -1_000_000_001L,
            -1_000_000_000L,
            -999_999_999L,
            -2L,
            -1L,
            0L,
            1L,
            2L,
            999_999_999L,
            1_000_000_000L,
            1_000_000_001L,
            Long.MAX_VALUE,
        )

        durations.forEach { duration ->
            val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())
            multiplicands.forEach { multiplicand ->
                assertSameOutcome(
                    javaOperation = { javaDuration.multipliedBy(multiplicand).toString() },
                    kotlinOperation = { duration.multipliedBy(multiplicand).toString() },
                )
            }
        }
    }

    @Test
    fun conversionsAndComponentPartsMatchJavaTime() {
        val durations = listOf(
            Duration.ofSeconds(Long.MIN_VALUE),
            Duration.ofNanos(Long.MIN_VALUE),
            Duration.ofSeconds(-90_062, 12_345_679),
            Duration.ofNanos(-1),
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofSeconds(90_061, 987_654_321),
            Duration.ofNanos(Long.MAX_VALUE),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )

        durations.forEach { duration ->
            val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())

            assertSameOutcome(javaDuration::toDays, duration::toDays)
            assertSameOutcome(javaDuration::toHours, duration::toHours)
            assertSameOutcome(javaDuration::toMinutes, duration::toMinutes)
            assertSameOutcome(javaDuration::toSeconds, duration::toSeconds)
            assertSameOutcome(javaDuration::toMillis, duration::toMillis)
            assertSameOutcome(javaDuration::toNanos, duration::toNanos)
            assertSameOutcome(javaDuration::toDaysPart, duration::toDaysPart)
            assertSameOutcome(javaDuration::toHoursPart, duration::toHoursPart)
            assertSameOutcome(javaDuration::toMinutesPart, duration::toMinutesPart)
            assertSameOutcome(javaDuration::toSecondsPart, duration::toSecondsPart)
            assertSameOutcome(javaDuration::toMillisPart, duration::toMillisPart)
            assertSameOutcome(javaDuration::toNanosPart, duration::toNanosPart)
        }
    }

    @Test
    fun parsingMatchesJavaTime() {
        val inputs = listOf(
            "PT0S",
            "pt20.345s",
            "PT15M",
            "PT10H",
            "P2D",
            "P2DT3H4M",
            "PT-6H3M",
            "-PT6H3M",
            "-PT-6H+3M",
            "PT+1,25S",
            "PT1.S",
            "PT-0.5S",
            "P+2DT-3H+4M-5.000000006S",
            "PT9223372036854775807.999999999S",
            "PT-9223372036854775808S",
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

        inputs.forEach { input ->
            assertSameOutcome(
                javaOperation = { JavaDuration.parse(input).toString() },
                kotlinOperation = { Duration.parse(input).toString() },
            )
        }
    }

    @Test
    fun divisionMatchesJavaTime() {
        val durations = listOf(
            Duration.ofSeconds(Long.MIN_VALUE),
            Duration.ofSeconds(-2, 1),
            Duration.ofMillis(-1_500),
            Duration.ofNanos(-1),
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofMillis(1_500),
            Duration.ofSeconds(1, 999_999_999),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )
        val scalarDivisors = listOf(
            Long.MIN_VALUE,
            -1_000_000_001L,
            -2L,
            -1L,
            0L,
            1L,
            2L,
            1_000_000_001L,
            Long.MAX_VALUE,
        )
        val durationDivisors = listOf(
            Duration.ofSeconds(Long.MIN_VALUE),
            Duration.ofSeconds(-1),
            Duration.ofNanos(-1),
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofMillis(333),
            Duration.ofSeconds(1),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )

        durations.forEach { duration ->
            val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())
            scalarDivisors.forEach { divisor ->
                assertSameOutcome(
                    javaOperation = { javaDuration.dividedBy(divisor).toString() },
                    kotlinOperation = { duration.dividedBy(divisor).toString() },
                )
            }
            durationDivisors.forEach { divisor ->
                val javaDivisor = JavaDuration.ofSeconds(divisor.seconds, divisor.nano.toLong())
                assertSameOutcome(
                    javaOperation = { javaDuration.dividedBy(javaDivisor) },
                    kotlinOperation = { duration.dividedBy(divisor) },
                )
            }
        }
    }

    @Test
    fun truncationMatchesJavaTime() {
        val durations = listOf(
            Duration.ofSeconds(Long.MIN_VALUE, 999_999_999),
            Duration.ofSeconds(-90_062, 12_345_679),
            Duration.ofMillis(-61_750),
            Duration.ofNanos(-1),
            Duration.ZERO,
            Duration.ofNanos(1),
            Duration.ofSeconds(90_061, 987_654_321),
            Duration.ofSeconds(Long.MAX_VALUE, 999_999_999),
        )
        val units = listOf(
            ChronoUnit.NANOS,
            ChronoUnit.MICROS,
            ChronoUnit.MILLIS,
            ChronoUnit.SECONDS,
            ChronoUnit.MINUTES,
            ChronoUnit.HOURS,
            ChronoUnit.HALF_DAYS,
            ChronoUnit.DAYS,
            ChronoUnit.WEEKS,
            ChronoUnit.MONTHS,
        )

        durations.forEach { duration ->
            val javaDuration = JavaDuration.ofSeconds(duration.seconds, duration.nano.toLong())
            units.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertSameOutcome(
                    javaOperation = { javaDuration.truncatedTo(javaUnit).toString() },
                    kotlinOperation = { duration.truncatedTo(unit).toString() },
                )
            }
        }
    }

    @Test
    fun betweenMatchesJavaTime() {
        val pairs = listOf(
            JavaInstant.ofEpochSecond(0, 100_000_000) to
                JavaInstant.ofEpochSecond(0, 350_000_000),
            JavaInstant.ofEpochSecond(0, 900_000_000) to
                JavaInstant.ofEpochSecond(2, 100_000_000),
            JavaInstant.ofEpochSecond(2, 100_000_000) to
                JavaInstant.ofEpochSecond(0, 900_000_000),
            JavaInstant.ofEpochSecond(-10, 999_999_999) to
                JavaInstant.ofEpochSecond(10, 1),
        )

        pairs.forEach { (start, end) ->
            assertEquals(
                JavaDuration.between(start, end).toString(),
                Duration.between(JavaTemporalAdapter(start), JavaTemporalAdapter(end)).toString(),
            )
        }
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
    }

    private data class JavaTemporalAdapter(
        val delegate: JavaTemporal,
    ) : Temporal {
        override fun isSupported(field: TemporalField?): Boolean =
            field is ChronoField && delegate.isSupported(JavaChronoField.valueOf(field.name))

        override fun isSupported(unit: TemporalUnit?): Boolean =
            unit is ChronoUnit && delegate.isSupported(JavaChronoUnit.valueOf(unit.name))

        override fun getLong(field: TemporalField): Long =
            if (field is ChronoField) {
                delegate.getLong(JavaChronoField.valueOf(field.name))
            } else {
                throw UnsupportedTemporalTypeException("Unsupported field: $field")
            }

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (field is ChronoField) {
                JavaTemporalAdapter(delegate.with(JavaChronoField.valueOf(field.name), newValue))
            } else {
                throw UnsupportedTemporalTypeException("Unsupported field: $field")
            }

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            if (unit is ChronoUnit) {
                JavaTemporalAdapter(delegate.plus(amountToAdd, JavaChronoUnit.valueOf(unit.name)))
            } else {
                throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
            }

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
            if (endExclusive !is JavaTemporalAdapter || unit !is ChronoUnit) {
                throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
            }
            return delegate.until(endExclusive.delegate, JavaChronoUnit.valueOf(unit.name))
        }
    }
}
