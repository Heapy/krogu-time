package io.heapy.grogu.time

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ClockTest {
    @Test
    fun createsFixedAndOffsetClocksWithValueSemantics() {
        val instant = Instant.parse("2024-06-01T10:15:30.123456789Z")
        val fixed = Clock.fixed(instant, ZoneOffset.ofHours(2))
        assertSame(instant, fixed.instant())
        assertEquals(instant.toEpochMilli(), fixed.millis())
        assertSame(fixed, fixed.withZone(ZoneOffset.ofHours(2)))

        val utc = fixed.withZone(ZoneOffset.UTC)
        assertNotSame(fixed, utc)
        assertEquals(Clock.fixed(instant, ZoneOffset.UTC), utc)
        assertEquals("FixedClock[2024-06-01T10:15:30.123456789Z,Z]", utc.toString())

        assertSame(utc, Clock.offset(utc, Duration.ZERO))
        val offset = Clock.offset(utc, Duration.ofMinutes(90))
        assertEquals(instant.plusSeconds(5_400), offset.instant())
        assertEquals(instant.toEpochMilli() + 5_400_000, offset.millis())
        assertEquals(offset, Clock.offset(Clock.fixed(instant, ZoneOffset.UTC), Duration.ofMinutes(90)))
        assertEquals(
            "OffsetClock[FixedClock[2024-06-01T10:15:30.123456789Z,Z],PT1H30M]",
            offset.toString(),
        )
    }

    @Test
    fun truncatesTickClocksOnTheEpochTimeline() {
        val negativeBase = Clock.fixed(
            Instant.parse("1969-12-31T23:59:59.750000123Z"),
            ZoneOffset.UTC,
        )
        val halfSecond = Clock.tick(negativeBase, Duration.ofMillis(500))
        assertEquals(Instant.parse("1969-12-31T23:59:59.500Z"), halfSecond.instant())
        assertEquals(-500, halfSecond.millis())
        assertEquals(
            "TickClock[FixedClock[1969-12-31T23:59:59.750000123Z,Z],PT0.5S]",
            halfSecond.toString(),
        )

        val preciseBase = Clock.fixed(
            Instant.parse("2024-06-01T10:15:30.123456789Z"),
            ZoneOffset.UTC,
        )
        assertEquals(
            Instant.parse("2024-06-01T10:15:30.123456750Z"),
            Clock.tick(preciseBase, Duration.ofNanos(250)).instant(),
        )
        assertSame(preciseBase, Clock.tick(preciseBase, Duration.ZERO))
        assertSame(preciseBase, Clock.tick(preciseBase, Duration.ofNanos(1)))
        assertFailsWith<IllegalArgumentException> {
            Clock.tick(preciseBase, Duration.ofNanos(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            Clock.tick(preciseBase, Duration.ofNanos(333))
        }
    }

    @Test
    fun exposesSystemClocksInExplicitZones() {
        val utc = Clock.systemUTC()
        assertSame(utc, Clock.systemUTC())
        assertSame(utc, Clock.system(ZoneOffset.UTC))
        assertEquals(ZoneOffset.UTC, utc.zone)
        assertEquals("SystemClock[Z]", utc.toString())
        assertTrue(abs(utc.instant().toEpochMilli() - utc.millis()) < 1_000)

        val plusTwo = utc.withZone(ZoneOffset.ofHours(2))
        assertEquals(ZoneOffset.ofHours(2), plusTwo.zone)
        assertEquals(Clock.system(ZoneOffset.ofHours(2)), plusTwo)
        assertSame(plusTwo, plusTwo.withZone(ZoneOffset.ofHours(2)))
    }
}
