package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.format.ResolverStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalFieldDisplayNameTest {
    @Test
    fun customFieldsDefaultToTheirStringRepresentation() {
        assertEquals("SampleField", SampleField.getDisplayName(Locale.ENGLISH))
        assertEquals("SampleField", SampleField.getDisplayName(Locale.forLanguageTag("fr")))
    }

    @Test
    fun standardFieldsExposeLocalizedNamesOrCanonicalFallbacks() {
        assertEquals("second", ChronoField.SECOND_OF_MINUTE.getDisplayName(Locale.ENGLISH))
        assertEquals("minute", ChronoField.MINUTE_OF_HOUR.getDisplayName(Locale.ENGLISH))
        assertEquals("hour", ChronoField.HOUR_OF_DAY.getDisplayName(Locale.ENGLISH))
        assertEquals("AM/PM", ChronoField.AMPM_OF_DAY.getDisplayName(Locale.ENGLISH))
        assertEquals("day of the week", ChronoField.DAY_OF_WEEK.getDisplayName(Locale.ENGLISH))
        assertEquals("month", ChronoField.MONTH_OF_YEAR.getDisplayName(Locale.ENGLISH))
        assertEquals("year", ChronoField.YEAR.getDisplayName(Locale.ENGLISH))
        assertEquals("era", ChronoField.ERA.getDisplayName(Locale.ENGLISH))

        assertEquals("NanoOfSecond", ChronoField.NANO_OF_SECOND.getDisplayName(Locale.ENGLISH))
        assertEquals("DayOfMonth", ChronoField.DAY_OF_MONTH.getDisplayName(Locale.ENGLISH))
        assertEquals("DayOfQuarter", IsoFields.DAY_OF_QUARTER.getDisplayName(Locale.ENGLISH))
        assertEquals("week", IsoFields.WEEK_OF_WEEK_BASED_YEAR.getDisplayName(Locale.ENGLISH))
        assertEquals("week", WeekFields.ISO.weekOfYear.getDisplayName(Locale.ENGLISH))
        assertEquals("WeekOfMonth", WeekFields.ISO.weekOfMonth.getDisplayName(Locale.ENGLISH))
    }

    private data object SampleField : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.MONTHS
        override val range: ValueRange = ValueRange.of(1, 31)
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = false

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = 0

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor? = null

        override fun toString(): String = "SampleField"
    }
}
