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

class OffsetDateTimeTest {
    @Test
    fun createsExposesAndConvertsComponents() {
        val offset = ZoneOffset.ofHoursMinutes(5, 30)
        val value = OffsetDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789, offset)
        assertEquals(LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789), value.dateTime)
        assertEquals(LocalDate.of(2024, 2, 29), value.date)
        assertEquals(LocalTime.of(13, 14, 15, 123_456_789), value.time)
        assertEquals(offset, value.offset)
        assertEquals(2024, value.year)
        assertEquals(Month.FEBRUARY, value.month)
        assertEquals(29, value.dayOfMonth)
        assertEquals(13, value.hour)
        assertEquals(123_456_789, value.nano)
        assertEquals(value, value.dateTime.atOffset(offset))
        assertEquals(value.toInstant(), Instant.ofEpochSecond(value.toEpochSecond(), value.nano.toLong()))
        assertEquals(value, value.toInstant().atOffset(offset))
        assertEquals(OffsetTime.of(value.time, offset), value.toOffsetTime())
        assertEquals(OffsetDateTime.of(LocalDateTime.MIN, ZoneOffset.MAX), OffsetDateTime.MIN)
        assertEquals(OffsetDateTime.of(LocalDateTime.MAX, ZoneOffset.MIN), OffsetDateTime.MAX)
    }

    @Test
    fun createsLocalDateTimesFromEpochSecondsAndOffsets() {
        val offset = ZoneOffset.ofHoursMinutes(5, 30)
        val local = LocalDateTime.ofEpochSecond(0, 123_456_789, offset)
        assertEquals(LocalDateTime.of(1970, 1, 1, 5, 30, 0, 123_456_789), local)
        assertEquals(0L, local.toEpochSecond(offset))
        assertEquals(Instant.ofEpochSecond(0, 123_456_789), local.toInstant(offset))
        assertFailsWith<DateTimeException> {
            LocalDateTime.ofEpochSecond(Instant.MAX.epochSecond, 0, ZoneOffset.UTC)
        }
    }

    @Test
    fun parsesFormatsAndConvertsFromTemporals() {
        val expected = OffsetDateTime.of(
            2024,
            2,
            29,
            13,
            14,
            15,
            123_400_000,
            ZoneOffset.ofHoursMinutes(5, 30),
        )
        assertEquals(expected, OffsetDateTime.parse("2024-02-29T13:14:15.1234+05:30"))
        assertEquals("2024-02-29T13:14:15.123400+05:30", expected.toString())
        assertEquals(expected, OffsetDateTime.parse(expected.toString()))
        assertEquals(
            OffsetDateTime.of(2024, 2, 29, 13, 14, 0, 0, ZoneOffset.ofHours(5)),
            OffsetDateTime.parse("2024-02-29T13:14+05"),
        )
        assertSame(expected, OffsetDateTime.from(expected))
        assertFailsWith<io.heapy.grogu.time.format.DateTimeParseException> {
            OffsetDateTime.parse("2024-02-29T13:14:15")
        }
    }

    @Test
    fun exposesAllStandardFieldsAndUnitsExceptForever() {
        val value = OffsetDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789, ZoneOffset.ofHours(2))
        ChronoField.entries.forEach { field -> assertTrue(value.isSupported(field), field.toString()) }
        assertEquals(value.toEpochSecond(), value.getLong(ChronoField.INSTANT_SECONDS))
        assertEquals(7_200L, value.getLong(ChronoField.OFFSET_SECONDS))
        assertEquals(2024L, value.getLong(ChronoField.YEAR))
        assertSame(value.offset, value.query(TemporalQueries.offset()))
        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit !== ChronoUnit.FOREVER, value.isSupported(unit), unit.toString())
        }
        assertFailsWith<UnsupportedTemporalTypeException> { value.get(ChronoField.INSTANT_SECONDS) }
    }

    @Test
    fun changesOffsetsFieldsAndComponents() {
        val original = OffsetDateTime.of(2024, 2, 29, 23, 30, 0, 123, ZoneOffset.ofHours(1))
        assertEquals(
            OffsetDateTime.of(original.dateTime, ZoneOffset.ofHours(2)),
            original.withOffsetSameLocal(ZoneOffset.ofHours(2)),
        )
        assertEquals(
            OffsetDateTime.of(original.dateTime.plusHours(1), ZoneOffset.ofHours(2)),
            original.withOffsetSameInstant(ZoneOffset.ofHours(2)),
        )
        assertEquals(2023, original.withYear(2023).year)
        assertEquals(1, original.withMonth(1).monthValue)
        assertEquals(2, original.withDayOfMonth(2).dayOfMonth)
        assertEquals(2, original.withHour(2).hour)
        assertEquals(ZoneOffset.UTC, original.with(ChronoField.OFFSET_SECONDS, 0).offset)
        assertEquals(
            original.toInstant().plusSeconds(5),
            original.with(ChronoField.INSTANT_SECONDS, original.toEpochSecond() + 5).toInstant(),
        )
        assertEquals(original.truncatedTo(ChronoUnit.HOURS).minute, 0)
    }

    @Test
    fun addsMeasuresAndComparesOnLocalAndInstantTimelines() {
        val start = OffsetDateTime.of(2024, 2, 29, 23, 30, 0, 0, ZoneOffset.ofHours(1))
        assertEquals(2024, start.plusDays(1).year)
        assertEquals(3, start.plusDays(1).monthValue)
        assertEquals(1, start.plusHours(1).dayOfMonth)
        assertEquals(start, start.plus(Duration.ofSeconds(2)).minus(Duration.ofSeconds(2)))
        assertEquals(start, start.minusNanos(Long.MIN_VALUE).plusNanos(Long.MIN_VALUE))

        val sameInstant = start.withOffsetSameInstant(ZoneOffset.ofHours(2))
        val oneNanoLater = sameInstant.plusNanos(1)
        assertTrue(start.isEqual(sameInstant))
        assertTrue(start < sameInstant)
        assertTrue(oneNanoLater.isAfter(start))
        assertTrue(start.isBefore(oneNanoLater))
        assertEquals(0L, start.until(sameInstant, ChronoUnit.NANOS))
        assertEquals(1L, start.until(oneNanoLater, ChronoUnit.NANOS))
        assertEquals(0, OffsetDateTime.timeLineOrder().compare(start, sameInstant))
    }
}
