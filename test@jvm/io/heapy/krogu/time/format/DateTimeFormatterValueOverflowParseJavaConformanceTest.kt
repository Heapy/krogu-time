package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterValueOverflowParseJavaConformanceTest {
    private val texts = listOf(
        "99999999999999999",
        "999999999999999999",
        "1000000000000000000",
        "9223372036854775806",
        "9223372036854775807",
        "9223372036854775808",
        "9223372036854775809",
        "9999999999999999998",
        "9999999999999999999",
        "99999999999999999999",
        "-99999999999999999",
        "-999999999999999999",
        "-1000000000000000000",
        "-9223372036854775807",
        "-9223372036854775808",
        "-9223372036854775809",
        "-9223372036854775810",
        "-9999999999999999999",
        "-99999999999999999999",
    )

    @Test
    fun overflowingValuesStopAtTheLastLongThatFitsLikeJavaTime() {
        val mismatches = texts.mapNotNull { text ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .appendValue(
                    java.time.temporal.ChronoField.DAY_OF_MONTH,
                    1,
                    19,
                    java.time.format.SignStyle.NORMAL,
                )
                .toFormatter()
            val javaPosition = java.text.ParsePosition(0)
            val javaParsed = javaFormatter.parseUnresolved(text, javaPosition)

            val kroguFormatter = DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 19, SignStyle.NORMAL)
                .toFormatter()
            val kroguPosition = ParsePosition(0)
            val kroguParsed = kroguFormatter.parseUnresolved(text, kroguPosition)

            val expected = listOf(
                javaPosition.index,
                javaPosition.errorIndex,
                javaParsed?.getLong(java.time.temporal.ChronoField.DAY_OF_MONTH),
            )
            val actual = listOf(
                kroguPosition.index,
                kroguPosition.errorIndex,
                kroguParsed?.getLong(ChronoField.DAY_OF_MONTH),
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
