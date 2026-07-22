package io.heapy.grogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.Period as JavaPeriod
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.grogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodJavaConformanceTest {
    @Test
    fun valueAndArithmeticBehaviorMatchesJavaTime() {
        val values = listOf(
            Triple(Int.MIN_VALUE, 0, 0),
            Triple(-1, -13, -31),
            Triple(0, 0, 0),
            Triple(1, 2, 3),
            Triple(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE),
        )
        val amounts = listOf(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE)
        val scalars = listOf(Int.MIN_VALUE, -1, 0, 1, 2, Int.MAX_VALUE)

        values.forEach { (years, months, days) ->
            val javaPeriod = JavaPeriod.of(years, months, days)
            val period = Period.of(years, months, days)
            assertEquals(javaPeriod.years, period.years)
            assertEquals(javaPeriod.months, period.months)
            assertEquals(javaPeriod.days, period.days)
            assertEquals(javaPeriod.isZero, period.isZero)
            assertEquals(javaPeriod.isNegative, period.isNegative)
            assertEquals(javaPeriod.toTotalMonths(), period.toTotalMonths())
            assertEquals(javaPeriod.toString(), period.toString())
            assertEquals(javaPeriod.hashCode(), period.hashCode())
            assertEquals(
                javaPeriod.units.map { it.toString() },
                period.units.map { it.toString() },
            )
            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertSameOutcome(
                    javaOperation = { javaPeriod.get(javaUnit) },
                    kotlinOperation = { period.get(unit) },
                    context = "period=$period unit=$unit",
                )
            }

            amounts.forEach { amount ->
                assertSameOutcome(
                    javaOperation = { javaPeriod.plusYears(amount).toString() },
                    kotlinOperation = { period.plusYears(amount).toString() },
                    context = "period=$period plusYears=$amount",
                )
                assertSameOutcome(
                    javaOperation = { javaPeriod.plusMonths(amount).toString() },
                    kotlinOperation = { period.plusMonths(amount).toString() },
                    context = "period=$period plusMonths=$amount",
                )
                assertSameOutcome(
                    javaOperation = { javaPeriod.plusDays(amount).toString() },
                    kotlinOperation = { period.plusDays(amount).toString() },
                    context = "period=$period plusDays=$amount",
                )
                assertSameOutcome(
                    javaOperation = { javaPeriod.minusYears(amount).toString() },
                    kotlinOperation = { period.minusYears(amount).toString() },
                    context = "period=$period minusYears=$amount",
                )
                assertSameOutcome(
                    javaOperation = { javaPeriod.minusMonths(amount).toString() },
                    kotlinOperation = { period.minusMonths(amount).toString() },
                    context = "period=$period minusMonths=$amount",
                )
                assertSameOutcome(
                    javaOperation = { javaPeriod.minusDays(amount).toString() },
                    kotlinOperation = { period.minusDays(amount).toString() },
                    context = "period=$period minusDays=$amount",
                )
            }
            scalars.forEach { scalar ->
                assertSameOutcome(
                    javaOperation = { javaPeriod.multipliedBy(scalar).toString() },
                    kotlinOperation = { period.multipliedBy(scalar).toString() },
                    context = "period=$period scalar=$scalar",
                )
            }
            assertSameOutcome(
                javaOperation = { javaPeriod.normalized().toString() },
                kotlinOperation = { period.normalized().toString() },
                context = "period=$period normalized",
            )
        }

        values.forEach { first ->
            val javaFirst = JavaPeriod.of(first.first, first.second, first.third)
            val firstPeriod = Period.of(first.first, first.second, first.third)
            values.forEach { second ->
                val javaSecond = JavaPeriod.of(second.first, second.second, second.third)
                val secondPeriod = Period.of(second.first, second.second, second.third)
                assertSameOutcome(
                    javaOperation = { javaFirst.plus(javaSecond).toString() },
                    kotlinOperation = { firstPeriod.plus(secondPeriod).toString() },
                    context = "$firstPeriod plus $secondPeriod",
                )
                assertSameOutcome(
                    javaOperation = { javaFirst.minus(javaSecond).toString() },
                    kotlinOperation = { firstPeriod.minus(secondPeriod).toString() },
                    context = "$firstPeriod minus $secondPeriod",
                )
            }
        }
    }

    @Test
    fun temporalApplicationMatchesJavaTime() {
        val dates = listOf(
            JavaLocalDate.MIN,
            JavaLocalDate.of(-1, 2, 28),
            JavaLocalDate.of(2024, 1, 31),
            JavaLocalDate.MAX,
        )
        val periods = listOf(
            JavaPeriod.of(-1, -2, -3),
            JavaPeriod.ZERO,
            JavaPeriod.of(1, 2, 3),
            JavaPeriod.of(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE),
        )

        dates.forEach { javaDate ->
            val date = LocalDate.of(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth)
            periods.forEach { javaPeriod ->
                val period = Period.of(javaPeriod.years, javaPeriod.months, javaPeriod.days)
                assertSameOutcome(
                    javaOperation = { javaDate.plus(javaPeriod).toString() },
                    kotlinOperation = { date.plus(period).toString() },
                    context = "date=$date plus $period",
                )
                assertSameOutcome(
                    javaOperation = { javaDate.minus(javaPeriod).toString() },
                    kotlinOperation = { date.minus(period).toString() },
                    context = "date=$date minus $period",
                )
            }
        }
    }

    @Test
    fun parsingMatchesJavaTime() {
        val inputs = listOf(
            "P0D",
            "P2Y",
            "p-2y",
            "+P1Y-2M+3W-4D",
            "-P1Y2M3W4D",
            "P2147483647Y",
            "P-2147483648Y",
            "P306783378W",
            "P306783379W",
            "-P-2147483648Y",
            "",
            "P",
            "PT1H",
            "P1D2Y",
            "P2147483648Y",
        )

        inputs.forEach { input ->
            assertSameOutcome(
                javaOperation = { JavaPeriod.parse(input).toString() },
                kotlinOperation = { Period.parse(input).toString() },
                context = input,
            )
        }
    }

    @Test
    fun dateDifferenceMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -719_893L,
            -1L,
            0L,
            19_782L,
            365_241_780_471L,
        )

        epochDays.forEach { startEpochDay ->
            val javaStart = JavaLocalDate.ofEpochDay(startEpochDay)
            val start = LocalDate.ofEpochDay(startEpochDay)
            epochDays.forEach { endEpochDay ->
                val javaEnd = JavaLocalDate.ofEpochDay(endEpochDay)
                val end = LocalDate.ofEpochDay(endEpochDay)
                assertEquals(javaStart.until(javaEnd).toString(), start.until(end).toString())
                assertEquals(
                    JavaPeriod.between(javaStart, javaEnd).toString(),
                    Period.between(start, end).toString(),
                )
            }
        }
    }

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
