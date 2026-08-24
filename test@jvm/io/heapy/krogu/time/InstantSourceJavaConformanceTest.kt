package io.heapy.krogu.time

import java.time.Duration as JavaDuration
import java.time.Instant as JavaInstant
import java.time.InstantSource as JavaInstantSource
import java.time.ZoneOffset as JavaZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class InstantSourceJavaConformanceTest {
    @Test
    fun factoriesAndZoneBridgeMatchJavaTime() {
        val instantText = "1969-12-31T23:59:59.750000123Z"
        val ours = InstantSource.fixed(Instant.parse(instantText))
        val java = JavaInstantSource.fixed(JavaInstant.parse(instantText))
        assertSourceEquals(java, ours)

        val ourOffset = InstantSource.offset(ours, Duration.ofMinutes(90))
        val javaOffset = JavaInstantSource.offset(java, JavaDuration.ofMinutes(90))
        assertSourceEquals(javaOffset, ourOffset)

        val ourTick = InstantSource.tick(ourOffset, Duration.ofMillis(500))
        val javaTick = JavaInstantSource.tick(javaOffset, JavaDuration.ofMillis(500))
        assertSourceEquals(javaTick, ourTick)

        val ourCustom = TestSource(Instant.parse(instantText))
        val javaCustom = object : JavaInstantSource {
            override fun instant(): JavaInstant = JavaInstant.parse(instantText)

            override fun toString(): String = "TestSource"
        }
        val ourClock = ourCustom.withZone(ZoneOffset.ofHours(2))
        val javaClock = javaCustom.withZone(JavaZoneOffset.ofHours(2))
        assertEquals(javaClock.instant().toString(), ourClock.instant().toString())
        assertEquals(javaClock.millis(), ourClock.millis())
        assertEquals(javaClock.toString(), ourClock.toString())
    }

    private fun assertSourceEquals(expected: JavaInstantSource, actual: InstantSource) {
        assertEquals(expected.instant().toString(), actual.instant().toString())
        assertEquals(expected.millis(), actual.millis())
        assertEquals(expected.toString(), actual.toString())
        assertEquals(expected.hashCode(), actual.hashCode())
    }

    private class TestSource(
        private val value: Instant,
    ) : InstantSource {
        override fun instant(): Instant = value

        override fun toString(): String = "TestSource"
    }
}
