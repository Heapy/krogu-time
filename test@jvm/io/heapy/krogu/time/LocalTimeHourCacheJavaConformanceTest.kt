package io.heapy.krogu.time

import java.time.Instant as JavaInstant
import java.time.LocalTime as JavaLocalTime
import java.time.ZoneOffset as JavaZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeHourCacheJavaConformanceTest {
    private class Case(
        val name: String,
        val java: () -> JavaLocalTime,
        val krogu: () -> LocalTime,
    )

    private val nanosPerSecond = 1_000_000_000L
    private val secondsPerHour = 3_600L

    // Every hour is exercised, because java.time caches all 24 of them and a
    // cache that covered only midnight and noon would otherwise pass.
    private val cases: List<Case> = buildList {
        (0 until 24).forEach { hour ->
            val secondOfDay = hour * secondsPerHour
            val nanoOfDay = secondOfDay * nanosPerSecond
            add(
                Case(
                    "of($hour, 0)",
                    { JavaLocalTime.of(hour, 0) },
                    { LocalTime.of(hour, 0) },
                ),
            )
            add(
                Case(
                    "of($hour, 0, 0)",
                    { JavaLocalTime.of(hour, 0, 0) },
                    { LocalTime.of(hour, 0, 0) },
                ),
            )
            add(
                Case(
                    "of($hour, 0, 0, 0)",
                    { JavaLocalTime.of(hour, 0, 0, 0) },
                    { LocalTime.of(hour, 0, 0, 0) },
                ),
            )
            add(
                Case(
                    "ofSecondOfDay($secondOfDay)",
                    { JavaLocalTime.ofSecondOfDay(secondOfDay) },
                    { LocalTime.ofSecondOfDay(secondOfDay) },
                ),
            )
            add(
                Case(
                    "ofNanoOfDay($nanoOfDay)",
                    { JavaLocalTime.ofNanoOfDay(nanoOfDay) },
                    { LocalTime.ofNanoOfDay(nanoOfDay) },
                ),
            )
            add(
                Case(
                    "ofInstant($secondOfDay, UTC)",
                    {
                        JavaLocalTime.ofInstant(
                            JavaInstant.ofEpochSecond(secondOfDay),
                            JavaZoneOffset.UTC,
                        )
                    },
                    {
                        LocalTime.ofInstant(
                            Instant.ofEpochSecond(secondOfDay),
                            ZoneOffset.UTC,
                        )
                    },
                ),
            )
        }

        // A cache that reached beyond whole hours would be caught here, since
        // java.time allocates afresh for every one of these.
        add(Case("of(5, 1)", { JavaLocalTime.of(5, 1) }, { LocalTime.of(5, 1) }))
        add(Case("of(5, 0, 1)", { JavaLocalTime.of(5, 0, 1) }, { LocalTime.of(5, 0, 1) }))
        add(
            Case(
                "of(5, 0, 0, 1)",
                { JavaLocalTime.of(5, 0, 0, 1) },
                { LocalTime.of(5, 0, 0, 1) },
            ),
        )
        add(
            Case(
                "ofSecondOfDay(5h + 1s)",
                { JavaLocalTime.ofSecondOfDay(5 * secondsPerHour + 1) },
                { LocalTime.ofSecondOfDay(5 * secondsPerHour + 1) },
            ),
        )
        add(
            Case(
                "ofNanoOfDay(5h + 1ns)",
                { JavaLocalTime.ofNanoOfDay(5 * secondsPerHour * nanosPerSecond + 1) },
                { LocalTime.ofNanoOfDay(5 * secondsPerHour * nanosPerSecond + 1) },
            ),
        )
        add(
            Case(
                "ofInstant(5h + 1ns, UTC)",
                {
                    JavaLocalTime.ofInstant(
                        JavaInstant.ofEpochSecond(5 * secondsPerHour, 1),
                        JavaZoneOffset.UTC,
                    )
                },
                {
                    LocalTime.ofInstant(
                        Instant.ofEpochSecond(5 * secondsPerHour, 1),
                        ZoneOffset.UTC,
                    )
                },
            ),
        )
    }

    // java.time.LocalTime is a value-based class, and Kotlin rejects === on one,
    // so the identity question this whole file asks is put through Any instead.
    private fun isSame(first: Any?, second: Any?): Boolean = first === second

    @Test
    fun factoriesShareWholeHourInstancesLikeJavaTime() {
        val mismatches = cases.mapNotNull { case ->
            val expected = isSame(case.java(), case.java())
            val actual = isSame(case.krogu(), case.krogu())
            if (expected == actual) null else "${case.name}: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // Sharing instances must not change which time a factory returns, so a cache
    // handing back the neighbouring hour is caught rather than rewarded.
    @Test
    fun cachedFactoriesReturnTheSameTimesAsJavaTime() {
        val mismatches = cases.mapNotNull { case ->
            val expected = case.java().toString()
            val actual = case.krogu().toString()
            if (expected == actual) null else "${case.name}: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun constantsAreTheCachedInstancesLikeJavaTime() {
        val maxNanoOfDay = 24 * secondsPerHour * nanosPerSecond - 1
        val mismatches = listOf(
            Triple(
                "MIDNIGHT === of(0, 0)",
                isSame(JavaLocalTime.MIDNIGHT, JavaLocalTime.of(0, 0)),
                isSame(LocalTime.MIDNIGHT, LocalTime.of(0, 0)),
            ),
            Triple(
                "MIN === MIDNIGHT",
                isSame(JavaLocalTime.MIN, JavaLocalTime.MIDNIGHT),
                isSame(LocalTime.MIN, LocalTime.MIDNIGHT),
            ),
            Triple(
                "MIN === ofSecondOfDay(0)",
                isSame(JavaLocalTime.MIN, JavaLocalTime.ofSecondOfDay(0)),
                isSame(LocalTime.MIN, LocalTime.ofSecondOfDay(0)),
            ),
            Triple(
                "NOON === of(12, 0)",
                isSame(JavaLocalTime.NOON, JavaLocalTime.of(12, 0)),
                isSame(LocalTime.NOON, LocalTime.of(12, 0)),
            ),
            Triple(
                "NOON === ofNanoOfDay(12h)",
                isSame(
                    JavaLocalTime.NOON,
                    JavaLocalTime.ofNanoOfDay(12 * secondsPerHour * nanosPerSecond),
                ),
                isSame(
                    LocalTime.NOON,
                    LocalTime.ofNanoOfDay(12 * secondsPerHour * nanosPerSecond),
                ),
            ),
            Triple(
                "MAX === ofNanoOfDay(maxNanoOfDay)",
                isSame(JavaLocalTime.MAX, JavaLocalTime.ofNanoOfDay(maxNanoOfDay)),
                isSame(LocalTime.MAX, LocalTime.ofNanoOfDay(maxNanoOfDay)),
            ),
        ).mapNotNull { (name, expected, actual) ->
            if (expected == actual) null else "$name: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // The cached array and the constants live in one companion, and a constant
    // declared ahead of the array would read null, so every constant is asked for
    // its own value rather than being trusted because a factory call agreed.
    @Test
    fun constantsSurviveClassInitialisation() {
        val mismatches = listOf(
            Triple(
                "MIDNIGHT",
                JavaLocalTime.MIDNIGHT.toString(),
                LocalTime.MIDNIGHT.toString(),
            ),
            Triple("MIN", JavaLocalTime.MIN.toString(), LocalTime.MIN.toString()),
            Triple("MAX", JavaLocalTime.MAX.toString(), LocalTime.MAX.toString()),
            Triple("NOON", JavaLocalTime.NOON.toString(), LocalTime.NOON.toString()),
        ).mapNotNull { (name, expected, actual) ->
            if (expected == actual) null else "$name: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }
}
