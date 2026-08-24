package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterResolverStyleJavaConformanceTest {
    @Test
    fun dateAndTimeResolutionMatchesJavaTime() {
        ResolverStyle.entries.forEach { style ->
            val javaStyle = java.time.format.ResolverStyle.valueOf(style.name)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                    .withResolverStyle(javaStyle)
                    .resolverStyle
                    .name,
                DateTimeFormatter.ISO_LOCAL_DATE
                    .withResolverStyle(style)
                    .resolverStyle
                    .name,
            )
        }

        assertDateMatches("2019-02-29", ResolverStyle.SMART)
        assertDateMatches("2019-15-40", ResolverStyle.LENIENT)
        assertTimeMatches("24:00", ResolverStyle.SMART)
        assertTimeMatches("25:61", ResolverStyle.LENIENT)
        assertDateTimeMatches("2019-02-29T24:00", ResolverStyle.SMART)
        assertOffsetDateTimeMatches("2019-02-29T24:00+02:00", ResolverStyle.SMART)
        assertAlternativeDateMatches(
            "2019-366",
            ResolverStyle.LENIENT,
            java.time.format.DateTimeFormatter.ISO_ORDINAL_DATE,
            DateTimeFormatter.ISO_ORDINAL_DATE,
        )
        assertAlternativeDateMatches(
            "2019-W53-1",
            ResolverStyle.SMART,
            java.time.format.DateTimeFormatter.ISO_WEEK_DATE,
            DateTimeFormatter.ISO_WEEK_DATE,
        )
        assertAlternativeDateMatches(
            "20190229",
            ResolverStyle.SMART,
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
        )
        assertRfcMatches("29 Feb 2019 12:00 GMT", ResolverStyle.SMART)
        assertRfcMatches("40 Feb 2019 25:61 GMT", ResolverStyle.LENIENT)
    }

    private fun assertDateMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = DateTimeFormatter.ISO_LOCAL_DATE
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalDate.from(javaParsed).toString(),
            io.heapy.krogu.time.LocalDate.from(kroguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalTime.from(javaParsed).toString(),
            io.heapy.krogu.time.LocalTime.from(kroguParsed).toString(),
            "$style $text",
        )
        assertEquals(
            javaParsed.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            kroguParsed.query(DateTimeFormatter.parsedExcessDays()).toString(),
            "$style $text excess days",
        )
    }

    private fun assertDateTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalDateTime.from(javaParsed).toString(),
            io.heapy.krogu.time.LocalDateTime.from(kroguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertOffsetDateTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.OffsetDateTime.from(javaParsed).toString(),
            io.heapy.krogu.time.OffsetDateTime.from(kroguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertAlternativeDateMatches(
        text: String,
        style: ResolverStyle,
        javaFormatter: java.time.format.DateTimeFormatter,
        kroguFormatter: DateTimeFormatter,
    ) {
        val javaParsed = javaFormatter
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = kroguFormatter.withResolverStyle(style).parse(text)
        assertEquals(
            java.time.LocalDate.from(javaParsed).toString(),
            io.heapy.krogu.time.LocalDate.from(kroguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertRfcMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val kroguParsed = DateTimeFormatter.RFC_1123_DATE_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.OffsetDateTime.from(javaParsed).toString(),
            io.heapy.krogu.time.OffsetDateTime.from(kroguParsed).toString(),
            "$style $text",
        )
    }
}
