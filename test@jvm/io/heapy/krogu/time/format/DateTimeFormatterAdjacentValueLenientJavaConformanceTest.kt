package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterAdjacentValueLenientJavaConformanceTest {
    private val inputs = listOf(
        Input(false, 1, 2, 2, "5"),
        Input(false, 1, 2, 2, "54"),
        Input(false, 1, 2, 2, "54A"),
        Input(false, 1, 2, 2, "543"),
        Input(false, 1, 2, 2, "5432"),
        Input(false, 1, 2, 2, "54321"),
        Input(true, 1, 2, 2, "5"),
        Input(true, 1, 2, 2, "54"),
        Input(true, 1, 2, 2, "54A"),
        Input(true, 1, 2, 2, "543"),
        Input(true, 1, 2, 2, "5432"),
        Input(true, 1, 2, 2, "54321"),
        Input(false, 1, 3, 1, "54"),
        Input(false, 1, 3, 3, "543"),
        Input(false, 1, 3, 3, "5432"),
        Input(false, 2, 4, 3, "5432"),
        Input(false, 2, 4, 3, "54321"),
        Input(true, 1, 3, 1, "54"),
        Input(true, 1, 3, 3, "543"),
        Input(true, 1, 3, 3, "5432"),
        Input(true, 2, 4, 3, "5432"),
        Input(true, 2, 4, 3, "54321"),
    )

    @Test
    fun adjacentFixedValuesRetainTheirWidthInLenientParsingLikeJavaTime() {
        val mismatches = inputs.mapNotNull { input ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            if (input.strict) {
                javaBuilder.parseStrict()
            } else {
                javaBuilder.parseLenient()
            }
            val javaFormatter = javaBuilder
                .appendValue(
                    java.time.temporal.ChronoField.MONTH_OF_YEAR,
                    input.minWidth,
                    input.maxWidth,
                    java.time.format.SignStyle.NORMAL,
                )
                .appendValue(
                    java.time.temporal.ChronoField.DAY_OF_MONTH,
                    input.followingWidth,
                )
                .toFormatter()
            val javaPosition = java.text.ParsePosition(0)
            val javaParsed = javaFormatter.parseUnresolved(input.text, javaPosition)

            val kroguBuilder = DateTimeFormatterBuilder()
            if (input.strict) {
                kroguBuilder.parseStrict()
            } else {
                kroguBuilder.parseLenient()
            }
            val kroguFormatter = kroguBuilder
                .appendValue(
                    ChronoField.MONTH_OF_YEAR,
                    input.minWidth,
                    input.maxWidth,
                    SignStyle.NORMAL,
                )
                .appendValue(ChronoField.DAY_OF_MONTH, input.followingWidth)
                .toFormatter()
            val kroguPosition = ParsePosition(0)
            val kroguParsed = kroguFormatter.parseUnresolved(input.text, kroguPosition)

            val expected = listOf(
                javaPosition.index,
                javaPosition.errorIndex,
                javaParsed?.getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                javaParsed?.getLong(java.time.temporal.ChronoField.DAY_OF_MONTH),
            )
            val actual = listOf(
                kroguPosition.index,
                kroguPosition.errorIndex,
                kroguParsed?.getLong(ChronoField.MONTH_OF_YEAR),
                kroguParsed?.getLong(ChronoField.DAY_OF_MONTH),
            )
            if (expected == actual) {
                null
            } else {
                "$input: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private data class Input(
        val strict: Boolean,
        val minWidth: Int,
        val maxWidth: Int,
        val followingWidth: Int,
        val text: String,
    )
}
