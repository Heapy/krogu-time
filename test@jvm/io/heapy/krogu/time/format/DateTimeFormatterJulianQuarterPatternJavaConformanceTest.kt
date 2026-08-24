package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.temporal.IsoFields
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterJulianQuarterPatternJavaConformanceTest {
    @Test
    fun numericPatternPrintingParsingAndDescriptionsMatchJavaTime() {
        val javaDate = java.time.LocalDate.of(2024, 5, 31)
        val kroguDate = LocalDate.of(2024, 5, 31)

        listOf("g", "gg", "gggggg", "Q", "QQ", "q", "qq").forEach { pattern ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
            val kroguFormatter = DateTimeFormatter.ofPattern(pattern)
            assertEquals(javaFormatter.toString(), kroguFormatter.toString(), pattern)
            assertEquals(javaFormatter.format(javaDate), kroguFormatter.format(kroguDate), pattern)
        }

        listOf("-1", "0", "40587", "60369").forEach { text ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern("g")
            val kroguFormatter = DateTimeFormatter.ofPattern("g")
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter).toString(),
                kroguFormatter.parse(text, LocalDate::from).toString(),
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
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-'Q'Q-")
            .appendValue(IsoFields.DAY_OF_QUARTER)
            .toFormatter()

        listOf(
            java.time.format.ResolverStyle.STRICT to ResolverStyle.STRICT,
            java.time.format.ResolverStyle.SMART to ResolverStyle.SMART,
            java.time.format.ResolverStyle.LENIENT to ResolverStyle.LENIENT,
        ).forEach { (javaStyle, kroguStyle) ->
            val text = if (javaStyle == java.time.format.ResolverStyle.LENIENT) {
                "2023-Q5-1"
            } else {
                "2024-Q1-60"
            }
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter.withResolverStyle(javaStyle)).toString(),
                kroguFormatter.withResolverStyle(kroguStyle).parse(text, LocalDate::from).toString(),
                kroguStyle.toString(),
            )
        }
    }
}
