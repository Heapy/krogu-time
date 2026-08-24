package io.heapy.krogu.time.zone

import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.Month
import io.heapy.krogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZoneOffsetTransitionRuleTest {
    @Test
    fun createsRecurringTransitionsUsingUtcStandardAndWallTime() {
        val standard = ZoneOffset.ofHours(1)
        val summer = ZoneOffset.ofHours(2)
        val spring = ZoneOffsetTransitionRule.of(
            Month.MARCH,
            -1,
            DayOfWeek.SUNDAY,
            LocalTime.of(1, 0),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.UTC,
            standard,
            standard,
            summer,
        )
        assertEquals(Month.MARCH, spring.month)
        assertEquals(-1, spring.dayOfMonthIndicator)
        assertEquals(DayOfWeek.SUNDAY, spring.dayOfWeek)
        assertEquals(LocalTime.of(1, 0), spring.localTime)
        assertFalse(spring.isMidnightEndOfDay)
        assertEquals(ZoneOffsetTransitionRule.TimeDefinition.UTC, spring.timeDefinition)
        assertEquals(standard, spring.standardOffset)
        assertEquals(standard, spring.offsetBefore)
        assertEquals(summer, spring.offsetAfter)
        assertEquals(
            LocalDateTime.of(2024, 3, 31, 2, 0),
            spring.createTransition(2024).dateTimeBefore,
        )

        val autumn = ZoneOffsetTransitionRule.of(
            Month.OCTOBER,
            -8,
            DayOfWeek.SUNDAY,
            LocalTime.of(2, 30),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.STANDARD,
            standard,
            summer,
            standard,
        )
        assertEquals(
            LocalDateTime.of(2024, 10, 20, 3, 30),
            autumn.createTransition(2024).dateTimeBefore,
        )

        val endOfDay = ZoneOffsetTransitionRule.of(
            Month.JANUARY,
            15,
            null,
            LocalTime.MIDNIGHT,
            true,
            ZoneOffsetTransitionRule.TimeDefinition.WALL,
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            standard,
        )
        assertNull(endOfDay.dayOfWeek)
        assertTrue(endOfDay.isMidnightEndOfDay)
        assertEquals(
            LocalDateTime.of(2024, 1, 16, 0, 0),
            endOfDay.createTransition(2024).dateTimeBefore,
        )
    }

    @Test
    fun validatesRulesAndUsesJavaCompatibleValueSemantics() {
        val rule = sampleRule()
        assertEquals(sampleRule(), rule)
        assertEquals(sampleRule().hashCode(), rule.hashCode())
        assertEquals(
            "TransitionRule[Gap +01:00 to +02:00, SUNDAY on or before last day of " +
                "MARCH at 01:00 UTC, standard offset +01:00]",
            rule.toString(),
        )

        listOf(-29, 0, 32).forEach { day ->
            assertFailsWith<IllegalArgumentException> { sampleRule(dayOfMonthIndicator = day) }
        }
        assertFailsWith<IllegalArgumentException> {
            sampleRule(localTime = LocalTime.NOON, midnightEndOfDay = true)
        }
        assertFailsWith<IllegalArgumentException> {
            sampleRule(localTime = LocalTime.of(1, 0, 0, 1))
        }
    }

    private fun sampleRule(
        dayOfMonthIndicator: Int = -1,
        localTime: LocalTime = LocalTime.of(1, 0),
        midnightEndOfDay: Boolean = false,
    ): ZoneOffsetTransitionRule = ZoneOffsetTransitionRule.of(
        Month.MARCH,
        dayOfMonthIndicator,
        DayOfWeek.SUNDAY,
        localTime,
        midnightEndOfDay,
        ZoneOffsetTransitionRule.TimeDefinition.UTC,
        ZoneOffset.ofHours(1),
        ZoneOffset.ofHours(1),
        ZoneOffset.ofHours(2),
    )
}
