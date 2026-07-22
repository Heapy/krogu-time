package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Period
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DateTimeFormatterBuilderCompositionTest {
    @Test
    fun appendsAFormatterBetweenOtherBuilderElements() {
        val builder = DateTimeFormatterBuilder().appendLiteral('<')
        assertSame(builder, builder.append(DateTimeFormatter.ISO_LOCAL_DATE))
        val formatter = builder.appendLiteral('>').toFormatter()
        val date = LocalDate.of(2024, 2, 29)

        assertEquals("<2024-02-29>", formatter.format(date))
        assertEquals(date, LocalDate.from(formatter.parse("<2024-02-29>")))
    }

    @Test
    fun optionallyAppendsEveryElementOfAFormatter() {
        val time = DateTimeFormatter.ofPattern("'T'HH:mm:ss")
        val builder = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
        assertSame(builder, builder.appendOptional(time))
        val formatter = builder.toFormatter()
        val date = LocalDate.of(2024, 2, 29)
        val dateTime = LocalDateTime.of(date, LocalTime.of(12, 30, 5))

        assertEquals("2024-02-29", formatter.format(date))
        assertEquals("2024-02-29T12:30:05", formatter.format(dateTime))
        assertEquals(date, LocalDate.from(formatter.parse("2024-02-29")))
        assertEquals(
            dateTime,
            LocalDateTime.from(formatter.parse("2024-02-29T12:30:05")),
        )
    }

    @Test
    fun outerResolverStyleControlsAppendedFormattersAndCarriesExcessDays() {
        val formatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.STRICT))
            .toFormatter()
        val parsed = formatter.parse("2024-02-29T24:00")

        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), LocalDateTime.from(parsed))
        assertEquals(Period.ZERO, parsed.query(DateTimeFormatter.parsedExcessDays()))
        assertFailsWith<DateTimeParseException> {
            formatter.withResolverStyle(ResolverStyle.STRICT).parse("2024-02-29T24:00")
        }

        val timeOnly = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter()
            .parse("24:00")
        assertEquals(LocalTime.MIDNIGHT, LocalTime.from(timeOnly))
        assertEquals(Period.ofDays(1), timeOnly.query(DateTimeFormatter.parsedExcessDays()))
    }

    @Test
    fun retainsZonesOffsetsInstantsAndLeapSecondState() {
        val zonedFormatter = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendLiteral('>')
            .toFormatter()
        val zoned = ZonedDateTime.parse("2024-03-31T01:30:00+01:00[Europe/Paris]")
        val zonedText = "<2024-03-31T01:30:00+01:00[Europe/Paris]>"

        assertEquals(zonedText, zonedFormatter.format(zoned))
        assertEquals(zoned, ZonedDateTime.from(zonedFormatter.parse(zonedText)))

        val instantFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_INSTANT)
            .toFormatter()
        val leap = instantFormatter.parse("2016-12-31t23:59:60z")
        assertEquals(Instant.parse("2016-12-31T23:59:60Z"), Instant.from(leap))
        assertTrue(leap.query(DateTimeFormatter.parsedLeapSecond()))
        assertFalse(
            instantFormatter.parse("2016-12-31T23:59:59Z")
                .query(DateTimeFormatter.parsedLeapSecond()),
        )
    }

    @Test
    fun paddingAndOptionalRollbackTreatTheFormatterAsOneElement() {
        val padded = DateTimeFormatterBuilder()
            .padNext(12, '_')
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .toFormatter()
        assertEquals("__2024-02-29", padded.format(LocalDate.of(2024, 2, 29)))
        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.from(padded.parse("__2024-02-29")),
        )

        val optionalTime = DateTimeFormatter.ofPattern("'T'HH:mm")
        val optional = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(optionalTime)
            .appendLiteral('!')
            .toFormatter()
        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.from(optional.parse("2024-02-29!")),
        )
        assertFailsWith<DateTimeParseException> { optional.parse("2024-02-29Txx!") }
    }

    @Test
    fun detectsConflictsWithFieldsParsedByAnAppendedFormatter() {
        val formatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter()

        assertEquals(
            LocalDate.of(2024, 2, 29),
            LocalDate.from(formatter.parse("2024-02-29/2024")),
        )
        assertFailsWith<DateTimeParseException> { formatter.parse("2024-02-29/2025") }
    }

    @Test
    fun appendedTokensParticipateInDefaultsAndSequentialParserSettings() {
        val defaultedSecond = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 45)
            .toFormatter()
        assertEquals(
            LocalTime.of(12, 30, 45),
            LocalTime.from(defaultedSecond.parse("12:30")),
        )

        val insensitivePrefix = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("Ab")
            .toFormatter()
        val sharedSettings = DateTimeFormatterBuilder()
            .append(insensitivePrefix)
            .appendLiteral("Cd")
            .toFormatter()
        sharedSettings.parse("abcd")
    }

    @Test
    fun ignoresOverridesStoredOnTheAppendedFormatter() {
        val overridden = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(ThaiBuddhistChronology)
            .withZone(ZoneId.of("Europe/Paris"))
            .withResolverStyle(ResolverStyle.LENIENT)
        val formatter = DateTimeFormatterBuilder().append(overridden).toFormatter()

        assertEquals("2024-02-29", formatter.format(LocalDate.of(2024, 2, 29)))
        assertFailsWith<DateTimeParseException> {
            formatter.withResolverStyle(ResolverStyle.STRICT).parse("2024-02-30")
        }
    }
}
