package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterPadParseJavaConformanceTest {
    private val texts = listOf(
        "222", "222X", "#22", "#22X", "##2", "##2X", "##22", "-22", "#-2",
        "3", "3X", "#3", "#3X", "##A", "  3", "##", "#", "",
    )

    // Strict parsing must fill the whole padded width. Java reports a short
    // parse at the start of the padded section plus the index the padding
    // ended at, which counts the start twice; a literal in front of the
    // padding is what makes that visible.
    private val leadingLiterals = listOf("", "QQ", "QQQQ")

    @Test
    fun strictPadParsingMatchesJavaTime() {
        assertEquals(emptyList(), mismatches(strict = true))
    }

    @Test
    fun lenientPadParsingMatchesJavaTime() {
        assertEquals(emptyList(), mismatches(strict = false))
    }

    private fun mismatches(strict: Boolean): List<String> =
        leadingLiterals.flatMap { literal ->
            texts.mapNotNull { suffix ->
                val text = literal + suffix

                val javaBuilder = java.time.format.DateTimeFormatterBuilder()
                if (literal.isNotEmpty()) javaBuilder.appendLiteral(literal)
                if (!strict) javaBuilder.parseLenient()
                javaBuilder.padNext(3, '#').appendValue(
                    java.time.temporal.ChronoField.MONTH_OF_YEAR,
                    1,
                    3,
                    java.time.format.SignStyle.NORMAL,
                )
                val javaPosition = java.text.ParsePosition(0)
                val javaParsed = javaBuilder.toFormatter().parseUnresolved(text, javaPosition)

                val kroguBuilder = DateTimeFormatterBuilder()
                if (literal.isNotEmpty()) kroguBuilder.appendLiteral(literal)
                if (!strict) kroguBuilder.parseLenient()
                kroguBuilder.padNext(3, '#').appendValue(
                    ChronoField.MONTH_OF_YEAR,
                    1,
                    3,
                    SignStyle.NORMAL,
                )
                val kroguPosition = ParsePosition(0)
                val kroguParsed = kroguBuilder.toFormatter().parseUnresolved(text, kroguPosition)

                val expected = listOf(
                    javaPosition.index,
                    javaPosition.errorIndex,
                    javaParsed?.isSupported(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                    javaParsed?.takeIf {
                        it.isSupported(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                    }?.getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                )
                val actual = listOf(
                    kroguPosition.index,
                    kroguPosition.errorIndex,
                    kroguParsed?.isSupported(ChronoField.MONTH_OF_YEAR),
                    kroguParsed?.takeIf {
                        it.isSupported(ChronoField.MONTH_OF_YEAR)
                    }?.getLong(ChronoField.MONTH_OF_YEAR),
                )
                if (expected == actual) {
                    null
                } else {
                    "'$literal' + '$suffix': Java=$expected, Kotlin=$actual"
                }
            }
        }
}
