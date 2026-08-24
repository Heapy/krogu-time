package io.heapy.krogu.time.format

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.YearMonth
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.chrono.HijrahChronology
import io.heapy.krogu.time.chrono.HijrahDate
import io.heapy.krogu.time.chrono.IsoChronology
import io.heapy.krogu.time.chrono.ThaiBuddhistChronology
import io.heapy.krogu.time.chrono.ThaiBuddhistDate
import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class DateTimeFormatterChronologyOverrideTest {
    @Test
    fun exposesPredefinedChronologiesAndCopiesOverridesImmutably() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        assertSame(IsoChronology, formatter.chronology)
        assertSame(formatter, formatter.withChronology(IsoChronology))

        val thai = formatter.withChronology(ThaiBuddhistChronology)
        assertNotSame(formatter, thai)
        assertSame(ThaiBuddhistChronology, thai.chronology)
        assertSame(thai, thai.withChronology(ThaiBuddhistChronology))
        assertSame(IsoChronology, formatter.chronology)
        assertEquals(formatter.toString(), thai.toString())

        val withoutOverride = formatter.withChronology(null)
        assertNull(withoutOverride.chronology)
        assertSame(withoutOverride, withoutOverride.withChronology(null))
        assertNull(DateTimeFormatter.ISO_LOCAL_TIME.chronology)
        assertNull(DateTimeFormatter.ISO_INSTANT.chronology)
        assertSame(IsoChronology, DateTimeFormatter.RFC_1123_DATE_TIME.chronology)
    }

    @Test
    fun convertsWholeDatesBeforeFormatting() {
        val isoDate = LocalDate.of(2024, 3, 1)
        val thaiDate = ThaiBuddhistDate.of(2567, 3, 1)

        assertEquals(
            "2567-03-01",
            DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(ThaiBuddhistChronology)
                .format(isoDate),
        )
        assertEquals(
            "2024-03-01",
            DateTimeFormatter.ISO_LOCAL_DATE.format(thaiDate),
        )
        assertEquals(
            "2567-03-01",
            DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(null)
                .format(thaiDate),
        )
        assertEquals(
            "2567-03-01T00:30:00",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withChronology(ThaiBuddhistChronology)
                .withZone(ZoneOffset.UTC)
                .format(Instant.parse("2024-03-01T00:30:00Z")),
        )
    }

    @Test
    fun rejectsChronologyConversionForPartialDates() {
        assertFailsWith<DateTimeException> {
            DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(ThaiBuddhistChronology)
                .format(YearMonth.of(2024, 3))
        }
    }

    @Test
    fun resolvesParsedDatesInTheOverrideChronology() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(ThaiBuddhistChronology)
        val parsed = formatter.parse("2567-03-01")

        assertSame(ThaiBuddhistChronology, parsed.query(TemporalQueries.chronology()))
        assertEquals(ThaiBuddhistDate.of(2567, 3, 1), ThaiBuddhistDate.from(parsed))
        assertEquals(LocalDate.of(2024, 3, 1), LocalDate.from(parsed))

        val parsedTime = DateTimeFormatter.ISO_LOCAL_TIME
            .withChronology(ThaiBuddhistChronology)
            .parse("12:30")
        assertSame(ThaiBuddhistChronology, parsedTime.query(TemporalQueries.chronology()))
        assertSame(
            IsoChronology,
            DateTimeFormatter.ISO_LOCAL_TIME
                .parse("12:30")
                .query(TemporalQueries.chronology()),
        )
    }

    @Test
    fun resolvesNonIsoCalendarDatesWithTheConfiguredStyle() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(HijrahChronology)

        assertFailsWith<DateTimeParseException> {
            formatter.parse("1445-08-30")
        }
        assertEquals(
            HijrahDate.of(1445, 8, 29),
            HijrahDate.from(
                formatter
                    .withResolverStyle(ResolverStyle.SMART)
                    .parse("1445-08-30"),
            ),
        )
        assertEquals(
            HijrahDate.of(1445, 9, 1),
            HijrahDate.from(
                formatter
                    .withResolverStyle(ResolverStyle.LENIENT)
                    .parse("1445-08-30"),
            ),
        )
    }

    @Test
    fun resolvesAnOverriddenChronologyFromParsedInstantsAndZones() {
        val parsed = DateTimeFormatter.ISO_INSTANT
            .withChronology(ThaiBuddhistChronology)
            .withZone(ZoneOffset.UTC)
            .parse("2024-03-01T00:30:00Z")

        assertSame(ThaiBuddhistChronology, parsed.query(TemporalQueries.chronology()))
        assertEquals(ThaiBuddhistDate.of(2567, 3, 1), ThaiBuddhistDate.from(parsed))
    }
}
