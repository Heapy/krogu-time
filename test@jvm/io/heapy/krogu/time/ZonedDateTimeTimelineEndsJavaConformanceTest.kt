package io.heapy.krogu.time

import io.heapy.krogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ZonedDateTimeTimelineEndsJavaConformanceTest {
    private val zoneIds = listOf("Asia/Tokyo", "America/Los_Angeles", "UTC", "+14:00", "-12:00")

    private val localDateTimes = listOf(
        "MAX" to Pair(LocalDateTime.MAX, java.time.LocalDateTime.MAX),
        "MIN" to Pair(LocalDateTime.MIN, java.time.LocalDateTime.MIN),
        "ordinary" to Pair(
            LocalDateTime.of(2024, 3, 30, 12, 30, 15, 123_456_789),
            java.time.LocalDateTime.of(2024, 3, 30, 12, 30, 15, 123_456_789),
        ),
    )

    // Moving the end of a difference into the start's zone can push its local
    // date-time past the ends of the timeline. Java then moves the start into
    // the end's zone instead, so the difference stays measurable rather than
    // failing.
    @Test
    fun untilAcrossZonesAtTheTimelineEndsMatchesJavaTime() {
        val mismatches = pairs().flatMap { (name, values) ->
            val (kroguPair, javaPair) = values
            ChronoUnit.entries.map { unit ->
                val javaUnit = java.time.temporal.ChronoUnit.valueOf(unit.name)
                val expected = outcome { javaPair.first.until(javaPair.second, javaUnit) }
                val actual = outcome { kroguPair.first.until(kroguPair.second, unit) }
                Triple(name, unit, expected to actual)
            }.mapNotNull { (label, unit, outcomes) ->
                val (expected, actual) = outcomes
                if (expected == actual) null else "$label in $unit: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun durationBetweenAtTheTimelineEndsMatchesJavaTime() {
        val mismatches = pairs().mapNotNull { (name, values) ->
            val (kroguPair, javaPair) = values
            val expected = outcome { java.time.Duration.between(javaPair.first, javaPair.second) }
            val actual = outcome { Duration.between(kroguPair.first, kroguPair.second) }
            if (expected == actual) null else "$name: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun pairs(): List<Pair<String, Pair<Pair<ZonedDateTime, ZonedDateTime>, Pair<java.time.ZonedDateTime, java.time.ZonedDateTime>>>> =
        localDateTimes.flatMap { (localName, locals) ->
            zoneIds.flatMap { startZone ->
                zoneIds.map { endZone ->
                    val kroguEnd = ZonedDateTime.of(locals.first, ZoneId.of(endZone))
                    val kroguStart = kroguEnd.withZoneSameLocal(ZoneId.of(startZone))
                    val javaEnd = java.time.ZonedDateTime.of(locals.second, java.time.ZoneId.of(endZone))
                    val javaStart = javaEnd.withZoneSameLocal(java.time.ZoneId.of(startZone))
                    "$localName $startZone -> $endZone" to
                        Pair(kroguStart to kroguEnd, javaStart to javaEnd)
                }
            }
        }

    private fun outcome(block: () -> Any?): String = try {
        block().toString()
    } catch (exception: Throwable) {
        exception::class.simpleName ?: "unknown"
    }
}
