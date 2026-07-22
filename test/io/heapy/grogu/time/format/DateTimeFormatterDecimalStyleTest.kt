package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.temporal.TemporalQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DateTimeFormatterDecimalStyleTest {
    private val localizedStyle = DecimalStyle.STANDARD
        .withZeroDigit('\u0660')
        .withPositiveSign('\uFF30')
        .withNegativeSign('\u2212')
        .withDecimalSeparator('\u066B')

    @Test
    fun exposesAndCopiesDecimalStyleImmutably() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        assertSame(DecimalStyle.STANDARD, formatter.decimalStyle)
        assertSame(formatter, formatter.withDecimalStyle(DecimalStyle.STANDARD))

        val localized = formatter.withDecimalStyle(localizedStyle)
        assertNotSame(formatter, localized)
        assertEquals(localizedStyle, localized.decimalStyle)
        assertSame(DecimalStyle.STANDARD, formatter.decimalStyle)
        assertEquals(formatter.toString(), localized.toString())
    }

    @Test
    fun localizesNumericFieldsSignsAndFractionSeparators() {
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withDecimalStyle(localizedStyle)
        val localizedDate = "\u2212\u0660\u0660\u0664\u0662-\u0660\u0662-\u0660\u0663"
        assertEquals(localizedDate, dateFormatter.format(LocalDate.of(-42, 2, 3)))
        assertEquals(
            LocalDate.of(-42, 2, 3),
            dateFormatter.parse(localizedDate, TemporalQuery(LocalDate::from)),
        )

        val dateTime = OffsetDateTime.parse("2024-02-29T12:30:05.125+02:30")
        val dateTimeFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withDecimalStyle(localizedStyle)
        val localizedDateTime =
            "\u0662\u0660\u0662\u0664-\u0660\u0662-\u0662\u0669" +
                "T\u0661\u0662:\u0663\u0660:\u0660\u0665\u066B\u0661\u0662\u0665+02:30"
        assertEquals(localizedDateTime, dateTimeFormatter.format(dateTime))
        assertEquals(
            dateTime,
            dateTimeFormatter.parse(localizedDateTime, TemporalQuery(OffsetDateTime::from)),
        )
    }

    @Test
    fun keepsInstantAndOffsetSyntaxAscii() {
        val instant = Instant.parse("2024-02-29T12:30:05.125Z")
        assertEquals(
            "2024-02-29T12:30:05.125Z",
            DateTimeFormatter.ISO_INSTANT
                .withDecimalStyle(localizedStyle)
                .format(instant),
        )

        val rfcDateTime = OffsetDateTime.parse("2024-02-29T12:30:05+02:30")
        val rfcFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withDecimalStyle(localizedStyle)
        val localizedRfc =
            "Thu, \u0662\u0669 Feb \u0662\u0660\u0662\u0664 " +
                "\u0661\u0662:\u0663\u0660:\u0660\u0665 +0230"
        assertEquals(localizedRfc, rfcFormatter.format(rfcDateTime))
        assertEquals(
            rfcDateTime,
            rfcFormatter.parse(localizedRfc, TemporalQuery(OffsetDateTime::from)),
        )
    }
}
