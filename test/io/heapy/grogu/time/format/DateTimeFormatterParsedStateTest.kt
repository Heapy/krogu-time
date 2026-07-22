package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.Period
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DateTimeFormatterParsedStateTest {
    @Test
    fun reportsWhetherIsoInstantParsingSawALeapSecond() {
        val parsed = DateTimeFormatter.ISO_INSTANT.parse("2016-12-31T23:59:60Z")

        assertEquals(Instant.parse("2016-12-31T23:59:60Z"), Instant.from(parsed))
        assertTrue(parsed.query(DateTimeFormatter.parsedLeapSecond()))
        assertFalse(
            DateTimeFormatter.ISO_INSTANT
                .parse("2016-12-31T23:59:59Z")
                .query(DateTimeFormatter.parsedLeapSecond()),
        )
        assertFalse(
            DateTimeFormatter.ISO_LOCAL_DATE
                .parse("2024-02-29")
                .query(DateTimeFormatter.parsedLeapSecond()),
        )
    }

    @Test
    fun parsedStateQueriesAreSingletonsWithNeutralDefaults() {
        assertSame(
            DateTimeFormatter.parsedExcessDays(),
            DateTimeFormatter.parsedExcessDays(),
        )
        assertSame(
            DateTimeFormatter.parsedLeapSecond(),
            DateTimeFormatter.parsedLeapSecond(),
        )
        assertEquals(
            Period.ZERO,
            DateTimeFormatter.ISO_LOCAL_TIME
                .parse("12:30")
                .query(DateTimeFormatter.parsedExcessDays()),
        )
    }
}
