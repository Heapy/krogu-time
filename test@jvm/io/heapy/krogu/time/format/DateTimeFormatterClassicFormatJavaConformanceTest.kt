package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.temporal.TemporalQuery
import java.text.FieldPosition
import java.text.ParseException
import java.text.ParsePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class DateTimeFormatterClassicFormatJavaConformanceTest {
    @Test
    fun classicFormatPrintsAndAppendsLikeJavaTime() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat()
        val kroguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        assertEquals(
            javaFormat.format(java.time.LocalDate.of(2024, 2, 29)),
            kroguFormat.format(LocalDate.of(2024, 2, 29)),
        )

        val javaPosition = FieldPosition(0)
        val kroguPosition = FieldPosition(0)
        val javaBuffer = javaFormat.format(
            java.time.LocalDate.of(2024, 2, 29),
            StringBuffer("prefix:"),
            javaPosition,
        )
        val kroguBuffer = kroguFormat.format(
            LocalDate.of(2024, 2, 29),
            StringBuffer("prefix:"),
            kroguPosition,
        )

        assertEquals(javaBuffer.toString(), kroguBuffer.toString())
        assertEquals(javaPosition.beginIndex, kroguPosition.beginIndex)
        assertEquals(javaPosition.endIndex, kroguPosition.endIndex)
        assertFailsWith<IllegalArgumentException> { kroguFormat.format("2024-02-29") }
    }

    @Test
    fun classicFormatFullyParsesAndReportsParseExceptionsLikeJavaTime() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat()
        val kroguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        val javaParsed = javaFormat.parseObject("2024-02-29") as java.time.temporal.TemporalAccessor
        val kroguParsed = kroguFormat.parseObject("2024-02-29") as
            io.heapy.krogu.time.temporal.TemporalAccessor
        assertEquals(
            java.time.LocalDate.from(javaParsed).toString(),
            LocalDate.from(kroguParsed).toString(),
        )

        listOf("2024-0x-29", "2023-02-29", "2024-02-29:suffix").forEach { text ->
            val javaFailure = assertFailsWith<ParseException> { javaFormat.parseObject(text) }
            val kroguFailure = assertFailsWith<ParseException> { kroguFormat.parseObject(text) }

            assertEquals(javaFailure.errorOffset, kroguFailure.errorOffset, text)
        }
    }

    @Test
    fun queriedClassicFormatReturnsTheRequestedType() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            java.time.temporal.TemporalQuery(java.time.LocalDate::from),
        )
        val kroguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            TemporalQuery(LocalDate::from),
        )

        assertIs<LocalDate>(kroguFormat.parseObject("2024-02-29"))
        assertEquals(
            javaFormat.parseObject("2024-02-29").toString(),
            kroguFormat.parseObject("2024-02-29").toString(),
        )
    }

    @Test
    fun positionedClassicParsingMatchesJavaTimeSuccessAndFailureState() {
        val cases = listOf(
            PositionedCase("prefix:2024-02-29:suffix", 7, -1),
            PositionedCase("prefix:2024-0x-29", 7, -1),
            PositionedCase("prefix:2023-02-29:suffix", 7, -1),
            PositionedCase("prefix:2024-02-29:suffix", 7, 3),
        )
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat()
        val kroguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        cases.forEach { case ->
            val javaPosition = ParsePosition(case.startIndex).apply {
                errorIndex = case.initialErrorIndex
            }
            val kroguPosition = ParsePosition(case.startIndex).apply {
                errorIndex = case.initialErrorIndex
            }

            val javaParsed = javaFormat.parseObject(case.text, javaPosition)
            val kroguParsed = kroguFormat.parseObject(case.text, kroguPosition)

            assertEquals(javaParsed == null, kroguParsed == null, case.text)
            if (javaParsed != null && kroguParsed != null) {
                assertEquals(
                    java.time.LocalDate.from(javaParsed as java.time.temporal.TemporalAccessor).toString(),
                    LocalDate.from(
                        kroguParsed as io.heapy.krogu.time.temporal.TemporalAccessor,
                    ).toString(),
                    case.text,
                )
            }
            assertEquals(javaPosition.index, kroguPosition.index, case.text)
            assertEquals(javaPosition.errorIndex, kroguPosition.errorIndex, case.text)
        }
    }

    @Test
    fun positionedQueryFailuresMatchJavaTime() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            java.time.temporal.TemporalQuery(java.time.LocalTime::from),
        )
        val kroguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            TemporalQuery(LocalTime::from),
        )
        val javaPosition = ParsePosition(7)
        val kroguPosition = ParsePosition(7)

        assertNull(javaFormat.parseObject("prefix:2024-02-29:suffix", javaPosition))
        assertNull(kroguFormat.parseObject("prefix:2024-02-29:suffix", kroguPosition))
        assertEquals(javaPosition.index, kroguPosition.index)
        assertEquals(javaPosition.errorIndex, kroguPosition.errorIndex)
    }

    private data class PositionedCase(
        val text: String,
        val startIndex: Int,
        val initialErrorIndex: Int,
    )
}
