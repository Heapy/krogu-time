package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderTextMapJavaConformanceTest {
    @Test
    fun mappedAndNumericFormattingMatchesJavaTime() {
        val lookup = linkedMapOf(1L to "JNY", 2L to "FBY")
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendText(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()

        (1..3).forEach { month ->
            assertEquals(
                javaFormatter.format(java.time.LocalDate.of(2024, month, 1)),
                kroguFormatter.format(io.heapy.krogu.time.LocalDate.of(2024, month, 1)),
            )
        }
        assertEquals(javaFormatter.toString(), kroguFormatter.toString())
    }

    @Test
    fun strictLenientAndCaseInsensitiveParsingMatchesJavaTime() {
        val lookup = linkedMapOf(1L to "Jan", 2L to "January", 3L to "MARCH")
        val inputs = listOf("Jan", "January", "january", "MARCH", "march", "1", "3", "+3", "")

        listOf("strict", "insensitive", "lenient").forEach { mode ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            val kroguBuilder = DateTimeFormatterBuilder()
            when (mode) {
                "insensitive" -> {
                    javaBuilder.parseCaseInsensitive()
                    kroguBuilder.parseCaseInsensitive()
                }
                "lenient" -> {
                    javaBuilder.parseLenient()
                    kroguBuilder.parseLenient()
                }
            }
            val javaFormatter = javaBuilder
                .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
                .toFormatter()
            val kroguFormatter = kroguBuilder
                .appendText(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
                .toFormatter()

            inputs.forEach { text ->
                val javaResult = runCatching {
                    javaFormatter.parse(text).getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                }
                val kroguResult = runCatching {
                    kroguFormatter.parse(text)
                        .getLong(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR)
                }
                assertEquals(javaResult.isSuccess, kroguResult.isSuccess, "mode=$mode text=$text")
                if (javaResult.isSuccess) {
                    assertEquals(javaResult.getOrThrow(), kroguResult.getOrThrow(), text)
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
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendText(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, lookup)
            .toFormatter()
        lookup[2L] = "Changed"

        listOf("A", "Alphabet", "Changed").forEach { text ->
            val javaResult = runCatching {
                javaFormatter.parse(text).getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            }
            val kroguResult = runCatching {
                kroguFormatter.parse(text)
                    .getLong(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR)
            }
            assertEquals(javaResult.isSuccess, kroguResult.isSuccess, text)
            if (javaResult.isSuccess) {
                assertEquals(javaResult.getOrThrow(), kroguResult.getOrThrow(), text)
            }
        }
    }
}
