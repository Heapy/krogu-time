package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterAdjacentValueErrorIndexJavaConformanceTest {
    private val texts = listOf(
        "",
        "A",
        "0",
        "1",
        "9",
        "00",
        "01",
        "10",
        "12",
        "99",
        "1A",
        "9A",
        "12A",
        "123",
        "1234",
        "+1",
        "-1",
    )

    @Test
    fun adjacentValueFailuresReportTheConsumedFieldIndexLikeJavaTime() {
        val mismatches = texts.mapNotNull { text ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .appendValue(
                    java.time.temporal.ChronoField.DAY_OF_MONTH,
                    1,
                    19,
                    java.time.format.SignStyle.NEVER,
                )
                .appendValue(java.time.temporal.ChronoField.DAY_OF_YEAR, 1)
                .toFormatter()
            val javaPosition = java.text.ParsePosition(0)
            val javaParsed = javaFormatter.parseUnresolved(text, javaPosition)

            val kroguFormatter = DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 19, SignStyle.NEVER)
                .appendValue(ChronoField.DAY_OF_YEAR, 1)
                .toFormatter()
            val kroguPosition = ParsePosition(0)
            val kroguParsed = kroguFormatter.parseUnresolved(text, kroguPosition)

            val expected = listOf(
                javaPosition.index,
                javaPosition.errorIndex,
                javaParsed?.getLong(java.time.temporal.ChronoField.DAY_OF_MONTH),
                javaParsed?.getLong(java.time.temporal.ChronoField.DAY_OF_YEAR),
            )
            val actual = listOf(
                kroguPosition.index,
                kroguPosition.errorIndex,
                kroguParsed?.getLong(ChronoField.DAY_OF_MONTH),
                kroguParsed?.getLong(ChronoField.DAY_OF_YEAR),
            )
            if (expected == actual) {
                null
            } else {
                "'$text': Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }
}
