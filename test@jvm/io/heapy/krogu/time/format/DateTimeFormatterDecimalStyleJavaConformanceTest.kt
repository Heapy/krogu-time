package io.heapy.krogu.time.format

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
        val kroguStyle = DecimalStyle.STANDARD
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF30')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')

        val javaDateFormatter =
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.withDecimalStyle(javaStyle)
        val kroguDateFormatter =
            DateTimeFormatter.ISO_LOCAL_DATE.withDecimalStyle(kroguStyle)
        val javaDate = java.time.LocalDate.of(-42, 2, 3)
        val kroguDate = io.heapy.krogu.time.LocalDate.of(-42, 2, 3)
        val localizedDate = javaDateFormatter.format(javaDate)
        assertEquals(localizedDate, kroguDateFormatter.format(kroguDate))
        assertEquals(
            java.time.LocalDate.from(javaDateFormatter.parse(localizedDate)).toString(),
            io.heapy.krogu.time.LocalDate.from(kroguDateFormatter.parse(localizedDate)).toString(),
        )

        val javaOffsetDateTime = java.time.OffsetDateTime.parse("2024-02-29T12:30:05.125+02:30")
        val kroguOffsetDateTime =
            io.heapy.krogu.time.OffsetDateTime.parse("2024-02-29T12:30:05.125+02:30")
        val javaOffsetFormatter =
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withDecimalStyle(javaStyle)
        val kroguOffsetFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withDecimalStyle(kroguStyle)
        val localizedOffsetDateTime = javaOffsetFormatter.format(javaOffsetDateTime)
        assertEquals(localizedOffsetDateTime, kroguOffsetFormatter.format(kroguOffsetDateTime))
        assertEquals(
            java.time.OffsetDateTime
                .from(javaOffsetFormatter.parse(localizedOffsetDateTime))
                .toString(),
            io.heapy.krogu.time.OffsetDateTime
                .from(kroguOffsetFormatter.parse(localizedOffsetDateTime))
                .toString(),
        )

        val javaInstant = java.time.Instant.parse("2024-02-29T12:30:05.125Z")
        val kroguInstant = io.heapy.krogu.time.Instant.parse("2024-02-29T12:30:05.125Z")
        assertEquals(
            java.time.format.DateTimeFormatter.ISO_INSTANT
                .withDecimalStyle(javaStyle)
                .format(javaInstant),
            DateTimeFormatter.ISO_INSTANT
                .withDecimalStyle(kroguStyle)
                .format(kroguInstant),
        )
    }
}
