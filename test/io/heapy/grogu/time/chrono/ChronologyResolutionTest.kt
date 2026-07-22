package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.format.ResolverStyle
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.TemporalField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ChronologyResolutionTest {
    @Test
    fun resolvesEpochCalendarOrdinalAndAlignedDateFields() {
        val epochFields = fields(ChronoField.EPOCH_DAY to 0)
        assertEquals(LocalDate.of(1970, 1, 1), IsoChronology.resolveDate(epochFields, ResolverStyle.STRICT))
        assertEquals(emptyMap(), epochFields)

        val ordinalFields = fields(
            ChronoField.YEAR to 2024,
            ChronoField.DAY_OF_YEAR to 60,
        )
        assertEquals(LocalDate.of(2024, 2, 29), IsoChronology.resolveDate(ordinalFields, ResolverStyle.STRICT))
        assertEquals(emptyMap(), ordinalFields)

        val alignedFields = fields(
            ChronoField.YEAR to 2024,
            ChronoField.MONTH_OF_YEAR to 2,
            ChronoField.ALIGNED_WEEK_OF_MONTH to 2,
            ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH to 3,
        )
        assertEquals(LocalDate.of(2024, 2, 10), IsoChronology.resolveDate(alignedFields, ResolverStyle.STRICT))
        assertEquals(emptyMap(), alignedFields)
    }

    @Test
    fun resolverStylesApplyStrictSmartAndLenientCalendarRules() {
        val strictFields = calendarFields(2023, 2, 31)
        assertFailsWith<DateTimeException> {
            IsoChronology.resolveDate(strictFields, ResolverStyle.STRICT)
        }

        val smartFields = calendarFields(2023, 2, 31)
        assertEquals(LocalDate.of(2023, 2, 28), IsoChronology.resolveDate(smartFields, ResolverStyle.SMART))
        assertEquals(emptyMap(), smartFields)

        val lenientFields = calendarFields(2023, 2, 31)
        assertEquals(LocalDate.of(2023, 3, 3), IsoChronology.resolveDate(lenientFields, ResolverStyle.LENIENT))
        assertEquals(emptyMap(), lenientFields)
    }

    @Test
    fun strictYearOfEraRemainsUnresolvedWithoutAnEraOrProlepticYear() {
        val fieldValues = fields(
            ChronoField.YEAR_OF_ERA to 2024,
            ChronoField.MONTH_OF_YEAR to 2,
            ChronoField.DAY_OF_MONTH to 29,
        )

        assertNull(IsoChronology.resolveDate(fieldValues, ResolverStyle.STRICT))
        assertEquals(
            fields(
                ChronoField.YEAR_OF_ERA to 2024,
                ChronoField.MONTH_OF_YEAR to 2,
                ChronoField.DAY_OF_MONTH to 29,
            ),
            fieldValues,
        )
    }

    @Test
    fun prolepticMonthExpansionDetectsConflictingFields() {
        val fieldValues = fields(
            ChronoField.PROLEPTIC_MONTH to 2024L * 12 + 1,
            ChronoField.YEAR to 2023,
            ChronoField.DAY_OF_MONTH to 29,
        )

        assertFailsWith<DateTimeException> {
            IsoChronology.resolveDate(fieldValues, ResolverStyle.STRICT)
        }
    }

    @Test
    fun japaneseEraYearAndDayOfYearResolveRelativeToTheEraStart() {
        val fieldValues = fields(
            ChronoField.ERA to JapaneseEra.REIWA.value.toLong(),
            ChronoField.YEAR_OF_ERA to 1,
            ChronoField.DAY_OF_YEAR to 1,
        )

        assertEquals(
            JapaneseDate.of(2019, 5, 1),
            JapaneseChronology.resolveDate(fieldValues, ResolverStyle.STRICT),
        )
        assertEquals(emptyMap(), fieldValues)
    }

    private fun calendarFields(year: Long, month: Long, day: Long): MutableMap<TemporalField, Long> =
        fields(
            ChronoField.YEAR to year,
            ChronoField.MONTH_OF_YEAR to month,
            ChronoField.DAY_OF_MONTH to day,
        )

    private fun fields(vararg values: Pair<TemporalField, Long>): MutableMap<TemporalField, Long> =
        mutableMapOf(*values)
}
