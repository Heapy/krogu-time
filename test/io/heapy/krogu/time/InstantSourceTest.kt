package io.heapy.krogu.time

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InstantSourceTest {
    @Test
    fun createsSystemFixedOffsetAndTickSources() {
        val system = InstantSource.system()
        assertSame(system, InstantSource.system())
        assertEquals("SystemInstantSource", system.toString())
        assertTrue(abs(system.instant().toEpochMilli() - system.millis()) < 1_000)

        val instant = Instant.parse("2024-06-01T10:15:30.123456789Z")
        val fixed = InstantSource.fixed(instant)
        assertTrue(fixed is Clock)
        assertSame(instant, fixed.instant())
        assertEquals("FixedClock[2024-06-01T10:15:30.123456789Z,Z]", fixed.toString())
        assertSame(fixed, InstantSource.offset(fixed, Duration.ZERO))
        assertSame(fixed, InstantSource.tick(fixed, Duration.ofNanos(1)))

        val offset = InstantSource.offset(fixed, Duration.ofSeconds(30))
        assertEquals(instant.plusSeconds(30), offset.instant())
        val tick = InstantSource.tick(offset, Duration.ofMillis(500))
        assertEquals(Instant.parse("2024-06-01T10:16:00Z"), tick.instant())
    }

    @Test
    fun bridgesAnySourceToAZoneAwareClock() {
        val source = TestSource(Instant.parse("2024-06-01T10:15:30.123456789Z"))
        assertEquals(source.instant().toEpochMilli(), source.millis())

        val clock = source.withZone(ZoneOffset.ofHours(2))
        assertEquals(ZoneOffset.ofHours(2), clock.zone)
        assertSame(source.instant(), clock.instant())
        assertEquals("SourceClock[TestSource,+02:00]", clock.toString())
        assertSame(clock, clock.withZone(ZoneOffset.ofHours(2)))
        assertEquals(source.withZone(ZoneOffset.UTC), clock.withZone(ZoneOffset.UTC))
    }

    private class TestSource(
        private val value: Instant,
    ) : InstantSource {
        override fun instant(): Instant = value

        override fun toString(): String = "TestSource"
    }
}
