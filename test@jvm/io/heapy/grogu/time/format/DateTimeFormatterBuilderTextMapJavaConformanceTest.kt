package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderTextMapJavaConformanceTest {
    @Test
    fun mappedAndNumericFormattingMatchesJavaTime() {
        val lookup = linkedMapOf(1L to "JNY", 2L to "FBY")
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendText(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()

        (1..3).forEach { month ->
            assertEquals(
                javaFormatter.format(java.time.LocalDate.of(2024, month, 1)),
                groguFormatter.format(io.heapy.grogu.time.LocalDate.of(2024, month, 1)),
            )
        }
        assertEquals(javaFormatter.toString(), groguFormatter.toString())
    }

    @Test
    fun strictLenientAndCaseInsensitiveParsingMatchesJavaTime() {
        val lookup = linkedMapOf(1L to "Jan", 2L to "January", 3L to "MARCH")
        val inputs = listOf("Jan", "January", "january", "MARCH", "march", "1", "3", "+3", "")

        listOf("strict", "insensitive", "lenient").forEach { mode ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            val groguBuilder = DateTimeFormatterBuilder()
            when (mode) {
                "insensitive" -> {
                    javaBuilder.parseCaseInsensitive()
                    groguBuilder.parseCaseInsensitive()
                }
                "lenient" -> {
                    javaBuilder.parseLenient()
                    groguBuilder.parseLenient()
                }
            }
            val javaFormatter = javaBuilder
                .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
                .toFormatter()
            val groguFormatter = groguBuilder
                .appendText(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
                .toFormatter()

            inputs.forEach { text ->
                val javaResult = runCatching {
                    javaFormatter.parse(text).getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                }
                val groguResult = runCatching {
                    groguFormatter.parse(text)
                        .getLong(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR)
                }
                assertEquals(javaResult.isSuccess, groguResult.isSuccess, "mode=$mode text=$text")
                if (javaResult.isSuccess) {
                    assertEquals(javaResult.getOrThrow(), groguResult.getOrThrow(), text)
                }
            }
        }
    }

    @Test
    fun lookupSnapshotLongestMatchAndDuplicateTextMatchJavaTime() {
        val lookup = linkedMapOf(1L to "A", 2L to "Alphabet", 3L to "A")
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendText(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()
        lookup[2L] = "Changed"

        listOf("A", "Alphabet", "Changed").forEach { text ->
            val javaResult = runCatching {
                javaFormatter.parse(text).getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            }
            val groguResult = runCatching {
                groguFormatter.parse(text)
                    .getLong(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR)
            }
            assertEquals(javaResult.isSuccess, groguResult.isSuccess, text)
            if (javaResult.isSuccess) {
                assertEquals(javaResult.getOrThrow(), groguResult.getOrThrow(), text)
            }
        }
    }
}
