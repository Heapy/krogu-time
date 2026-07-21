package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException

/** An era of a calendar system. */
public interface Era : TemporalAccessor, TemporalAdjuster {
    /** The numeric era value used by [ChronoField.ERA]. */
    public val value: Int

    override fun isSupported(field: TemporalField): Boolean =
        if (field is ChronoField) {
            field === ChronoField.ERA
        } else {
            field.isSupportedBy(this)
        }

    override fun getLong(field: TemporalField): Long = when {
        field === ChronoField.ERA -> value.toLong()
        field is ChronoField -> throw UnsupportedTemporalTypeException("Unsupported field: $field")
        else -> field.getFrom(this)
    }

    override fun adjustInto(temporal: Temporal): Temporal =
        temporal.with(ChronoField.ERA, value.toLong())
}
