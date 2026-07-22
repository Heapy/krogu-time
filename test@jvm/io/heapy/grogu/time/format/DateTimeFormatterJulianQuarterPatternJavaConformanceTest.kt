package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.IsoFields
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterJulianQuarterPatternJavaConformanceTest {
    @Test
    fun numericPatternPrintingParsingAndDescriptionsMatchJavaTime() {
        val javaDate = java.time.LocalDate.of(2024, 5, 31)
        val groguDate = LocalDate.of(2024, 5, 31)

        listOf("g", "gg", "gggggg", "Q", "QQ", "q", "qq").forEach { pattern ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
            val groguFormatter = DateTimeFormatter.ofPattern(pattern)
            assertEquals(javaFormatter.toString(), groguFormatter.toString(), pattern)
            assertEquals(javaFormatter.format(javaDate), groguFormatter.format(groguDate), pattern)
        }

        listOf("-1", "0", "40587", "60369").forEach { text ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern("g")
            val groguFormatter = DateTimeFormatter.ofPattern("g")
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter).toString(),
                groguFormatter.parse(text, LocalDate::from).toString(),
                text,
            )
        }
    }

    @Test
    fun quarterResolutionStylesMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-'Q'Q-")
            .appendValue(java.time.temporal.IsoFields.DAY_OF_QUARTER)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-'Q'Q-")
            .appendValue(IsoFields.DAY_OF_QUARTER)
            .toFormatter()

        listOf(
            java.time.format.ResolverStyle.STRICT to ResolverStyle.STRICT,
            java.time.format.ResolverStyle.SMART to ResolverStyle.SMART,
            java.time.format.ResolverStyle.LENIENT to ResolverStyle.LENIENT,
        ).forEach { (javaStyle, groguStyle) ->
            val text = if (javaStyle == java.time.format.ResolverStyle.LENIENT) {
                "2023-Q5-1"
            } else {
                "2024-Q1-60"
            }
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter.withResolverStyle(javaStyle)).toString(),
                groguFormatter.withResolverStyle(groguStyle).parse(text, LocalDate::from).toString(),
                groguStyle.toString(),
            )
        }
    }
}
