package io.heapy.grogu.time.zone

import io.heapy.grogu.time.Duration
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ZoneOffsetTransitionTest {
    @Test
    fun exposesGapAndOverlapTransitionValues() {
        val transitionDateTime = LocalDateTime.of(2024, 3, 31, 2, 0)
        val before = ZoneOffset.ofHours(1)
        val after = ZoneOffset.ofHours(2)
        val gap = ZoneOffsetTransition.of(transitionDateTime, before, after)

        assertEquals(transitionDateTime, gap.dateTimeBefore)
        assertEquals(transitionDateTime.plusHours(1), gap.dateTimeAfter)
        assertEquals(before, gap.offsetBefore)
        assertEquals(after, gap.offsetAfter)
        assertEquals(transitionDateTime.toEpochSecond(before), gap.toEpochSecond())
        assertEquals(gap.toEpochSecond(), gap.instant.epochSecond)
        assertEquals(Duration.ofHours(1), gap.duration)
        assertTrue(gap.isGap)
        assertFalse(gap.isOverlap)
        assertFalse(gap.isValidOffset(before))
        assertFalse(gap.isValidOffset(after))
        assertEquals(
            "Transition[Gap at 2024-03-31T02:00+01:00 to +02:00]",
            gap.toString(),
        )

        val overlap = ZoneOffsetTransition.of(transitionDateTime, after, before)
        assertEquals(transitionDateTime.minusHours(1), overlap.dateTimeAfter)
        assertEquals(Duration.ofHours(-1), overlap.duration)
        assertFalse(overlap.isGap)
        assertTrue(overlap.isOverlap)
        assertTrue(overlap.isValidOffset(before))
        assertTrue(overlap.isValidOffset(after))
        assertTrue(overlap < gap)
    }

    @Test
    fun validatesFactoryInputsAndUsesStructuralEquality() {
        val transitionDateTime = LocalDateTime.of(2024, 3, 31, 2, 0)
        val before = ZoneOffset.ofHours(1)
        val after = ZoneOffset.ofHours(2)
        val transition = ZoneOffsetTransition.of(transitionDateTime, before, after)
        assertEquals(ZoneOffsetTransition.of(transitionDateTime, before, after), transition)
        assertEquals(
            ZoneOffsetTransition.of(transitionDateTime, before, after).hashCode(),
            transition.hashCode(),
        )
        assertFailsWith<IllegalArgumentException> {
            ZoneOffsetTransition.of(transitionDateTime, before, before)
        }
        assertFailsWith<IllegalArgumentException> {
            ZoneOffsetTransition.of(transitionDateTime.withNano(1), before, after)
        }
    }
}
