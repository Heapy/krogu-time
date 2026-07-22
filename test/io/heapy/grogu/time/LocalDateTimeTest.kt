package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
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
}
