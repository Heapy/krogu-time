package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OffsetTimeTest {
    @Test
    fun createsAndExposesLocalTimeAndOffset() {
        val offset = ZoneOffset.ofHoursMinutes(5, 30)
        val time = OffsetTime.of(13, 14, 15, 123_456_789, offset)
        assertEquals(LocalTime.of(13, 14, 15, 123_456_789), time.time)
        assertEquals(offset, time.offset)
        assertEquals(13, time.hour)
        assertEquals(14, time.minute)
        assertEquals(15, time.second)
        assertEquals(123_456_789, time.nano)
        assertEquals(time, OffsetTime.of(time.time, offset))
        assertEquals(time, time.time.atOffset(offset))
        assertEquals(OffsetTime.of(LocalTime.MIN, ZoneOffset.MAX), OffsetTime.MIN)
        assertEquals(OffsetTime.of(LocalTime.MAX, ZoneOffset.MIN), OffsetTime.MAX)
    }

    @Test
    fun parsesAndFormatsIsoOffsetTimes() {
        val cases = mapOf(
            "00:00Z" to OffsetTime.of(LocalTime.MIN, ZoneOffset.UTC),
            "13:14:15+05:30" to OffsetTime.of(13, 14, 15, 0, ZoneOffset.ofHoursMinutes(5, 30)),
            "23:59:59.123456789-01:30:15" to OffsetTime.of(
                23,
                59,
                59,
                123_456_789,
                ZoneOffset.ofHoursMinutesSeconds(-1, -30, -15),
            ),
        )
        cases.forEach { (text, expected) -> assertEquals(expected, OffsetTime.parse(text), text) }
        cases.values.forEach { assertEquals(it, OffsetTime.parse(it.toString())) }
        assertFailsWith<io.heapy.grogu.time.format.DateTimeParseException> {
            OffsetTime.parse("13:14:15")
        }
    }

    @Test
    fun exposesTimeAndOffsetFieldsAndTimeUnits() {
        val time = OffsetTime.of(13, 14, 15, 123_456_789, ZoneOffset.ofHours(2))
        ChronoField.entries.forEach { field ->
            val expectedSupport = field.isTimeBased || field === ChronoField.OFFSET_SECONDS
            assertEquals(expectedSupport, time.isSupported(field), field.toString())
        }
        assertEquals(7_200L, time.getLong(ChronoField.OFFSET_SECONDS))
        assertEquals(13L, time.getLong(ChronoField.HOUR_OF_DAY))
        assertSame(time.offset, time.query(TemporalQueries.offset()))
        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit.isTimeBased, time.isSupported(unit), unit.toString())
        }
        assertFailsWith<UnsupportedTemporalTypeException> { time.getLong(ChronoField.EPOCH_DAY) }
    }

    @Test
    fun changesOffsetsWithOrWithoutPreservingTheInstant() {
        val original = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(1))
        assertEquals(
            OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(2)),
            original.withOffsetSameLocal(ZoneOffset.ofHours(2)),
        )
        assertEquals(
            OffsetTime.of(13, 0, 0, 0, ZoneOffset.ofHours(2)),
            original.withOffsetSameInstant(ZoneOffset.ofHours(2)),
        )
        assertSame(original, original.withOffsetSameInstant(original.offset))
    }

    @Test
    fun replacesTruncatesAddsAndSubtractsWhileRetainingOffset() {
        val original = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(-3))
        assertEquals(OffsetTime.of(1, 59, 59, 999_999_999, original.offset), original.withHour(1))
        assertEquals(ZoneOffset.UTC, original.with(ChronoField.OFFSET_SECONDS, 0).offset)
        assertEquals(
            OffsetTime.of(23, 59, 0, 0, original.offset),
            original.truncatedTo(ChronoUnit.MINUTES),
        )
        assertEquals(OffsetTime.of(LocalTime.MIN, original.offset), original.plusNanos(1))
        assertEquals(original.plusHours(2), original.plus(2, ChronoUnit.HOURS))
        assertEquals(original, original.plus(Duration.ofSeconds(2, 3)).minus(Duration.ofSeconds(2, 3)))
        assertEquals(original, original.minusNanos(Long.MIN_VALUE).plusNanos(Long.MIN_VALUE))
        assertFailsWith<UnsupportedTemporalTypeException> { original.plus(1, ChronoUnit.DAYS) }
    }

    @Test
    fun comparesAndMeasuresOnTheOffsetTimeline() {
        val noonUtc = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC)
        val sameInstant = OffsetTime.of(13, 0, 0, 0, ZoneOffset.ofHours(1))
        val later = OffsetTime.of(13, 0, 0, 1, ZoneOffset.ofHours(1))
        assertTrue(noonUtc.isEqual(sameInstant))
        assertTrue(noonUtc < sameInstant)
        assertTrue(later.isAfter(noonUtc))
        assertTrue(noonUtc.isBefore(later))
        assertEquals(1L, noonUtc.until(later, ChronoUnit.NANOS))
        assertEquals(0L, noonUtc.until(later, ChronoUnit.SECONDS))
        assertEquals(
            LocalDate.of(2024, 2, 29).toEpochDay() * 86_400 + 12 * 3_600L,
            noonUtc.toEpochSecond(LocalDate.of(2024, 2, 29)),
        )
    }
}
