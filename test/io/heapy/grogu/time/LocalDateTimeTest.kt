package io.heapy.grogu.time

import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocalDateTimeTest {
    @Test
    fun createsAndExposesDateAndTimeComponents() {
        val dateTime = LocalDateTime.of(2024, Month.FEBRUARY, 29, 13, 14, 15, 123_456_789)
        assertEquals(2024, dateTime.year)
        assertEquals(Month.FEBRUARY, dateTime.month)
        assertEquals(2, dateTime.monthValue)
        assertEquals(29, dateTime.dayOfMonth)
        assertEquals(60, dateTime.dayOfYear)
        assertEquals(DayOfWeek.THURSDAY, dateTime.dayOfWeek)
        assertEquals(13, dateTime.hour)
        assertEquals(14, dateTime.minute)
        assertEquals(15, dateTime.second)
        assertEquals(123_456_789, dateTime.nano)
        assertEquals(LocalDate.of(2024, 2, 29), dateTime.toLocalDate())
        assertEquals(LocalTime.of(13, 14, 15, 123_456_789), dateTime.toLocalTime())

        assertEquals(
            dateTime,
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
        )
        assertEquals(dateTime, LocalDateTime.of(dateTime.date, dateTime.time))
        assertEquals(
            LocalDateTime.of(2024, 2, 29, 13, 14),
            LocalDateTime.of(2024, Month.FEBRUARY, 29, 13, 14),
        )
        assertEquals(
            LocalDateTime.of(2024, 2, 29, 13, 14, 15),
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 0),
        )
        assertFailsWith<DateTimeException> { LocalDateTime.of(2023, 2, 29, 0, 0) }
        assertFailsWith<DateTimeException> { LocalDateTime.of(2024, 2, 29, 24, 0) }
    }

    @Test
    fun parsesStrictIsoLocalDateTimes() {
        val cases = mapOf(
            "2024-02-29T00:00" to LocalDateTime.of(2024, 2, 29, 0, 0),
            "2024-02-29T13:14:15" to LocalDateTime.of(2024, 2, 29, 13, 14, 15),
            "2024-02-29T13:14:15." to LocalDateTime.of(2024, 2, 29, 13, 14, 15),
            "2024-02-29T13:14:15.123456789" to
                LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
            "2024-02-29t13:14" to LocalDateTime.of(2024, 2, 29, 13, 14),
            "-0001-01-01T01:02" to LocalDateTime.of(-1, 1, 1, 1, 2),
            "+10000-01-01T23:59" to LocalDateTime.of(10_000, 1, 1, 23, 59),
        )
        cases.forEach { (text, expected) ->
            assertEquals(expected, LocalDateTime.parse(text), text)
        }

        val invalidInputs = mapOf(
            "" to 0,
            "2024-02-29" to 10,
            "2024-02-29T" to 11,
            "2024-02-29 01:02" to 10,
            "2024-02-29T1:02" to 11,
            "2024-02-29T01:2" to 14,
            "2024-02-29T01:02Z" to 16,
            "2023-02-29T01:02" to 0,
            "2024-02-29T24:00" to 0,
            "２０２４-０２-２９T０１:０２" to 0,
        )
        invalidInputs.forEach { (input, expectedIndex) ->
            val error = assertFailsWith<DateTimeParseException>(input) {
                LocalDateTime.parse(input)
            }
            assertEquals(input, error.parsedString)
            assertEquals(expectedIndex, error.errorIndex, input)
        }
    }

    @Test
    fun exposesAllLocalDateAndLocalTimeFields() {
        val dateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        ChronoField.entries.forEach { field ->
            val component = if (field.isTimeBased) dateTime.time else dateTime.date
            val expectedSupport = field.isDateBased || field.isTimeBased
            assertEquals(expectedSupport, dateTime.isSupported(field), field.toString())
            if (expectedSupport) {
                assertEquals(component.range(field), dateTime.range(field), field.toString())
                assertEquals(component.getLong(field), dateTime.getLong(field), field.toString())
            }
        }
        assertFailsWith<UnsupportedTemporalTypeException> {
            dateTime.getLong(ChronoField.INSTANT_SECONDS)
        }
        assertEquals(2024, dateTime.get(ChronoField.YEAR))
        assertEquals(123_456_789, dateTime.get(ChronoField.NANO_OF_SECOND))
        val nanoOfDayException = assertFailsWith<UnsupportedTemporalTypeException> {
            dateTime.get(ChronoField.NANO_OF_DAY)
        }
        assertEquals(
            "Invalid field 'NanoOfDay' for get() method, use getLong() instead",
            nanoOfDayException.message,
        )
        val epochDayException = assertFailsWith<UnsupportedTemporalTypeException> {
            dateTime.get(ChronoField.EPOCH_DAY)
        }
        assertEquals(
            "Invalid field 'EpochDay' for get() method, use getLong() instead",
            epochDayException.message,
        )
    }

    @Test
    fun convertsFromTemporalsAndCombinesExistingTypes() {
        val date = LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(13, 14, 15, 123_456_789)
        val expected = LocalDateTime.of(date, time)
        assertEquals(expected, LocalDateTime.from(expected))
        assertEquals(
            expected,
            LocalDateTime.from(DateTimeAccessor(date.toEpochDay(), time.toNanoOfDay())),
        )
        assertFailsWith<DateTimeException> { LocalDateTime.from(DateTimeAccessor()) }

        assertEquals(expected, date.atTime(time))
        assertEquals(LocalDateTime.of(date, LocalTime.of(13, 14)), date.atTime(13, 14))
        assertEquals(LocalDateTime.of(date, LocalTime.of(13, 14, 15)), date.atTime(13, 14, 15))
        assertEquals(expected, date.atTime(13, 14, 15, 123_456_789))
        assertEquals(LocalDateTime.of(date, LocalTime.MIDNIGHT), date.atStartOfDay())
        assertEquals(expected, time.atDate(date))
    }

    @Test
    fun formatsOrdersAndExposesBoundaryConstants() {
        val earlier = LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_999_999)
        val later = LocalDateTime.of(2024, 3, 1, 0, 0)
        assertEquals("2024-02-29T23:59:59.999999999", earlier.toString())
        assertTrue(earlier < later)
        assertTrue(later.isAfter(earlier))
        assertTrue(earlier.isBefore(later))
        assertTrue(earlier.isEqual(LocalDateTime.of(earlier.date, earlier.time)))
        assertEquals(LocalDateTime.of(LocalDate.MIN, LocalTime.MIN), LocalDateTime.MIN)
        assertEquals(LocalDateTime.of(LocalDate.MAX, LocalTime.MAX), LocalDateTime.MAX)
    }

    @Test
    fun supportsUnitsAndReplacesDateAndTimeParts() {
        val dateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        ChronoUnit.entries.forEach { unit ->
            assertEquals(unit !== ChronoUnit.FOREVER, dateTime.isSupported(unit), unit.toString())
        }

        assertEquals(LocalDateTime.of(2023, 2, 28, 13, 14, 15, 123_456_789), dateTime.withYear(2023))
        assertEquals(LocalDateTime.of(2024, 1, 29, 13, 14, 15, 123_456_789), dateTime.withMonth(1))
        assertEquals(LocalDateTime.of(2024, 2, 2, 13, 14, 15, 123_456_789), dateTime.withDayOfMonth(2))
        assertEquals(LocalDateTime.of(2024, 3, 1, 13, 14, 15, 123_456_789), dateTime.withDayOfYear(61))
        assertEquals(LocalDateTime.of(2024, 2, 29, 2, 14, 15, 123_456_789), dateTime.withHour(2))
        assertEquals(LocalDateTime.of(2024, 2, 29, 13, 2, 15, 123_456_789), dateTime.withMinute(2))
        assertEquals(LocalDateTime.of(2024, 2, 29, 13, 14, 2, 123_456_789), dateTime.withSecond(2))
        assertEquals(LocalDateTime.of(2024, 2, 29, 13, 14, 15, 2), dateTime.withNano(2))
        assertEquals(LocalDateTime.of(2025, 2, 28, 13, 14, 15, 123_456_789), dateTime.with(ChronoField.YEAR, 2025))
        assertEquals(LocalDateTime.of(2024, 2, 29, 2, 14, 15, 123_456_789), dateTime.with(ChronoField.HOUR_OF_DAY, 2))
        assertEquals(LocalDateTime.of(2025, 1, 2, 13, 14, 15, 123_456_789), dateTime.with(LocalDate.of(2025, 1, 2)))
        assertEquals(LocalDateTime.of(2024, 2, 29, 1, 2, 3, 4), dateTime.with(LocalTime.of(1, 2, 3, 4)))
        assertEquals(
            LocalDateTime.of(2024, 2, 29, 13, 14, 20, 123_456_789),
            dateTime.with(TemporalAdjuster { it.with(ChronoField.SECOND_OF_MINUTE, 20) }),
        )
    }

    @Test
    fun addsAndSubtractsDateAndTimeAmountsAcrossMidnight() {
        val dateTime = LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_999_999)
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), dateTime.plusNanos(1))
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0, 0, 999_999_999), dateTime.plusSeconds(1))
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0, 59, 999_999_999), dateTime.plusMinutes(1))
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 59, 59, 999_999_999), dateTime.plusHours(1))
        assertEquals(LocalDateTime.of(2025, 2, 28, 23, 59, 59, 999_999_999), dateTime.plusYears(1))
        assertEquals(LocalDateTime.of(2024, 3, 29, 23, 59, 59, 999_999_999), dateTime.plusMonths(1))
        assertEquals(dateTime.plusDays(7), dateTime.plusWeeks(1))
        assertEquals(dateTime.plusHours(12), dateTime.plus(1, ChronoUnit.HALF_DAYS))
        assertEquals(dateTime.plusSeconds(2).plusNanos(3), dateTime.plus(Duration.ofSeconds(2, 3)))
        assertEquals(dateTime.plusMonths(1).plusDays(2), dateTime.plus(Period.of(0, 1, 2)))
        assertEquals(dateTime, dateTime.plusHours(25).minusHours(25))
        assertEquals(dateTime, dateTime.minusNanos(Long.MIN_VALUE).plusNanos(Long.MIN_VALUE))
        assertFailsWith<UnsupportedTemporalTypeException> { dateTime.plus(1, ChronoUnit.FOREVER) }
    }

    @Test
    fun truncatesMeasuresCompleteUnitsAndAdjustsAnotherTemporal() {
        val start = LocalDateTime.of(2024, 2, 28, 23, 30, 40, 500_000_000)
        val end = LocalDateTime.of(2024, 3, 1, 1, 31, 42, 750_000_000)
        assertEquals(LocalDateTime.of(2024, 2, 28, 23, 30), start.truncatedTo(ChronoUnit.MINUTES))
        assertEquals(93_662_250_000_000L, start.until(end, ChronoUnit.NANOS))
        assertEquals(93_662L, start.until(end, ChronoUnit.SECONDS))
        assertEquals(26L, start.until(end, ChronoUnit.HOURS))
        assertEquals(1L, start.until(end, ChronoUnit.DAYS))
        assertEquals(0L, start.until(end, ChronoUnit.MONTHS))
        assertEquals(-26L, end.until(start, ChronoUnit.HOURS))
        assertEquals(
            DateTimeRecordingTemporal(
                operations = listOf(
                    ChronoField.EPOCH_DAY to start.date.toEpochDay(),
                    ChronoField.NANO_OF_DAY to start.time.toNanoOfDay(),
                ),
            ),
            start.adjustInto(DateTimeRecordingTemporal()),
        )
    }

    private data class DateTimeAccessor(
        val epochDay: Long? = null,
        val nanoOfDay: Long? = null,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.EPOCH_DAY && epochDay != null ||
                field === ChronoField.NANO_OF_DAY && nanoOfDay != null

        override fun getLong(field: TemporalField): Long = when (field) {
            ChronoField.EPOCH_DAY -> epochDay
            ChronoField.NANO_OF_DAY -> nanoOfDay
            else -> null
        } ?: throw UnsupportedTemporalTypeException("Unsupported field: $field")
    }

    private data class DateTimeRecordingTemporal(
        val operations: List<Pair<TemporalField, Long>> = emptyList(),
    ) : Temporal {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.EPOCH_DAY || field === ChronoField.NANO_OF_DAY

        override fun isSupported(unit: TemporalUnit): Boolean = false

        override fun getLong(field: TemporalField): Long =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (isSupported(field)) copy(operations = operations + (field to newValue)) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
