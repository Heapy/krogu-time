package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Locale
import java.time.LocalDate as JavaLocalDate
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import java.time.format.ResolverStyle as JavaResolverStyle
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterLocalizedWeekJavaConformanceTest {
    @Test
    fun localizedWeekPatternsFormatLikeJavaTime() {
        val patterns = listOf(
            "Y w W e c",
            "YY ww ee",
            "YYYY-'W'ww-e",
            "eee ccc",
            "eeee cccc",
            "eeeee ccccc",
        )
        val locales = listOf("en-US", "en-GB", "fr-FR", "de-DE", "ar-SA", "fa-IR")
        val dates = listOf(
            2000 to 1 to 1,
            2008 to 12 to 31,
            2009 to 1 to 1,
            2020 to 12 to 31,
            2021 to 1 to 1,
            2021 to 1 to 3,
            2021 to 1 to 4,
        ).map { (yearMonth, day) ->
            val (year, month) = yearMonth
            JavaLocalDate.of(year, month, day) to LocalDate.of(year, month, day)
        }

        locales.forEach { tag ->
            patterns.forEach { pattern ->
                val javaFormatter = JavaDateTimeFormatter.ofPattern(pattern, JavaLocale.forLanguageTag(tag))
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(tag))

                assertEquals(javaFormatter.toString(), formatter.toString(), "$tag $pattern description")
                dates.forEach { (javaDate, date) ->
                    assertEquals(
                        javaFormatter.format(javaDate),
                        formatter.format(date),
                        "$tag $pattern $date",
                    )
                }
            }
        }
    }

    @Test
    fun localizedWeekPatternsParseLikeJavaTime() {
        val patterns = listOf(
            "YYYY-'W'ww-e",
            "YY-'W'ww-e",
            "uuuu-MM-W-e",
            "YYYYwwe",
            "YYwwe",
            "YYYY-'W'ww-eee",
            "YYYY-'W'ww-ccc",
        )
        val locales = listOf("en-US", "en-GB", "fr-FR", "ar-SA")
        val dates = listOf(
            JavaLocalDate.of(2000, 1, 1) to LocalDate.of(2000, 1, 1),
            JavaLocalDate.of(2009, 1, 1) to LocalDate.of(2009, 1, 1),
            JavaLocalDate.of(2020, 12, 31) to LocalDate.of(2020, 12, 31),
            JavaLocalDate.of(2021, 1, 1) to LocalDate.of(2021, 1, 1),
            JavaLocalDate.of(2021, 1, 4) to LocalDate.of(2021, 1, 4),
        )

        locales.forEach { tag ->
            patterns.forEach { pattern ->
                val javaFormatter = JavaDateTimeFormatter.ofPattern(pattern, JavaLocale.forLanguageTag(tag))
                    .withResolverStyle(JavaResolverStyle.STRICT)
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(tag))
                    .withResolverStyle(ResolverStyle.STRICT)

                dates.forEach { (javaDate, date) ->
                    val text = javaFormatter.format(javaDate)
                    val javaParsed = JavaLocalDate.from(javaFormatter.parse(text))
                    val parsed = LocalDate.from(formatter.parse(text))

                    assertEquals(javaParsed.toString(), parsed.toString(), "$tag $pattern $text")
                    assertEquals(date, parsed, "$tag $pattern round trip $text")
                }
            }
        }
    }
}
