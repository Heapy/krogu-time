package io.heapy.grogu.time.zone

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Month
import io.heapy.grogu.time.ZoneOffset
import java.time.Instant as JavaInstant
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneOffset as JavaZoneOffset
import java.time.zone.ZoneOffsetTransition as JavaZoneOffsetTransition
import java.time.zone.ZoneOffsetTransitionRule as JavaZoneOffsetTransitionRule
import java.time.zone.ZoneRules as JavaZoneRules
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneRulesJavaConformanceTest {
    @Test
    fun variableRuleLookupsAndNavigationMatchJavaTime() {
        val rules = rules()
        val javaRules = javaRules()
        val instants = listOf(
            "1900-01-01T00:00:00Z",
            "2023-03-26T00:59:59.999999999Z",
            "2023-03-26T01:00:00Z",
            "2023-10-29T01:00:00Z",
            "2024-03-31T01:00:00Z",
            "2024-06-01T00:00:00Z",
            "2099-12-31T23:59:59Z",
        )
        instants.forEach { text ->
            val instant = Instant.parse(text)
            val javaInstant = JavaInstant.parse(text)
            assertEquals(javaRules.getOffset(javaInstant).toString(), rules.getOffset(instant).toString(), text)
            assertEquals(
                javaRules.getStandardOffset(javaInstant).toString(),
                rules.getStandardOffset(instant).toString(),
                text,
            )
            assertEquals(
                javaRules.getDaylightSavings(javaInstant).toString(),
                rules.getDaylightSavings(instant).toString(),
                text,
            )
            assertEquals(javaRules.isDaylightSavings(javaInstant), rules.isDaylightSavings(instant), text)
            assertEquals(
                javaRules.nextTransition(javaInstant)?.toString(),
                rules.nextTransition(instant)?.toString(),
                text,
            )
            assertEquals(
                javaRules.previousTransition(javaInstant)?.toString(),
                rules.previousTransition(instant)?.toString(),
                text,
            )
        }

        val locals = listOf(
            "2023-03-26T01:59:59.999999999",
            "2023-03-26T02:00",
            "2023-03-26T02:30",
            "2023-03-26T03:00",
            "2023-10-29T02:00",
            "2023-10-29T02:30",
            "2023-10-29T03:00",
            "2024-03-31T02:30",
            "2024-06-01T12:00",
        )
        locals.forEach { text ->
            val local = LocalDateTime.parse(text)
            val javaLocal = JavaLocalDateTime.parse(text)
            assertEquals(javaRules.getOffset(javaLocal).toString(), rules.getOffset(local).toString(), text)
            assertEquals(
                javaRules.getValidOffsets(javaLocal).map(Any::toString),
                rules.getValidOffsets(local).map(Any::toString),
                text,
            )
            assertEquals(
                javaRules.getTransition(javaLocal)?.toString(),
                rules.getTransition(local)?.toString(),
                text,
            )
        }

        assertEquals(javaRules.isFixedOffset, rules.isFixedOffset)
        assertEquals(javaRules.transitions.map(Any::toString), rules.getTransitions().map(Any::toString))
        assertEquals(javaRules.transitionRules.map(Any::toString), rules.getTransitionRules().map(Any::toString))
        assertEquals(javaRules.hashCode(), rules.hashCode())
        assertEquals(javaRules.toString(), rules.toString())
    }

    private fun rules(): ZoneRules = ZoneRules.of(
        STANDARD,
        STANDARD,
        emptyList(),
        historicTransitions(),
        lastRules(),
    )

    private fun javaRules(): JavaZoneRules = JavaZoneRules.of(
        STANDARD.toJava(),
        STANDARD.toJava(),
        emptyList(),
        historicTransitions().map { transition ->
            JavaZoneOffsetTransition.of(
                transition.dateTimeBefore.toJava(),
                transition.offsetBefore.toJava(),
                transition.offsetAfter.toJava(),
            )
        },
        lastRules().map { it.toJava() },
    )

    private fun historicTransitions(): List<ZoneOffsetTransition> = listOf(
        ZoneOffsetTransition.of(LocalDateTime.of(2023, 3, 26, 2, 0), STANDARD, SUMMER),
        ZoneOffsetTransition.of(LocalDateTime.of(2023, 10, 29, 3, 0), SUMMER, STANDARD),
    )

    private fun lastRules(): List<ZoneOffsetTransitionRule> = listOf(
        recurring(Month.MARCH, STANDARD, SUMMER),
        recurring(Month.OCTOBER, SUMMER, STANDARD),
    )

    private fun recurring(
        month: Month,
        before: ZoneOffset,
        after: ZoneOffset,
    ): ZoneOffsetTransitionRule = ZoneOffsetTransitionRule.of(
        month,
        -1,
        DayOfWeek.SUNDAY,
        LocalTime.of(1, 0),
        false,
        ZoneOffsetTransitionRule.TimeDefinition.UTC,
        STANDARD,
        before,
        after,
    )

    private fun ZoneOffsetTransitionRule.toJava(): JavaZoneOffsetTransitionRule =
        JavaZoneOffsetTransitionRule.of(
            java.time.Month.valueOf(month.name),
            dayOfMonthIndicator,
            dayOfWeek?.let { java.time.DayOfWeek.valueOf(it.name) },
            java.time.LocalTime.of(localTime.hour, localTime.minute, localTime.second),
            isMidnightEndOfDay,
            JavaZoneOffsetTransitionRule.TimeDefinition.valueOf(timeDefinition.name),
            standardOffset.toJava(),
            offsetBefore.toJava(),
            offsetAfter.toJava(),
        )

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

    private companion object {
        val STANDARD: ZoneOffset = ZoneOffset.ofHours(1)
        val SUMMER: ZoneOffset = ZoneOffset.ofHours(2)
    }
}
