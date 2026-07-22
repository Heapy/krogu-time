package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.IsoFields
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterLocalizedTextJavaConformanceTest {
    @Test
    fun textualPatternsFormatLikeJavaTimeAcrossLocalesAndStyles() {
        val patterns = listOf(
            "G uuuu MMMM d EEEE",
            "GGGG uuuu MMM d EEE",
            "GGGGG LLLLL EEEEE",
            "LLLL",
            "QQQ QQQQ QQQQQ",
            "qqq qqqq qqqqq",
            "h a",
        )
        val locales = listOf("en-US", "fr-FR", "de-DE", "ru-RU", "ja-JP")
        val javaDateTime = java.time.LocalDateTime.of(2024, 2, 29, 15, 0)
        val dateTime = LocalDateTime.of(LocalDate.of(2024, 2, 29), LocalTime.of(15, 0))

        locales.forEach { tag ->
            patterns.forEach { pattern ->
                val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(
                    pattern,
                    JavaLocale.forLanguageTag(tag),
                )
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(tag))

                assertEquals(javaFormatter.format(javaDateTime), formatter.format(dateTime), "$tag $pattern")
                assertEquals(javaFormatter.toString(), formatter.toString(), "$tag $pattern description")
            }
        }
    }

    @Test
    fun styledBuilderTextMatchesJavaTime() {
        val fields = listOf(
            java.time.temporal.ChronoField.ERA to ChronoField.ERA,
            java.time.temporal.ChronoField.MONTH_OF_YEAR to ChronoField.MONTH_OF_YEAR,
            java.time.temporal.ChronoField.DAY_OF_WEEK to ChronoField.DAY_OF_WEEK,
            java.time.temporal.ChronoField.AMPM_OF_DAY to ChronoField.AMPM_OF_DAY,
            java.time.temporal.IsoFields.QUARTER_OF_YEAR to IsoFields.QUARTER_OF_YEAR,
        )
        val javaTemporal = java.time.LocalDateTime.of(2024, 2, 29, 15, 0)
        val temporal = LocalDateTime.of(LocalDate.of(2024, 2, 29), LocalTime.of(15, 0))

        fields.forEach { (javaField, field) ->
            TextStyle.entries.forEach { style ->
                val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                    .appendText(javaField, java.time.format.TextStyle.valueOf(style.name))
                    .toFormatter(JavaLocale.FRANCE)
                val formatter = DateTimeFormatterBuilder()
                    .appendText(field, style)
                    .toFormatter(Locale.forLanguageTag("fr-FR"))

                val javaText = javaFormatter.format(javaTemporal)
                val text = formatter.format(temporal)
                assertEquals(javaText, text, "$field $style")
                assertEquals(javaFormatter.toString(), formatter.toString(), "$field $style description")
                assertEquals(
                    javaFormatter.parse(javaText).getLong(javaField),
                    formatter.parse(text).getLong(field),
                    "$field $style parse",
                )
            }
        }
    }

    @Test
    fun localizedTextParsingMatchesJavaTime() {
        listOf(
            Triple("d MMMM uuuu", "29 février 2024", "fr-FR"),
            Triple("MMMM d uuuu", "February 29 2024", "en-US"),
            Triple("h a", "3 PM", "en-US"),
        ).forEach { (pattern, text, tag) ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(
                pattern,
                JavaLocale.forLanguageTag(tag),
            )
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(tag))

            val javaParsed = javaFormatter.parse(text)
            val parsed = formatter.parse(text)
            listOf(
                java.time.temporal.ChronoField.YEAR to ChronoField.YEAR,
                java.time.temporal.ChronoField.MONTH_OF_YEAR to ChronoField.MONTH_OF_YEAR,
                java.time.temporal.ChronoField.DAY_OF_MONTH to ChronoField.DAY_OF_MONTH,
                java.time.temporal.ChronoField.HOUR_OF_AMPM to ChronoField.HOUR_OF_AMPM,
                java.time.temporal.ChronoField.AMPM_OF_DAY to ChronoField.AMPM_OF_DAY,
            ).forEach { (javaField, field) ->
                if (javaParsed.isSupported(javaField)) {
                    assertEquals(javaParsed.getLong(javaField), parsed.getLong(field), "$tag $pattern $field")
                }
            }
        }
    }
}
