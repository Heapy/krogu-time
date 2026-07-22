package io.heapy.grogu.time.zone

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Month
import io.heapy.grogu.time.ZoneOffset
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalTime as JavaLocalTime
import java.time.Month as JavaMonth
import java.time.ZoneOffset as JavaZoneOffset
import java.time.zone.ZoneOffsetTransitionRule as JavaZoneOffsetTransitionRule
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneOffsetTransitionRuleJavaConformanceTest {
    @Test
    fun propertiesGeneratedTransitionsHashesAndTextMatchJavaTime() {
        rules().forEach { rule ->
            val javaRule = rule.toJava()
            assertEquals(javaRule.month.name, rule.month.name)
            assertEquals(javaRule.dayOfMonthIndicator, rule.dayOfMonthIndicator)
            assertEquals(javaRule.dayOfWeek?.name, rule.dayOfWeek?.name)
            assertEquals(javaRule.localTime.toString(), rule.localTime.toString())
            assertEquals(javaRule.isMidnightEndOfDay, rule.isMidnightEndOfDay)
            assertEquals(javaRule.timeDefinition.name, rule.timeDefinition.name)
            assertEquals(javaRule.standardOffset.toString(), rule.standardOffset.toString())
            assertEquals(javaRule.offsetBefore.toString(), rule.offsetBefore.toString())
            assertEquals(javaRule.offsetAfter.toString(), rule.offsetAfter.toString())
            assertEquals(javaRule.hashCode(), rule.hashCode())
            assertEquals(javaRule.toString(), rule.toString())
            listOf(1900, 2000, 2023, 2024, 2099).forEach { year ->
                val javaTransition = javaRule.createTransition(year)
                val transition = rule.createTransition(year)
                assertEquals(javaTransition.toString(), transition.toString())
                assertEquals(javaTransition.instant.toString(), transition.instant.toString())
            }
        }
    }

    @Test
    fun validationMatchesJavaTime() {
        val cases = listOf(
            ValidationCase(-29, LocalTime.MIDNIGHT, false),
            ValidationCase(0, LocalTime.MIDNIGHT, false),
            ValidationCase(32, LocalTime.MIDNIGHT, false),
            ValidationCase(1, LocalTime.NOON, true),
            ValidationCase(1, LocalTime.of(0, 0, 0, 1), false),
        )
        cases.forEach { case ->
            assertSameOutcome(
                javaOperation = {
                    JavaZoneOffsetTransitionRule.of(
                        JavaMonth.MARCH,
                        case.dayOfMonthIndicator,
                        JavaDayOfWeek.SUNDAY,
                        case.localTime.toJava(),
                        case.midnightEndOfDay,
                        JavaZoneOffsetTransitionRule.TimeDefinition.UTC,
                        JavaZoneOffset.UTC,
                        JavaZoneOffset.UTC,
                        JavaZoneOffset.ofHours(1),
                    )
                },
                kotlinOperation = {
                    ZoneOffsetTransitionRule.of(
                        Month.MARCH,
                        case.dayOfMonthIndicator,
                        DayOfWeek.SUNDAY,
                        case.localTime,
                        case.midnightEndOfDay,
                        ZoneOffsetTransitionRule.TimeDefinition.UTC,
                        ZoneOffset.UTC,
                        ZoneOffset.UTC,
                        ZoneOffset.ofHours(1),
                    )
                },
                context = case.toString(),
            )
        }
    }

    private fun rules(): List<ZoneOffsetTransitionRule> = listOf(
        ZoneOffsetTransitionRule.of(
            Month.MARCH,
            -1,
            DayOfWeek.SUNDAY,
            LocalTime.of(1, 0),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.UTC,
            ZoneOffset.ofHours(1),
            ZoneOffset.ofHours(1),
            ZoneOffset.ofHours(2),
        ),
        ZoneOffsetTransitionRule.of(
            Month.OCTOBER,
            -8,
            DayOfWeek.SUNDAY,
            LocalTime.of(2, 30),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.STANDARD,
            ZoneOffset.ofHours(1),
            ZoneOffset.ofHours(2),
            ZoneOffset.ofHours(1),
        ),
        ZoneOffsetTransitionRule.of(
            Month.JANUARY,
            15,
            null,
            LocalTime.MIDNIGHT,
            true,
            ZoneOffsetTransitionRule.TimeDefinition.WALL,
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            ZoneOffset.ofHours(1),
        ),
        ZoneOffsetTransitionRule.of(
            Month.JUNE,
            20,
            DayOfWeek.MONDAY,
            LocalTime.of(12, 34, 56),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.WALL,
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            ZoneOffset.ofHours(1),
        ),
    )

    private fun ZoneOffsetTransitionRule.toJava(): JavaZoneOffsetTransitionRule =
        JavaZoneOffsetTransitionRule.of(
            JavaMonth.valueOf(month.name),
            dayOfMonthIndicator,
            dayOfWeek?.let { JavaDayOfWeek.valueOf(it.name) },
            localTime.toJava(),
            isMidnightEndOfDay,
            JavaZoneOffsetTransitionRule.TimeDefinition.valueOf(timeDefinition.name),
            standardOffset.toJava(),
            offsetBefore.toJava(),
            offsetAfter.toJava(),
        )

    private fun LocalTime.toJava(): JavaLocalTime = JavaLocalTime.of(hour, minute, second, nano)

    private fun ZoneOffset.toJava(): JavaZoneOffset = JavaZoneOffset.ofTotalSeconds(totalSeconds)

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }

    private data class ValidationCase(
        val dayOfMonthIndicator: Int,
        val localTime: LocalTime,
        val midnightEndOfDay: Boolean,
    )
}
