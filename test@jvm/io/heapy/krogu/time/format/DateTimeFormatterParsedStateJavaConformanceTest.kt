package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterParsedStateJavaConformanceTest {
    @Test
    fun parsedLeapSecondAndExcessDayQueriesMatchJavaTime() {
        val instants = listOf(
            "2016-12-31T23:59:59Z",
            "2016-12-31T23:59:60Z",
            "2016-12-31T23:59:60.123456789Z",
        )
        instants.forEach { text ->
            val javaParsed = java.time.format.DateTimeFormatter.ISO_INSTANT.parse(text)
            val kroguParsed = DateTimeFormatter.ISO_INSTANT.parse(text)

            assertEquals(
                javaParsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
                kroguParsed.query(DateTimeFormatter.parsedLeapSecond()),
                text,
            )
            assertEquals(
                java.time.Instant.from(javaParsed).toString(),
                io.heapy.krogu.time.Instant.from(kroguParsed).toString(),
                text,
            )
        }

        val javaTime = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.parse("12:30")
        val kroguTime = DateTimeFormatter.ISO_LOCAL_TIME.parse("12:30")
        assertEquals(
            javaTime.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            kroguTime.query(DateTimeFormatter.parsedExcessDays()).toString(),
        )
    }
}
