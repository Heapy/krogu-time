package io.heapy.grogu.time.format

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
            val groguParsed = DateTimeFormatter.ISO_INSTANT.parse(text)

            assertEquals(
                javaParsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
                groguParsed.query(DateTimeFormatter.parsedLeapSecond()),
                text,
            )
            assertEquals(
                java.time.Instant.from(javaParsed).toString(),
                io.heapy.grogu.time.Instant.from(groguParsed).toString(),
                text,
            )
        }

        val javaTime = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME.parse("12:30")
        val groguTime = DateTimeFormatter.ISO_LOCAL_TIME.parse("12:30")
        assertEquals(
            javaTime.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            groguTime.query(DateTimeFormatter.parsedExcessDays()).toString(),
        )
    }
}
