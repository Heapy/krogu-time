package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZoneId

/** Common queries for extracting information from temporal objects. */
public object TemporalQueries {
    private val offsetQuery: TemporalQuery<ZoneOffset?> = TemporalQuery { temporal ->
        if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
            ZoneOffset.ofTotalSeconds(temporal.get(ChronoField.OFFSET_SECONDS))
        } else {
            null
        }
    }

    private val zoneIdQuery: TemporalQuery<ZoneId?> = TemporalQuery { null }

    private val zoneQuery: TemporalQuery<ZoneId?> = TemporalQuery { temporal ->
        temporal.query(zoneIdQuery) ?: temporal.query(offsetQuery)
    }

    /** Returns a query that obtains the zone offset, or `null` when unavailable. */
    public fun offset(): TemporalQuery<ZoneOffset?> = offsetQuery

    /** Returns a query that obtains an explicit zone ID, excluding bare offsets. */
    public fun zoneId(): TemporalQuery<ZoneId?> = zoneIdQuery

    /** Returns a query that obtains a zone ID, falling back to a zone offset. */
    public fun zone(): TemporalQuery<ZoneId?> = zoneQuery
}
