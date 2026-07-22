package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import java.time.LocalTime as JavaLocalTime
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import java.time.format.ResolverStyle as JavaResolverStyle
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterDayPeriodJavaConformanceTest {
    @Test
    fun formattingMatchesJavaTimeAcrossLocalesStylesAndHours() {
        val locales = listOf("en-US", "fr-FR", "de-DE", "ja-JP", "zh-CN")
        val patterns = listOf("B", "BBBB", "BBBBB")
        val times = listOf(0, 1, 5, 6, 11, 12, 13, 17, 18, 21, 23)

        locales.forEach { tag ->
            patterns.forEach { pattern ->
                val javaFormatter = JavaDateTimeFormatter.ofPattern(pattern, JavaLocale.forLanguageTag(tag))
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(tag))
                assertEquals(javaFormatter.toString(), formatter.toString(), "$tag $pattern description")
                times.forEach { hour ->
                    assertEquals(
                        javaFormatter.format(JavaLocalTime.of(hour, 0)),
                        formatter.format(LocalTime.of(hour, 0)),
                        "$tag $pattern $hour:00",
                    )
                }
            }
        }
    }

    @Test
    fun parsingAndResolutionMatchJavaTime() {
        val cases = listOf(
            "B" to listOf("midnight", "in the morning", "noon", "in the afternoon", "at night"),
            "h B" to listOf("3 in the morning", "3 in the afternoon", "11 at night"),
            "HH:mm B" to listOf("03:15 in the morning", "15:15 in the afternoon"),
        )

        ResolverStyle.entries.forEach { resolverStyle ->
            cases.forEach { (pattern, texts) ->
                val javaFormatter = JavaDateTimeFormatter.ofPattern(pattern, JavaLocale.US)
                    .withResolverStyle(JavaResolverStyle.valueOf(resolverStyle.name))
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
                    .withResolverStyle(resolverStyle)
                texts.forEach { text ->
                    val javaResult = runCatching { javaFormatter.parse(text, JavaLocalTime::from) }
                    val result = runCatching { formatter.parse(text, LocalTime::from) }
                    assertEquals(javaResult.isSuccess, result.isSuccess, "$resolverStyle $pattern $text")
                    if (javaResult.isSuccess) {
                        assertEquals(javaResult.getOrThrow().toString(), result.getOrThrow().toString())
                    }
                }
            }
        }
    }

    @Test
    fun invalidPatternWidthsMatchJavaTime() {
        listOf("BB", "BBB", "BBBBBB").forEach { pattern ->
            assertFailsWith<IllegalArgumentException> { JavaDateTimeFormatter.ofPattern(pattern) }
            assertFailsWith<IllegalArgumentException> { DateTimeFormatter.ofPattern(pattern) }
        }
    }
}
