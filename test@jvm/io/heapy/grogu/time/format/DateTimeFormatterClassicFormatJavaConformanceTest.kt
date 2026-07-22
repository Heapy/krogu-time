package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.temporal.TemporalQuery
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
        val groguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        assertEquals(
            javaFormat.format(java.time.LocalDate.of(2024, 2, 29)),
            groguFormat.format(LocalDate.of(2024, 2, 29)),
        )

        val javaPosition = FieldPosition(0)
        val groguPosition = FieldPosition(0)
        val javaBuffer = javaFormat.format(
            java.time.LocalDate.of(2024, 2, 29),
            StringBuffer("prefix:"),
            javaPosition,
        )
        val groguBuffer = groguFormat.format(
            LocalDate.of(2024, 2, 29),
            StringBuffer("prefix:"),
            groguPosition,
        )

        assertEquals(javaBuffer.toString(), groguBuffer.toString())
        assertEquals(javaPosition.beginIndex, groguPosition.beginIndex)
        assertEquals(javaPosition.endIndex, groguPosition.endIndex)
        assertFailsWith<IllegalArgumentException> { groguFormat.format("2024-02-29") }
    }

    @Test
    fun classicFormatFullyParsesAndReportsParseExceptionsLikeJavaTime() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat()
        val groguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        val javaParsed = javaFormat.parseObject("2024-02-29") as java.time.temporal.TemporalAccessor
        val groguParsed = groguFormat.parseObject("2024-02-29") as
            io.heapy.grogu.time.temporal.TemporalAccessor
        assertEquals(
            java.time.LocalDate.from(javaParsed).toString(),
            LocalDate.from(groguParsed).toString(),
        )

        listOf("2024-0x-29", "2023-02-29", "2024-02-29:suffix").forEach { text ->
            val javaFailure = assertFailsWith<ParseException> { javaFormat.parseObject(text) }
            val groguFailure = assertFailsWith<ParseException> { groguFormat.parseObject(text) }

            assertEquals(javaFailure.errorOffset, groguFailure.errorOffset, text)
        }
    }

    @Test
    fun queriedClassicFormatReturnsTheRequestedType() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            java.time.temporal.TemporalQuery(java.time.LocalDate::from),
        )
        val groguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            TemporalQuery(LocalDate::from),
        )

        assertIs<LocalDate>(groguFormat.parseObject("2024-02-29"))
        assertEquals(
            javaFormat.parseObject("2024-02-29").toString(),
            groguFormat.parseObject("2024-02-29").toString(),
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
        val groguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat()

        cases.forEach { case ->
            val javaPosition = ParsePosition(case.startIndex).apply {
                errorIndex = case.initialErrorIndex
            }
            val groguPosition = ParsePosition(case.startIndex).apply {
                errorIndex = case.initialErrorIndex
            }

            val javaParsed = javaFormat.parseObject(case.text, javaPosition)
            val groguParsed = groguFormat.parseObject(case.text, groguPosition)

            assertEquals(javaParsed == null, groguParsed == null, case.text)
            if (javaParsed != null && groguParsed != null) {
                assertEquals(
                    java.time.LocalDate.from(javaParsed as java.time.temporal.TemporalAccessor).toString(),
                    LocalDate.from(
                        groguParsed as io.heapy.grogu.time.temporal.TemporalAccessor,
                    ).toString(),
                    case.text,
                )
            }
            assertEquals(javaPosition.index, groguPosition.index, case.text)
            assertEquals(javaPosition.errorIndex, groguPosition.errorIndex, case.text)
        }
    }

    @Test
    fun positionedQueryFailuresMatchJavaTime() {
        val javaFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            java.time.temporal.TemporalQuery(java.time.LocalTime::from),
        )
        val groguFormat = DateTimeFormatter.ISO_LOCAL_DATE.toFormat(
            TemporalQuery(LocalTime::from),
        )
        val javaPosition = ParsePosition(7)
        val groguPosition = ParsePosition(7)

        assertNull(javaFormat.parseObject("prefix:2024-02-29:suffix", javaPosition))
        assertNull(groguFormat.parseObject("prefix:2024-02-29:suffix", groguPosition))
        assertEquals(javaPosition.index, groguPosition.index)
        assertEquals(javaPosition.errorIndex, groguPosition.errorIndex)
    }

    private data class PositionedCase(
        val text: String,
        val startIndex: Int,
        val initialErrorIndex: Int,
    )
}
