package io.heapy.grogu.time

import java.time.Duration as JavaDuration
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
}
