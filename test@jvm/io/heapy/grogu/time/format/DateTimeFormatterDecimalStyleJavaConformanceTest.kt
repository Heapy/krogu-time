package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterDecimalStyleJavaConformanceTest {
    @Test
    fun formattingAndParsingWithCustomSymbolsMatchesJavaTime() {
        val javaStyle = java.time.format.DecimalStyle.STANDARD
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF30')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')
        val groguStyle = DecimalStyle.STANDARD
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF30')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')

        val javaDateFormatter =
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.withDecimalStyle(javaStyle)
        val groguDateFormatter =
            DateTimeFormatter.ISO_LOCAL_DATE.withDecimalStyle(groguStyle)
        val javaDate = java.time.LocalDate.of(-42, 2, 3)
        val groguDate = io.heapy.grogu.time.LocalDate.of(-42, 2, 3)
        val localizedDate = javaDateFormatter.format(javaDate)
        assertEquals(localizedDate, groguDateFormatter.format(groguDate))
        assertEquals(
            java.time.LocalDate.from(javaDateFormatter.parse(localizedDate)).toString(),
            io.heapy.grogu.time.LocalDate.from(groguDateFormatter.parse(localizedDate)).toString(),
        )

        val javaOffsetDateTime = java.time.OffsetDateTime.parse("2024-02-29T12:30:05.125+02:30")
        val groguOffsetDateTime =
            io.heapy.grogu.time.OffsetDateTime.parse("2024-02-29T12:30:05.125+02:30")
        val javaOffsetFormatter =
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withDecimalStyle(javaStyle)
        val groguOffsetFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withDecimalStyle(groguStyle)
        val localizedOffsetDateTime = javaOffsetFormatter.format(javaOffsetDateTime)
        assertEquals(localizedOffsetDateTime, groguOffsetFormatter.format(groguOffsetDateTime))
        assertEquals(
            java.time.OffsetDateTime
                .from(javaOffsetFormatter.parse(localizedOffsetDateTime))
                .toString(),
            io.heapy.grogu.time.OffsetDateTime
                .from(groguOffsetFormatter.parse(localizedOffsetDateTime))
                .toString(),
        )

        val javaInstant = java.time.Instant.parse("2024-02-29T12:30:05.125Z")
        val groguInstant = io.heapy.grogu.time.Instant.parse("2024-02-29T12:30:05.125Z")
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_INSTANT
                .withDecimalStyle(javaStyle)
                .format(javaInstant),
            DateTimeFormatter.ISO_INSTANT
                .withDecimalStyle(groguStyle)
                .format(groguInstant),
        )
    }
}
