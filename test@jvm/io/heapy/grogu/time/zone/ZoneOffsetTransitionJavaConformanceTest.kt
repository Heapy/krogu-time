package io.heapy.grogu.time.zone

import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneOffset
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneOffset as JavaZoneOffset
import java.time.zone.ZoneOffsetTransition as JavaZoneOffsetTransition
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneOffsetTransitionJavaConformanceTest {
    @Test
    fun factoriesValuesOrderingAndTextMatchJavaTime() {
        val dateTimes = listOf(
            LocalDateTime.of(1900, 1, 1, 0, 0),
            LocalDateTime.of(2024, 3, 31, 2, 0),
            LocalDateTime.of(2099, 12, 31, 23, 59, 59),
        )
        val offsetPairs = listOf(
            ZoneOffset.UTC to ZoneOffset.ofHours(1),
            ZoneOffset.ofHours(2) to ZoneOffset.ofHours(1),
            ZoneOffset.MIN to ZoneOffset.MAX,
        )
        val transitions = dateTimes.flatMap { dateTime ->
            offsetPairs.map { (before, after) ->
                ZoneOffsetTransition.of(dateTime, before, after)
            }
        }

        transitions.forEach { transition ->
            val javaTransition = transition.toJava()
            assertEquals(javaTransition.instant.toString(), transition.instant.toString())
            assertEquals(javaTransition.toEpochSecond(), transition.toEpochSecond())
            assertEquals(javaTransition.dateTimeBefore.toString(), transition.dateTimeBefore.toString())
            assertEquals(javaTransition.dateTimeAfter.toString(), transition.dateTimeAfter.toString())
            assertEquals(javaTransition.duration.toString(), transition.duration.toString())
            assertEquals(javaTransition.isGap, transition.isGap)
            assertEquals(javaTransition.isOverlap, transition.isOverlap)
            assertEquals(javaTransition.hashCode(), transition.hashCode())
            assertEquals(javaTransition.toString(), transition.toString())
            transitions.forEach { other ->
                assertEquals(javaTransition.compareTo(other.toJava()), transition.compareTo(other))
            }
        }
    }

    @Test
    fun factoryValidationMatchesJavaTime() {
        val cases = listOf(
            Triple(LocalDateTime.of(2024, 1, 1, 0, 0), ZoneOffset.UTC, ZoneOffset.UTC),
            Triple(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0, 1),
                ZoneOffset.UTC,
                ZoneOffset.ofHours(1),
            ),
        )
        cases.forEach { (dateTime, before, after) ->
            assertSameOutcome(
                javaOperation = {
                    JavaZoneOffsetTransition.of(
                        dateTime.toJava(),
                        before.toJava(),
                        after.toJava(),
                    )
                },
                kotlinOperation = { ZoneOffsetTransition.of(dateTime, before, after) },
                context = "$dateTime $before $after",
            )
        }
    }

    private fun ZoneOffsetTransition.toJava(): JavaZoneOffsetTransition =
        JavaZoneOffsetTransition.of(dateTimeBefore.toJava(), offsetBefore.toJava(), offsetAfter.toJava())

    private fun LocalDateTime.toJava(): JavaLocalDateTime = JavaLocalDateTime.of(
        year,
        monthValue,
        dayOfMonth,
        hour,
        minute,
        second,
        nano,
    )

    private fun ZoneOffset.toJava(): JavaZoneOffset = JavaZoneOffset.ofTotalSeconds(totalSeconds)

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(javaResult.exceptionOrNull()?.javaClass?.simpleName, kotlinResult.exceptionOrNull()?.javaClass?.simpleName, context)
    }
}
