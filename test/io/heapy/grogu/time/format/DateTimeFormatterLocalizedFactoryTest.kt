package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.IsoChronology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterLocalizedFactoryTest {
    @Test
    fun localizedFactoriesExposeJavaCompatibleConfiguration() {
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM)

        listOf(date, time, dateTime).forEach { formatter ->
            assertEquals(ResolverStyle.SMART, formatter.resolverStyle)
            assertSame(IsoChronology, formatter.chronology)
        }
        assertEquals("Localized(FULL,)", date.toString())
        assertEquals("Localized(,SHORT)", time.toString())
        assertEquals("Localized(LONG,MEDIUM)", dateTime.toString())
        assertEquals(
            "Localized(MEDIUM,MEDIUM)",
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString(),
        )
    }

    @Test
    fun localizedDateFactoriesFormatParseAndChangeLocaleLazily() {
        val american = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.US)
        val british = american.withLocale(Locale.UK)
        val date = LocalDate.of(2024, 2, 29)

        assertEquals("2/29/24", american.format(date))
        assertEquals("29/02/2024", british.format(date))
        assertEquals(date, LocalDate.from(american.parse(american.format(date))))
        assertEquals(date, LocalDate.from(british.parse(british.format(date))))
    }

    @Test
    fun builderComposesLocalizedDateAndTimeSections() {
        val dateTime = LocalDateTime.of(LocalDate.of(2024, 2, 29), LocalTime.of(15, 7, 9))
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.US)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.US)
        val formatter = DateTimeFormatterBuilder()
            .appendLocalized(FormatStyle.MEDIUM, null)
            .appendLiteral(" | ")
            .appendLocalized(null, FormatStyle.MEDIUM)
            .toFormatter(Locale.US)

        val text = date.format(dateTime) + " | " + time.format(dateTime)
        assertEquals(text, formatter.format(dateTime))
        assertEquals(dateTime, LocalDateTime.from(formatter.parse(text)))
        assertEquals("Localized(MEDIUM,)' | 'Localized(,MEDIUM)", formatter.toString())
    }

    @Test
    fun fullLocalizedTimeSupportsZoneText() {
        val time = LocalTime.of(15, 7, 9)
        val zoned = ZonedDateTime.of(LocalDate.of(2024, 7, 1), time, ZoneId.of("America/New_York"))
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(Locale.US)
        val text = formatter.format(zoned)

        assertEquals(time, LocalTime.from(formatter.parse(text)))
        assertEquals("America/New_York", ZoneId.from(formatter.parse(text)).id)
    }

    @Test
    fun builderRequiresAtLeastOneLocalizedStyle() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatterBuilder().appendLocalized(null, null)
        }
    }
}
