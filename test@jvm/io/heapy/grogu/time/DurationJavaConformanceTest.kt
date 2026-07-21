package io.heapy.grogu.time

import java.time.Duration as JavaDuration
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoUnit
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

    private fun assertSameOutcome(
        javaOperation: () -> String,
        kotlinOperation: () -> String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)

        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
    }
}
