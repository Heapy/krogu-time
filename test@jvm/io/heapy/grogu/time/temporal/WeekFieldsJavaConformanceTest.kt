package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.Locale
import java.util.Locale as JavaLocale
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.WeekFields as JavaWeekFields
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekFieldsJavaConformanceTest {
    @Test
    fun localeWeekDefinitionsMatchJavaTime() {
        listOf(
            "und",
            "en-US",
            "en-GB",
            "fr-FR",
            "ar-SA",
            "fa-IR",
            "en-US-u-fw-mon",
            "en-GB-u-fw-sun",
        ).forEach { languageTag ->
            val javaFields = JavaWeekFields.of(JavaLocale.forLanguageTag(languageTag))
            val fields = WeekFields.of(Locale.forLanguageTag(languageTag))

            assertDefinition(javaFields, fields)
        }
    }

    @Test
    fun everyExplicitWeekDefinitionMatchesJavaTime() {
        val dates = listOf(
            JavaLocalDate.of(2008, 12, 31) to LocalDate.of(2008, 12, 31),
            JavaLocalDate.of(2009, 1, 1) to LocalDate.of(2009, 1, 1),
            JavaLocalDate.of(2009, 1, 4) to LocalDate.of(2009, 1, 4),
            JavaLocalDate.of(2009, 1, 5) to LocalDate.of(2009, 1, 5),
            JavaLocalDate.of(2020, 2, 29) to LocalDate.of(2020, 2, 29),
            JavaLocalDate.of(2020, 12, 31) to LocalDate.of(2020, 12, 31),
            JavaLocalDate.of(2021, 1, 1) to LocalDate.of(2021, 1, 1),
            JavaLocalDate.of(2021, 12, 31) to LocalDate.of(2021, 12, 31),
        )

        JavaDayOfWeek.entries.forEach { javaFirstDay ->
            (1..7).forEach { minimalDays ->
                val javaFields = JavaWeekFields.of(javaFirstDay, minimalDays)
                val fields = WeekFields.of(DayOfWeek.of(javaFirstDay.value), minimalDays)
                assertDefinition(javaFields, fields)
                dates.forEach { (javaDate, date) ->
                    assertDateFields(javaFields, fields, javaDate, date)
                }
            }
        }
    }

    @Test
    fun fieldAdjustmentsMatchJavaTime() {
        val javaDate = JavaLocalDate.of(2020, 12, 31)
        val date = LocalDate.of(2020, 12, 31)

        listOf(
            JavaWeekFields.ISO to WeekFields.ISO,
            JavaWeekFields.SUNDAY_START to WeekFields.SUNDAY_START,
            JavaWeekFields.of(JavaDayOfWeek.WEDNESDAY, 7) to WeekFields.of(DayOfWeek.WEDNESDAY, 7),
        ).forEach { (javaFields, fields) ->
            val adjustments = listOf(
                Triple(javaFields.dayOfWeek(), fields.dayOfWeek, 2L),
                Triple(javaFields.weekOfMonth(), fields.weekOfMonth, 3L),
                Triple(javaFields.weekOfYear(), fields.weekOfYear, 20L),
                Triple(javaFields.weekOfWeekBasedYear(), fields.weekOfWeekBasedYear, 20L),
                Triple(javaFields.weekBasedYear(), fields.weekBasedYear, 2021L),
            )
            adjustments.forEach { (javaField, field, value) ->
                assertEquals(javaDate.with(javaField, value).toString(), date.with(field, value).toString())
            }
        }
    }

    private fun assertDefinition(javaFields: JavaWeekFields, fields: WeekFields) {
        assertEquals(javaFields.firstDayOfWeek.value, fields.firstDayOfWeek.value)
        assertEquals(javaFields.minimalDaysInFirstWeek, fields.minimalDaysInFirstWeek)
        assertEquals(javaFields.toString(), fields.toString())
        assertEquals(javaFields.hashCode(), fields.hashCode())
        assertEquals(javaFields == JavaWeekFields.of(javaFields.firstDayOfWeek, javaFields.minimalDaysInFirstWeek), fields == WeekFields.of(fields.firstDayOfWeek, fields.minimalDaysInFirstWeek))
    }

    private fun assertDateFields(
        javaFields: JavaWeekFields,
        fields: WeekFields,
        javaDate: JavaLocalDate,
        date: LocalDate,
    ) {
        val pairs = listOf(
            javaFields.dayOfWeek() to fields.dayOfWeek,
            javaFields.weekOfMonth() to fields.weekOfMonth,
            javaFields.weekOfYear() to fields.weekOfYear,
            javaFields.weekOfWeekBasedYear() to fields.weekOfWeekBasedYear,
            javaFields.weekBasedYear() to fields.weekBasedYear,
        )
        pairs.forEach { (javaField, field) ->
            assertEquals(javaField.toString(), field.toString())
            assertEquals(javaField.range().toString(), field.range.toString())
            assertEquals(javaDate.getLong(javaField), date.getLong(field))
            assertEquals(javaDate.range(javaField).toString(), date.range(field).toString())
        }
    }
}
