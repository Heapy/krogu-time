package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.format.TextStyle
import java.time.LocalDate as JavaLocalDate
import java.time.LocalTime as JavaLocalTime
import java.time.Month as JavaMonth
import java.time.chrono.HijrahDate as JavaHijrahDate
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoField as JavaChronoField
import java.util.Locale as JavaLocale
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthJavaConformanceTest {
    @Test
    fun factoriesAndLocalizedNamesMatchJavaTime() {
        val languageTags = listOf("und", "en", "fr", "de", "ja", "ar")
        Month.entries.forEach { month ->
            val javaMonth = JavaMonth.valueOf(month.name)
            languageTags.forEach { languageTag ->
                val locale = Locale.forLanguageTag(languageTag)
                val javaLocale = JavaLocale.forLanguageTag(languageTag)
                TextStyle.entries.forEach { style ->
                    assertEquals(
                        javaMonth.getDisplayName(JavaTextStyle.valueOf(style.name), javaLocale),
                        month.getDisplayName(style, locale),
                        message = "$month, $style, $languageTag",
                    )
                }
            }
        }

        val dates = listOf(
            LocalDate.of(1970, 1, 1) to JavaLocalDate.of(1970, 1, 1),
            LocalDate.of(2024, 2, 29) to JavaLocalDate.of(2024, 2, 29),
            LocalDate.of(-123, 12, 31) to JavaLocalDate.of(-123, 12, 31),
        )
        dates.forEach { (date, javaDate) ->
            assertEquals(JavaMonth.from(javaDate).name, Month.from(date).name)
        }

        listOf(
            Triple(1445, 9, 1),
            Triple(1400, 1, 1),
            Triple(1500, 12, 30),
        ).forEach { (year, month, day) ->
            assertEquals(
                JavaMonth.from(JavaHijrahDate.of(year, month, day)).name,
                Month.from(HijrahDate.of(year, month, day)).name,
            )
        }
        assertSameOutcome(
            javaOperation = { JavaMonth.from(JavaLocalTime.NOON) },
            kotlinOperation = { Month.from(LocalTime.NOON) },
        )
    }

    @Test
    fun coreBehaviorMatchesJavaTime() {
        val amounts = listOf(
            Long.MIN_VALUE,
            -10_000L,
            -13L,
            -12L,
            -1L,
            0L,
            1L,
            12L,
            13L,
            10_000L,
            Long.MAX_VALUE,
        )

        Month.entries.forEach { month ->
            val javaMonth = JavaMonth.valueOf(month.name)
            assertEquals(javaMonth.value, month.value)
            assertEquals(
                javaMonth.isSupported(JavaChronoField.MONTH_OF_YEAR),
                month.isSupported(ChronoField.MONTH_OF_YEAR),
            )
            assertEquals(
                javaMonth.getLong(JavaChronoField.MONTH_OF_YEAR),
                month.getLong(ChronoField.MONTH_OF_YEAR),
            )
            assertEquals(javaMonth.minLength(), month.minLength())
            assertEquals(javaMonth.maxLength(), month.maxLength())
            assertEquals(javaMonth.firstMonthOfQuarter().name, month.firstMonthOfQuarter().name)

            listOf(false, true).forEach { leapYear ->
                assertEquals(javaMonth.length(leapYear), month.length(leapYear))
                assertEquals(javaMonth.firstDayOfYear(leapYear), month.firstDayOfYear(leapYear))
            }

            amounts.forEach { amount ->
                assertEquals(javaMonth.plus(amount).name, month.plus(amount).name)
                assertEquals(javaMonth.minus(amount).name, month.minus(amount).name)
            }
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)

        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
    }
}
