package io.heapy.grogu.time.format

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
    }

    private fun assertDateMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val groguParsed = DateTimeFormatter.ISO_LOCAL_DATE
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalDate.from(javaParsed).toString(),
            io.heapy.grogu.time.LocalDate.from(groguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val groguParsed = DateTimeFormatter.ISO_LOCAL_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalTime.from(javaParsed).toString(),
            io.heapy.grogu.time.LocalTime.from(groguParsed).toString(),
            "$style $text",
        )
        assertEquals(
            javaParsed.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            groguParsed.query(DateTimeFormatter.parsedExcessDays()).toString(),
            "$style $text excess days",
        )
    }

    private fun assertDateTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val groguParsed = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.LocalDateTime.from(javaParsed).toString(),
            io.heapy.grogu.time.LocalDateTime.from(groguParsed).toString(),
            "$style $text",
        )
    }

    private fun assertOffsetDateTimeMatches(text: String, style: ResolverStyle) {
        val javaParsed = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            .parse(text)
        val groguParsed = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withResolverStyle(style)
            .parse(text)
        assertEquals(
            java.time.OffsetDateTime.from(javaParsed).toString(),
            io.heapy.grogu.time.OffsetDateTime.from(groguParsed).toString(),
            "$style $text",
        )
    }
}
