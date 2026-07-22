package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.format.ResolverStyle
import io.heapy.grogu.time.internal.subtractExact

/** Date fields that express an epoch day using common continuous day-number systems. */
public object JulianFields {
    /** Julian Day Number, where ISO 1970-01-01 is day 2,440,588. */
    public val JULIAN_DAY: TemporalField = Field.JULIAN_DAY

    /** Modified Julian Day, where ISO 1970-01-01 is day 40,587. */
    public val MODIFIED_JULIAN_DAY: TemporalField = Field.MODIFIED_JULIAN_DAY

    /** Rata Die, where ISO 0001-01-01 is day 1. */
    public val RATA_DIE: TemporalField = Field.RATA_DIE

    private enum class Field(
        private val displayName: String,
        private val offset: Long,
    ) : TemporalField {
        JULIAN_DAY("JulianDay", 2_440_588),
        MODIFIED_JULIAN_DAY("ModifiedJulianDay", 40_587),
        RATA_DIE("RataDie", 719_163),
        ;

        override val baseUnit: TemporalUnit
            get() = ChronoUnit.DAYS

        override val rangeUnit: TemporalUnit
            get() = ChronoUnit.FOREVER

        override val range: ValueRange = ValueRange.of(
            -365_243_219_162L + offset,
            365_241_780_471L + offset,
        )

        override val isDateBased: Boolean
            get() = true

        override val isTimeBased: Boolean
            get() = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean =
            temporal.isSupported(ChronoField.EPOCH_DAY)

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange {
            if (!isSupportedBy(temporal)) {
                throw DateTimeException("Unsupported field: $this")
            }
            return range
        }

        override fun getFrom(temporal: TemporalAccessor): Long =
            temporal.getLong(ChronoField.EPOCH_DAY) + offset

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
            if (!range.isValidValue(newValue)) {
                throw DateTimeException("Invalid value: $displayName $newValue")
            }
            @Suppress("UNCHECKED_CAST")
            return temporal.with(ChronoField.EPOCH_DAY, newValue - offset) as R
        }

        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor {
            val value = requireNotNull(fieldValues.remove(this))
            if (resolverStyle != ResolverStyle.LENIENT) {
                range.checkValidValue(value, this)
            }
            return Chronology.from(partialTemporal).dateEpochDay(subtractExact(value, offset))
        }

        override fun toString(): String = displayName
    }
}
