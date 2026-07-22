package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.YearMonth
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.temporal.TemporalQueries
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
}
