package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalFieldDisplayNameJavaConformanceTest {
    @Test
    fun standardAndComputedFieldDisplayNamesMatchJavaTime() {
        val locales = listOf(
            Locale.ROOT,
            Locale.ENGLISH,
            Locale.UK,
            Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("de"),
            Locale.forLanguageTag("ru"),
            Locale.forLanguageTag("ja"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("en-US-u-rg-gbzzzz"),
        )
        locales.forEach { locale ->
            val javaLocale = java.util.Locale.forLanguageTag(locale.toLanguageTag())
            ChronoField.entries.forEach { field ->
                assertEquals(
                    java.time.temporal.ChronoField.valueOf(field.name).getDisplayName(javaLocale),
                    field.getDisplayName(locale),
                    "${locale.toLanguageTag()} $field",
                )
            }

            val javaIsoFields = listOf(
                java.time.temporal.IsoFields.DAY_OF_QUARTER,
                java.time.temporal.IsoFields.QUARTER_OF_YEAR,
                java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
                java.time.temporal.IsoFields.WEEK_BASED_YEAR,
            )
            val groguIsoFields = listOf(
                IsoFields.DAY_OF_QUARTER,
                IsoFields.QUARTER_OF_YEAR,
                IsoFields.WEEK_OF_WEEK_BASED_YEAR,
                IsoFields.WEEK_BASED_YEAR,
            )
            javaIsoFields.zip(groguIsoFields).forEach { (javaField, field) ->
                assertEquals(
                    javaField.getDisplayName(javaLocale),
                    field.getDisplayName(locale),
                    "${locale.toLanguageTag()} $field",
                )
            }

            val javaWeekFields = java.time.temporal.WeekFields.ISO
            val groguWeekFields = WeekFields.ISO
            val javaComputedFields = listOf(
                javaWeekFields.dayOfWeek(),
                javaWeekFields.weekOfMonth(),
                javaWeekFields.weekOfYear(),
                javaWeekFields.weekOfWeekBasedYear(),
                javaWeekFields.weekBasedYear(),
            )
            val groguComputedFields = listOf(
                groguWeekFields.dayOfWeek,
                groguWeekFields.weekOfMonth,
                groguWeekFields.weekOfYear,
                groguWeekFields.weekOfWeekBasedYear,
                groguWeekFields.weekBasedYear,
            )
            javaComputedFields.zip(groguComputedFields).forEach { (javaField, field) ->
                assertEquals(
                    javaField.getDisplayName(javaLocale),
                    field.getDisplayName(locale),
                    "${locale.toLanguageTag()} $field",
                )
            }
        }
    }
}
