package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.format.LocaleTextField
import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.format.localeTextValues
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/** An era of a calendar system. */
public interface Era : TemporalAccessor, TemporalAdjuster {
    /** The numeric era value used by [ChronoField.ERA]. */
    public val value: Int

    /** Returns this era's localized display name, or its numeric value when unavailable. */
    public fun getDisplayName(style: TextStyle, locale: Locale): String =
        localeTextValues(
            locale.toLanguageTag(),
            displayChronologyId,
            LocaleTextField.ERA,
            style,
        ).firstOrNull { it.value == value.toLong() }?.text ?: value.toString()

    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) {
            field === ChronoField.ERA
        } else {
            field != null && field.isSupportedBy(this)
        }

    override fun getLong(field: TemporalField): Long = when {
        field === ChronoField.ERA -> value.toLong()
        field is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun <R> query(query: TemporalQuery<R>): R {
        if (query === TemporalQueries.precision()) {
            @Suppress("UNCHECKED_CAST")
            return ChronoUnit.ERAS as R
        }
        return super<TemporalAccessor>.query(query)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.ERA, value.toLong())
}

private val Era.displayChronologyId: String
    get() = when (this) {
        is JapaneseEra -> JapaneseChronology.id
        is HijrahEra -> HijrahChronology.id
        is MinguoEra -> MinguoChronology.id
        is ThaiBuddhistEra -> ThaiBuddhistChronology.id
        else -> IsoChronology.id
    }
