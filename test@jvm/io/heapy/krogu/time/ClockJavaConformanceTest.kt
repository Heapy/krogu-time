package io.heapy.krogu.time

import java.time.Clock as JavaClock
import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant
import java.time.ZoneOffset as JavaZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockJavaConformanceTest {
    @Test
    fun fixedOffsetAndTickClocksMatchJavaTime() {
        val instantText = "1969-12-31T23:59:59.750000123Z"
        val ourFixed = Clock.fixed(Instant.parse(instantText), ZoneOffset.UTC)
        val javaFixed = JavaClock.fixed(JavaInstant.parse(instantText), JavaZoneOffset.UTC)
        assertClockEquals(javaFixed, ourFixed)

        val ourOffset = Clock.offset(ourFixed, Duration.ofMinutes(-90))
        val javaOffset = JavaClock.offset(javaFixed, JavaDuration.ofMinutes(-90))
        assertClockEquals(javaOffset, ourOffset)

        listOf(1L, 20, 250, 500_000, 1_000_000, 500_000_000, 60_000_000_000).forEach { nanos ->
            val ourTick = Clock.tick(ourFixed, Duration.ofNanos(nanos))
            val javaTick = JavaClock.tick(javaFixed, JavaDuration.ofNanos(nanos))
            assertEquals(javaTick.instant().toString(), ourTick.instant().toString(), nanos.toString())
            assertEquals(javaTick.toString(), ourTick.toString(), nanos.toString())
        }
    }

    private fun assertClockEquals(expected: JavaClock, actual: Clock) {
        assertEquals(expected.zone.toString(), actual.zone.toString())
        assertEquals(expected.instant().toString(), actual.instant().toString())
        assertEquals(expected.millis(), actual.millis())
        assertEquals(expected.toString(), actual.toString())
        assertEquals(expected.hashCode(), actual.hashCode())
    }
}
