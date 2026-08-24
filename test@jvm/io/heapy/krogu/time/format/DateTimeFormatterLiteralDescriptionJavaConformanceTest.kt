package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterLiteralDescriptionJavaConformanceTest {
    private data class LiteralInput(
        val text: String,
        val characterOverload: Boolean,
    )

    private val inputs = listOf(
        LiteralInput("a", characterOverload = true),
        LiteralInput("'", characterOverload = true),
        LiteralInput(" ", characterOverload = true),
        LiteralInput("", characterOverload = false),
        LiteralInput("a", characterOverload = false),
        LiteralInput("'", characterOverload = false),
        LiteralInput("''", characterOverload = false),
        LiteralInput("a'b", characterOverload = false),
        LiteralInput("'a", characterOverload = false),
        LiteralInput("a'", characterOverload = false),
        LiteralInput("a''b", characterOverload = false),
    )

    @Test
    fun literalDescriptionsEscapeApostrophesLikeJavaTime() {
        val mismatches = inputs.mapNotNull { input ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            if (input.characterOverload) {
                javaBuilder.appendLiteral(input.text.single())
            } else {
                javaBuilder.appendLiteral(input.text)
            }

            val kroguBuilder = DateTimeFormatterBuilder()
            if (input.characterOverload) {
                kroguBuilder.appendLiteral(input.text.single())
            } else {
                kroguBuilder.appendLiteral(input.text)
            }

            val expected = javaBuilder.toFormatter().toString()
            val actual = kroguBuilder.toFormatter().toString()
            if (expected == actual) {
                null
            } else {
                "$input: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }
}
