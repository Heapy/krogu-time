package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.ZoneOffset

/** Common queries for extracting information from temporal objects. */
public object TemporalQueries {
    private val offsetQuery: TemporalQuery<ZoneOffset?> = TemporalQuery { temporal ->
        if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
            ZoneOffset.ofTotalSeconds(temporal.get(ChronoField.OFFSET_SECONDS))
        } else {
            null
        }
    }

    /** Returns a query that obtains the zone offset, or `null` when unavailable. */
    public fun offset(): TemporalQuery<ZoneOffset?> = offsetQuery
}
