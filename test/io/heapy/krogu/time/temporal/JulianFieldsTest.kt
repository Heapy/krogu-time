package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JulianFieldsTest {
    @Test
    fun exposesJulianFieldMetadataAndRanges() {
        val fields = listOf(
            JulianFields.JULIAN_DAY to "JulianDay",
            JulianFields.MODIFIED_JULIAN_DAY to "ModifiedJulianDay",
            JulianFields.RATA_DIE to "RataDie",
        )

        fields.forEach { (field, name) ->
            assertEquals(name, field.toString())
            assertEquals(ChronoUnit.DAYS, field.baseUnit)
            assertEquals(ChronoUnit.FOREVER, field.rangeUnit)
            assertTrue(field.isDateBased)
            assertFalse(field.isTimeBased)
            assertTrue(field.isSupportedBy(LocalDate.EPOCH))
            assertFalse(field.isSupportedBy(LocalTime.NOON))
        }
        assertEquals(
            ValueRange.of(-365_240_778_574L, 365_244_221_059L),
            JulianFields.JULIAN_DAY.range,
        )
        assertEquals(
            ValueRange.of(-365_243_178_575L, 365_241_821_058L),
            JulianFields.MODIFIED_JULIAN_DAY.range,
        )
        assertEquals(
            ValueRange.of(-365_242_499_999L, 365_242_499_634L),
            JulianFields.RATA_DIE.range,
        )
    }

    @Test
    fun readsKnownDayNumbersFromDatesAndDateTimes() {
        val epoch = LocalDate.of(1970, 1, 1)
        val firstCommonEraDay = LocalDate.of(1, 1, 1)
        val dateTime = LocalDateTime.of(epoch, LocalTime.of(23, 59, 59))

        assertEquals(2_440_588, epoch.getLong(JulianFields.JULIAN_DAY))
        assertEquals(40_587, epoch.getLong(JulianFields.MODIFIED_JULIAN_DAY))
        assertEquals(719_163, epoch.getLong(JulianFields.RATA_DIE))
        assertEquals(1, firstCommonEraDay.getLong(JulianFields.RATA_DIE))
        assertEquals(2_440_588, dateTime.getLong(JulianFields.JULIAN_DAY))
    }

    @Test
    fun adjustsEpochDayWhilePreservingLocalTime() {
        val source = LocalDateTime.of(2000, 1, 1, 12, 34, 56, 789)

        assertEquals(
            LocalDateTime.of(1970, 1, 2, 12, 34, 56, 789),
            source.with(JulianFields.JULIAN_DAY, 2_440_589),
        )
        assertEquals(
            LocalDateTime.of(1970, 1, 2, 12, 34, 56, 789),
            source.with(JulianFields.MODIFIED_JULIAN_DAY, 40_588),
        )
        assertEquals(
            LocalDateTime.of(1, 1, 2, 12, 34, 56, 789),
            source.with(JulianFields.RATA_DIE, 2),
        )
        assertFailsWith<DateTimeException> {
            source.with(JulianFields.JULIAN_DAY, Long.MAX_VALUE)
        }
        assertFailsWith<DateTimeException> {
            JulianFields.JULIAN_DAY.rangeRefinedBy(LocalTime.NOON)
        }
    }
}
