package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderInstantJavaConformanceTest {
    @Test
    fun instantFormattingMatchesJavaTimeForEveryFractionMode() {
        val instants = listOf(
            java.time.Instant.ofEpochSecond(0, 123_400_000) to
                io.heapy.krogu.time.Instant.ofEpochSecond(0, 123_400_000),
            java.time.Instant.ofEpochSecond(-1, 999_999_999) to
                io.heapy.krogu.time.Instant.ofEpochSecond(-1, 999_999_999),
            java.time.Instant.MIN to io.heapy.krogu.time.Instant.MIN,
            java.time.Instant.MAX to io.heapy.krogu.time.Instant.MAX,
        )

        instants.forEach { (javaInstant, kroguInstant) ->
            listOf(null, -1, 0, 1, 2, 3, 4, 6, 9).forEach { fractionalDigits ->
                val javaBuilder = java.time.format.DateTimeFormatterBuilder()
                val kroguBuilder = DateTimeFormatterBuilder()
                if (fractionalDigits == null) {
                    javaBuilder.appendInstant()
                    kroguBuilder.appendInstant()
                } else {
                    javaBuilder.appendInstant(fractionalDigits)
                    kroguBuilder.appendInstant(fractionalDigits)
                }

                assertEquals(
                    javaBuilder.toFormatter().format(javaInstant),
                    kroguBuilder.toFormatter().format(kroguInstant),
                    "fractionalDigits=$fractionalDigits instant=$javaInstant",
                )
            }
        }
    }

    @Test
    fun strictAndLenientInstantParsingMatchesJavaTime() {
        val inputs = listOf(
            "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00.Z",
            "1970-01-01T00:00:00.1Z",
            "1970-01-01T00:00:00.12Z",
            "1970-01-01T00:00:00.123Z",
            "1970-01-01T00:00:00.1234Z",
            "1970-01-01T00:00:00.123456789Z",
            "1970-01-01t00:00:00.123z",
            "1970-01-01T24:00:00.000Z",
            "2016-12-31T23:59:60.000Z",
            "+10000-01-01T00:00:00.000Z",
        )
        listOf(null, -1, 0, 3, 9).forEach { fractionalDigits ->
            listOf(false, true).forEach { lenient ->
                val javaBuilder = java.time.format.DateTimeFormatterBuilder()
                val kroguBuilder = DateTimeFormatterBuilder()
                if (lenient) {
                    javaBuilder.parseLenient()
                    kroguBuilder.parseLenient()
                }
                val javaFormatter = if (fractionalDigits == null) {
                    javaBuilder.appendInstant().toFormatter()
                } else {
                    javaBuilder.appendInstant(fractionalDigits).toFormatter()
                }
                val kroguFormatter = if (fractionalDigits == null) {
                    kroguBuilder.appendInstant().toFormatter()
                } else {
                    kroguBuilder.appendInstant(fractionalDigits).toFormatter()
                }

                inputs.forEach { text ->
                    val javaResult = runCatching {
                        java.time.Instant.from(javaFormatter.parse(text)).toString()
                    }
                    val kroguResult = runCatching {
                        io.heapy.krogu.time.Instant.from(kroguFormatter.parse(text)).toString()
                    }
                    assertEquals(
                        javaResult.isSuccess,
                        kroguResult.isSuccess,
                        "fractionalDigits=$fractionalDigits lenient=$lenient text=$text",
                    )
                    if (javaResult.isSuccess) {
                        assertEquals(javaResult.getOrThrow(), kroguResult.getOrThrow(), text)
                    }
                }
            }
        }
    }

    @Test
    fun composedParsingAndLeapSecondStateMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral('<')
            .appendInstant(3)
            .appendLiteral('>')
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral('<')
            .appendInstant(3)
            .appendLiteral('>')
            .toFormatter()
        val text = "<2016-12-31t23:59:60.000+01:00>"
        val javaParsed = javaFormatter.parse(text)
        val kroguParsed = kroguFormatter.parse(text)

        assertEquals(
            java.time.Instant.from(javaParsed).toString(),
            io.heapy.krogu.time.Instant.from(kroguParsed).toString(),
        )
        assertEquals(
            javaParsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
            kroguParsed.query(DateTimeFormatter.parsedLeapSecond()),
        )
    }
}
