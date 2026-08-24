package io.heapy.krogu.time

import io.heapy.krogu.time.format.TextStyle
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalQueries
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate as JavaLocalDate
import java.time.LocalTime as JavaLocalTime
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.TemporalQueries as JavaTemporalQueries
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DayOfWeekJavaConformanceTest {
    @Test
    fun factoriesAndLocalizedNamesMatchJavaTime() {
        val languageTags = listOf("und", "en", "fr", "de", "ja", "ar")
        DayOfWeek.entries.forEach { day ->
            val javaDay = JavaDayOfWeek.valueOf(day.name)
            languageTags.forEach { languageTag ->
                val locale = Locale.forLanguageTag(languageTag)
                val javaLocale = JavaLocale.forLanguageTag(languageTag)
                TextStyle.entries.forEach { style ->
                    assertEquals(
                        javaDay.getDisplayName(JavaTextStyle.valueOf(style.name), javaLocale),
                        day.getDisplayName(style, locale),
                        message = "$day, $style, $languageTag",
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
            assertEquals(JavaDayOfWeek.from(javaDate).name, DayOfWeek.from(date).name)
        }
        assertSameOutcome(
            javaOperation = { JavaDayOfWeek.from(JavaLocalTime.NOON) },
            kotlinOperation = { DayOfWeek.from(LocalTime.NOON) },
        )
    }

    @Test
    fun coreBehaviorMatchesJavaTime() {
        val amounts = listOf(
            Long.MIN_VALUE,
            -10_000L,
            -8L,
            -7L,
            -1L,
            0L,
            1L,
            7L,
            8L,
            10_000L,
            Long.MAX_VALUE,
        )

        DayOfWeek.entries.forEach { day ->
            val javaDay = JavaDayOfWeek.valueOf(day.name)
            assertEquals(javaDay.value, day.value)
            assertEquals(
                javaDay.isSupported(JavaChronoField.DAY_OF_WEEK),
                day.isSupported(ChronoField.DAY_OF_WEEK),
            )
            assertEquals(
                javaDay.getLong(JavaChronoField.DAY_OF_WEEK),
                day.getLong(ChronoField.DAY_OF_WEEK),
            )
            assertEquals(
                javaDay.query(JavaTemporalQueries.precision()).toString(),
                day.query(TemporalQueries.precision()).toString(),
            )

            amounts.forEach { amount ->
                assertEquals(javaDay.plus(amount).name, day.plus(amount).name)
                assertEquals(javaDay.minus(amount).name, day.minus(amount).name)
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
