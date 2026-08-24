package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalQueries
import java.text.ParsePosition as JavaParsePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DateTimeFormatterParsePositionJavaConformanceTest {
    @Test
    fun resolvedPositionParsingMatchesJavaTime() {
        val cases = listOf(
            PositionCase(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ISO_LOCAL_DATE,
                "prefix:2024-02-29:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.ISO_LOCAL_TIME,
                DateTimeFormatter.ISO_LOCAL_TIME,
                "prefix:23:59:58.123:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                "prefix:2024-02-29T23:59:58.123+05:30:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                "prefix:2024-01-15T12:30:00+01:00[Europe/Paris]:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.ISO_INSTANT,
                DateTimeFormatter.ISO_INSTANT,
                "prefix:2016-12-31T23:59:60Z:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.RFC_1123_DATE_TIME,
                "prefix:Tue, 3 Jun 2008 11:05:30 GMT:suffix",
                7,
            ),
            PositionCase(
                java.time.format.DateTimeFormatter.ofPattern("uuuu/DDD HH:mm"),
                DateTimeFormatter.ofPattern("uuuu/DDD HH:mm"),
                "prefix:2024/060 23:15:suffix",
                7,
            ),
        )

        cases.forEach { case ->
            val javaPosition = JavaParsePosition(case.startIndex)
            val kroguPosition = ParsePosition(case.startIndex)

            val javaParsed = case.javaFormatter.parse(case.text, javaPosition)
            val kroguParsed = case.kroguFormatter.parse(case.text, kroguPosition)

            assertTemporalEquals(javaParsed, kroguParsed, case.text)
            assertPositionEquals(javaPosition, kroguPosition)
        }
    }

    @Test
    fun resolvedPositionFailuresMatchJavaTime() {
        val cases = listOf(
            "prefix:2024-0x-29" to 7,
            "prefix:2023-02-29:suffix" to 7,
            "" to 0,
        )

        cases.forEach { (text, startIndex) ->
            val javaPosition = JavaParsePosition(startIndex)
            val kroguPosition = ParsePosition(startIndex)

            val javaFailure = runCatching {
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.parse(text, javaPosition)
            }.exceptionOrNull()
            val kroguFailure = runCatching {
                DateTimeFormatter.ISO_LOCAL_DATE.parse(text, kroguPosition)
            }.exceptionOrNull()

            assertIs<java.time.format.DateTimeParseException>(javaFailure)
            assertIs<DateTimeParseException>(kroguFailure)
            assertEquals(javaFailure.errorIndex, kroguFailure.errorIndex, text)
            assertPositionEquals(javaPosition, kroguPosition)
        }
    }

    @Test
    fun unresolvedPositionParsingMatchesJavaTime() {
        val text = "prefix:2024-13-65:suffix"
        val javaPosition = JavaParsePosition(7)
        val kroguPosition = ParsePosition(7)

        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(
            text,
            javaPosition,
        )
        val kroguParsed = DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(text, kroguPosition)

        requireNotNull(javaParsed)
        requireNotNull(kroguParsed)
        ChronoField.entries.forEach { kroguField ->
            val javaField = java.time.temporal.ChronoField.valueOf(kroguField.name)
            assertEquals(javaParsed.isSupported(javaField), kroguParsed.isSupported(kroguField), kroguField.name)
            if (javaParsed.isSupported(javaField)) {
                assertEquals(javaParsed.getLong(javaField), kroguParsed.getLong(kroguField), kroguField.name)
            }
        }
        assertEquals(
            javaParsed.query(java.time.temporal.TemporalQueries.chronology())?.id,
            kroguParsed.query(TemporalQueries.chronology())?.id,
        )
        assertPositionEquals(javaPosition, kroguPosition)
    }

    private fun assertTemporalEquals(
        expected: java.time.temporal.TemporalAccessor,
        actual: io.heapy.krogu.time.temporal.TemporalAccessor,
        message: String,
    ) {
        ChronoField.entries.forEach { kroguField ->
            val javaField = java.time.temporal.ChronoField.valueOf(kroguField.name)
            assertEquals(expected.isSupported(javaField), actual.isSupported(kroguField), "$message: $kroguField")
            if (expected.isSupported(javaField)) {
                assertEquals(expected.getLong(javaField), actual.getLong(kroguField), "$message: $kroguField")
            }
        }
        assertEquals(
            expected.query(java.time.temporal.TemporalQueries.chronology())?.id,
            actual.query(TemporalQueries.chronology())?.id,
            message,
        )
        assertEquals(
            expected.query(java.time.temporal.TemporalQueries.zone())?.id,
            actual.query(TemporalQueries.zone())?.id,
            message,
        )
        assertEquals(
            expected.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
            actual.query(DateTimeFormatter.parsedLeapSecond()),
            message,
        )
        assertEquals(
            expected.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            actual.query(DateTimeFormatter.parsedExcessDays()).toString(),
            message,
        )
    }

    @Test
    fun unresolvedFailuresAndInvalidIndexesMatchJavaTime() {
        listOf("prefix:2024-0x-29" to 7, "" to 0).forEach { (text, startIndex) ->
            val javaPosition = JavaParsePosition(startIndex)
            val kroguPosition = ParsePosition(startIndex)

            assertNull(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(text, javaPosition))
            assertNull(DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(text, kroguPosition))
            assertPositionEquals(javaPosition, kroguPosition)
        }

        listOf(-1, 11).forEach { startIndex ->
            val javaFailure = runCatching {
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(
                    "2024-02-29",
                    JavaParsePosition(startIndex),
                )
            }.exceptionOrNull()
            val kroguFailure = runCatching {
                DateTimeFormatter.ISO_LOCAL_DATE.parseUnresolved(
                    "2024-02-29",
                    ParsePosition(startIndex),
                )
            }.exceptionOrNull()

            assertEquals(javaFailure?.javaClass, kroguFailure?.javaClass, "index=$startIndex")
        }
    }

    private fun assertPositionEquals(
        expected: JavaParsePosition,
        actual: ParsePosition,
    ) {
        assertEquals(expected.index, actual.index)
        assertEquals(expected.errorIndex, actual.errorIndex)
    }

    private data class PositionCase(
        val javaFormatter: java.time.format.DateTimeFormatter,
        val kroguFormatter: DateTimeFormatter,
        val text: String,
        val startIndex: Int,
    )
}
