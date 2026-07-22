package io.heapy.grogu.time.temporal

import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.temporal.TemporalAdjuster as JavaTemporalAdjuster
import java.time.temporal.TemporalAdjusters as JavaTemporalAdjusters
import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalAdjustersJavaConformanceTest {
    private val dates = listOf(
        LocalDate.MIN,
        LocalDate.of(-1, 2, 28),
        LocalDate.of(2023, 2, 15),
        LocalDate.of(2024, 2, 29),
        LocalDate.of(2024, 12, 31),
        LocalDate.MAX,
    )

    @Test
    fun boundaryAdjustersMatchJavaTime() {
        val adjusters = listOf(
            JavaTemporalAdjusters.firstDayOfMonth() to TemporalAdjusters.firstDayOfMonth(),
            JavaTemporalAdjusters.lastDayOfMonth() to TemporalAdjusters.lastDayOfMonth(),
            JavaTemporalAdjusters.firstDayOfNextMonth() to TemporalAdjusters.firstDayOfNextMonth(),
            JavaTemporalAdjusters.firstDayOfYear() to TemporalAdjusters.firstDayOfYear(),
            JavaTemporalAdjusters.lastDayOfYear() to TemporalAdjusters.lastDayOfYear(),
            JavaTemporalAdjusters.firstDayOfNextYear() to TemporalAdjusters.firstDayOfNextYear(),
        )
        dates.forEach { date ->
            adjusters.forEachIndexed { index, (javaAdjuster, adjuster) ->
                assertSameOutcome(
                    javaOperation = { date.toJava().with(javaAdjuster).toString() },
                    kotlinOperation = { date.with(adjuster).toString() },
                    context = "$date boundary adjuster $index",
                )
            }
        }
    }

    @Test
    fun ordinalAndAdjacentWeekdayAdjustersMatchJavaTime() {
        dates.forEach { date ->
            DayOfWeek.entries.forEach { dayOfWeek ->
                val javaDayOfWeek = JavaDayOfWeek.valueOf(dayOfWeek.name)
                val pairs = listOf(
                    JavaTemporalAdjusters.firstInMonth(javaDayOfWeek) to
                        TemporalAdjusters.firstInMonth(dayOfWeek),
                    JavaTemporalAdjusters.lastInMonth(javaDayOfWeek) to
                        TemporalAdjusters.lastInMonth(dayOfWeek),
                    JavaTemporalAdjusters.next(javaDayOfWeek) to TemporalAdjusters.next(dayOfWeek),
                    JavaTemporalAdjusters.nextOrSame(javaDayOfWeek) to
                        TemporalAdjusters.nextOrSame(dayOfWeek),
                    JavaTemporalAdjusters.previous(javaDayOfWeek) to
                        TemporalAdjusters.previous(dayOfWeek),
                    JavaTemporalAdjusters.previousOrSame(javaDayOfWeek) to
                        TemporalAdjusters.previousOrSame(dayOfWeek),
                )
                pairs.forEachIndexed { index, (javaAdjuster, adjuster) ->
                    assertSameOutcome(
                        javaOperation = { date.toJava().with(javaAdjuster).toString() },
                        kotlinOperation = { date.with(adjuster).toString() },
                        context = "$date $dayOfWeek adjacent adjuster $index",
                    )
                }

                listOf(Int.MIN_VALUE, -5, -1, 0, 1, 5, Int.MAX_VALUE).forEach { ordinal ->
                    assertSameOutcome(
                        javaOperation = {
                            date.toJava().with(
                                JavaTemporalAdjusters.dayOfWeekInMonth(ordinal, javaDayOfWeek),
                            ).toString()
                        },
                        kotlinOperation = {
                            date.with(
                                TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek),
                            ).toString()
                        },
                        context = "$date $dayOfWeek ordinal=$ordinal",
                    )
                }
            }
        }
    }

    @Test
    fun dateOperatorAdjustmentMatchesJavaTimeAndPreservesTime() {
        val dateTime = LocalDateTime.of(2024, 2, 28, 13, 14, 15, 123_456_789)
        val javaDateTime = JavaLocalDateTime.of(2024, 2, 28, 13, 14, 15, 123_456_789)
        val javaAdjuster: JavaTemporalAdjuster = JavaTemporalAdjusters.ofDateAdjuster { it.plusDays(2) }
        val adjuster = TemporalAdjusters.ofDateAdjuster { it.plusDays(2) }
        assertEquals(javaDateTime.with(javaAdjuster).toString(), dateTime.with(adjuster).toString())
    }

    private fun LocalDate.toJava(): JavaLocalDate = JavaLocalDate.of(year, monthValue, dayOfMonth)

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), context)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }
}
