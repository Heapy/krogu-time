package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.format.ResolverStyle
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate as JavaLocalDate
import java.time.format.ResolverStyle as JavaResolverStyle
import java.time.temporal.WeekFields as JavaWeekFields
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun fieldResolutionMatchesJavaTimeAcrossResolverStyles() {
        val javaFields = JavaWeekFields.ISO
        val fields = WeekFields.ISO
        val styles = listOf(
            JavaResolverStyle.STRICT to ResolverStyle.STRICT,
            JavaResolverStyle.SMART to ResolverStyle.SMART,
            JavaResolverStyle.LENIENT to ResolverStyle.LENIENT,
        )

        styles.forEach { (javaStyle, style) ->
            val javaDayValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
                javaFields.dayOfWeek() to 5,
            )
            val dayValues = mutableMapOf<TemporalField, Long>(fields.dayOfWeek to 5)
            assertNull(javaFields.dayOfWeek().resolve(javaDayValues, JavaLocalDate.EPOCH, javaStyle))
            assertNull(fields.dayOfWeek.resolve(dayValues, LocalDate.EPOCH, style))
            assertFieldValues(javaDayValues, dayValues)

            val month = if (style == ResolverStyle.LENIENT) 14L else 1L
            val weekOfMonth = if (style == ResolverStyle.LENIENT) 7L else 0L
            val javaMonthValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
                java.time.temporal.ChronoField.YEAR to 2021,
                java.time.temporal.ChronoField.MONTH_OF_YEAR to month,
                java.time.temporal.ChronoField.DAY_OF_WEEK to 5,
                javaFields.weekOfMonth() to weekOfMonth,
            )
            val monthValues = mutableMapOf<TemporalField, Long>(
                ChronoField.YEAR to 2021,
                ChronoField.MONTH_OF_YEAR to month,
                ChronoField.DAY_OF_WEEK to 5,
                fields.weekOfMonth to weekOfMonth,
            )
            val javaMonthDate = javaFields.weekOfMonth().resolve(
                javaMonthValues,
                JavaLocalDate.EPOCH,
                javaStyle,
            )
            val monthDate = fields.weekOfMonth.resolve(monthValues, LocalDate.EPOCH, style)
            assertEquals(JavaLocalDate.from(javaMonthDate).toString(), LocalDate.from(requireNotNull(monthDate)).toString())
            assertFieldValues(javaMonthValues, monthValues)

            val weekOfYear = if (style == ResolverStyle.LENIENT) 54L else 0L
            val javaYearValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
                java.time.temporal.ChronoField.YEAR to 2021,
                java.time.temporal.ChronoField.DAY_OF_WEEK to 5,
                javaFields.weekOfYear() to weekOfYear,
            )
            val yearValues = mutableMapOf<TemporalField, Long>(
                ChronoField.YEAR to 2021,
                ChronoField.DAY_OF_WEEK to 5,
                fields.weekOfYear to weekOfYear,
            )
            val javaYearDate = javaFields.weekOfYear().resolve(
                javaYearValues,
                JavaLocalDate.EPOCH,
                javaStyle,
            )
            val yearDate = fields.weekOfYear.resolve(yearValues, LocalDate.EPOCH, style)
            assertEquals(JavaLocalDate.from(javaYearDate).toString(), LocalDate.from(requireNotNull(yearDate)).toString())
            assertFieldValues(javaYearValues, yearValues)

            val weekOfWeekBasedYear = if (style == ResolverStyle.LENIENT) 54L else 53L
            val javaWeekBasedValues = mutableMapOf<java.time.temporal.TemporalField, Long>(
                javaFields.weekBasedYear() to 2020,
                javaFields.weekOfWeekBasedYear() to weekOfWeekBasedYear,
                java.time.temporal.ChronoField.DAY_OF_WEEK to 5,
            )
            val weekBasedValues = mutableMapOf<TemporalField, Long>(
                fields.weekBasedYear to 2020,
                fields.weekOfWeekBasedYear to weekOfWeekBasedYear,
                ChronoField.DAY_OF_WEEK to 5,
            )
            val javaWeekBasedDate = javaFields.weekOfWeekBasedYear().resolve(
                javaWeekBasedValues,
                JavaLocalDate.EPOCH,
                javaStyle,
            )
            val weekBasedDate = fields.weekOfWeekBasedYear.resolve(
                weekBasedValues,
                LocalDate.EPOCH,
                style,
            )
            assertEquals(
                JavaLocalDate.from(javaWeekBasedDate).toString(),
                LocalDate.from(requireNotNull(weekBasedDate)).toString(),
            )
            assertFieldValues(javaWeekBasedValues, weekBasedValues)
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

    private fun assertFieldValues(
        javaValues: Map<java.time.temporal.TemporalField, Long>,
        values: Map<TemporalField, Long>,
    ) {
        assertEquals(
            javaValues.mapKeys { (field) -> field.toString() },
            values.mapKeys { (field) -> field.toString() },
        )
    }
}
