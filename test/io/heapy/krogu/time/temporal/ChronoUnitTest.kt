package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChronoUnitTest {
    @Test
    fun timeUnitsAreExactAndDivideTheStandardDay() {
        val expected = mapOf(
            ChronoUnit.NANOS to Duration.ofNanos(1),
            ChronoUnit.MICROS to Duration.ofNanos(1_000),
            ChronoUnit.MILLIS to Duration.ofMillis(1),
            ChronoUnit.SECONDS to Duration.ofSeconds(1),
            ChronoUnit.MINUTES to Duration.ofMinutes(1),
            ChronoUnit.HOURS to Duration.ofHours(1),
            ChronoUnit.HALF_DAYS to Duration.ofHours(12),
        )

        expected.forEach { (unit, duration) ->
            assertEquals(duration, unit.duration)
            assertTrue(unit.isTimeBased)
            assertFalse(unit.isDateBased)
            assertFalse(unit.isDurationEstimated)
        }
    }

    @Test
    fun calendarUnitsAreEstimated() {
        listOf(
            ChronoUnit.DAYS,
            ChronoUnit.WEEKS,
            ChronoUnit.MONTHS,
            ChronoUnit.YEARS,
            ChronoUnit.DECADES,
            ChronoUnit.CENTURIES,
            ChronoUnit.MILLENNIA,
            ChronoUnit.ERAS,
        ).forEach { unit ->
            assertTrue(unit.isDateBased)
            assertFalse(unit.isTimeBased)
            assertTrue(unit.isDurationEstimated)
        }
    }

    @Test
    fun foreverIsEstimatedButNeitherDateNorTimeBased() {
        assertTrue(ChronoUnit.FOREVER.isDurationEstimated)
        assertFalse(ChronoUnit.FOREVER.isDateBased)
        assertFalse(ChronoUnit.FOREVER.isTimeBased)
        assertEquals(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), ChronoUnit.FOREVER.duration)
    }

    @Test
    fun namesMatchJavaTimeStyle() {
        assertEquals("Nanos", ChronoUnit.NANOS.toString())
        assertEquals("HalfDays", ChronoUnit.HALF_DAYS.toString())
        assertEquals("Forever", ChronoUnit.FOREVER.toString())
    }
}
