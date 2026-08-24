package io.heapy.krogu.time.zone

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Month
import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZoneRulesTest {
    @Test
    fun resolvesHistoricAndRecurringTransitionsByInstant() {
        val rules = europeanRules()
        assertFalse(rules.isFixedOffset)
        assertEquals(STANDARD, rules.getOffset(Instant.parse("2023-03-26T00:59:59Z")))
        assertEquals(SUMMER, rules.getOffset(Instant.parse("2023-03-26T01:00:00Z")))
        assertEquals(SUMMER, rules.getOffset(Instant.parse("2023-10-29T00:59:59Z")))
        assertEquals(STANDARD, rules.getOffset(Instant.parse("2023-10-29T01:00:00Z")))
        assertEquals(SUMMER, rules.getOffset(Instant.parse("2024-06-01T00:00:00Z")))
        assertEquals(STANDARD, rules.getOffset(Instant.parse("2024-12-01T00:00:00Z")))

        val summerInstant = Instant.parse("2024-06-01T00:00:00Z")
        assertEquals(STANDARD, rules.getStandardOffset(summerInstant))
        assertEquals(Duration.ofHours(1), rules.getDaylightSavings(summerInstant))
        assertTrue(rules.isDaylightSavings(summerInstant))
        assertFalse(rules.isDaylightSavings(Instant.parse("2024-12-01T00:00:00Z")))
    }

    @Test
    fun resolvesLocalGapsOverlapsAndNormalTimes() {
        val rules = europeanRules()
        val gap = LocalDateTime.of(2023, 3, 26, 2, 30)
        assertEquals(STANDARD, rules.getOffset(gap))
        assertEquals(emptyList(), rules.getValidOffsets(gap))
        assertTrue(rules.getTransition(gap)?.isGap == true)
        assertFalse(rules.isValidOffset(gap, STANDARD))

        val overlap = LocalDateTime.of(2023, 10, 29, 2, 30)
        assertEquals(SUMMER, rules.getOffset(overlap))
        assertEquals(listOf(SUMMER, STANDARD), rules.getValidOffsets(overlap))
        assertTrue(rules.getTransition(overlap)?.isOverlap == true)
        assertTrue(rules.isValidOffset(overlap, STANDARD))
        assertTrue(rules.isValidOffset(overlap, SUMMER))

        val futureGap = LocalDateTime.of(2024, 3, 31, 2, 30)
        assertEquals(emptyList(), rules.getValidOffsets(futureGap))
        assertTrue(rules.getTransition(futureGap)?.isGap == true)
        assertEquals(listOf(SUMMER), rules.getValidOffsets(LocalDateTime.of(2024, 6, 1, 12, 0)))
    }

    @Test
    fun navigatesAndExposesHistoricAndRecurringTransitions() {
        val rules = europeanRules()
        val first = historicTransitions().first()
        val second = historicTransitions().last()
        assertEquals(first, rules.nextTransition(Instant.EPOCH))
        assertEquals(second, rules.nextTransition(first.instant))
        assertNull(rules.previousTransition(first.instant))
        assertEquals(first, rules.previousTransition(first.instant.plusNanos(1)))
        assertEquals(
            "Transition[Gap at 2024-03-31T02:00+01:00 to +02:00]",
            rules.previousTransition(Instant.parse("2024-06-01T00:00:00Z")).toString(),
        )
        assertEquals(historicTransitions(), rules.getTransitions())
        assertEquals(lastRules(), rules.getTransitionRules())
        assertEquals(europeanRules(), rules)
        assertEquals("ZoneRules[currentStandardOffset=+01:00]", rules.toString())
    }

    @Test
    fun tracksHistoricStandardOffsetChangesAndValidatesRuleCount() {
        val change = ZoneOffsetTransition.of(
            LocalDateTime.of(1990, 1, 1, 0, 0),
            ZoneOffset.UTC,
            STANDARD,
        )
        val rules = ZoneRules.of(
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            listOf(change),
            listOf(change),
            emptyList(),
        )
        assertEquals(ZoneOffset.UTC, rules.getStandardOffset(Instant.parse("1989-12-31T23:59:59Z")))
        assertEquals(STANDARD, rules.getStandardOffset(Instant.parse("1990-01-01T00:00:00Z")))
        assertFailsWith<IllegalArgumentException> {
            ZoneRules.of(STANDARD, STANDARD, emptyList(), emptyList(), List(17) { lastRules()[0] })
        }
    }

    private fun europeanRules(): ZoneRules = ZoneRules.of(
        STANDARD,
        STANDARD,
        emptyList(),
        historicTransitions(),
        lastRules(),
    )

    private fun historicTransitions(): List<ZoneOffsetTransition> = listOf(
        ZoneOffsetTransition.of(LocalDateTime.of(2023, 3, 26, 2, 0), STANDARD, SUMMER),
        ZoneOffsetTransition.of(LocalDateTime.of(2023, 10, 29, 3, 0), SUMMER, STANDARD),
    )

    private fun lastRules(): List<ZoneOffsetTransitionRule> = listOf(
        ZoneOffsetTransitionRule.of(
            Month.MARCH,
            -1,
            DayOfWeek.SUNDAY,
            LocalTime.of(1, 0),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.UTC,
            STANDARD,
            STANDARD,
            SUMMER,
        ),
        ZoneOffsetTransitionRule.of(
            Month.OCTOBER,
            -1,
            DayOfWeek.SUNDAY,
            LocalTime.of(1, 0),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.UTC,
            STANDARD,
            SUMMER,
            STANDARD,
        ),
    )

    private companion object {
        val STANDARD: ZoneOffset = ZoneOffset.ofHours(1)
        val SUMMER: ZoneOffset = ZoneOffset.ofHours(2)
    }
}
