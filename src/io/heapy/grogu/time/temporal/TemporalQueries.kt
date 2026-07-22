package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.chrono.Chronology

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

    private val localDateQuery: TemporalQuery<LocalDate?> = object : TemporalQuery<LocalDate?> {
        override fun queryFrom(temporal: TemporalAccessor): LocalDate? =
            if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
                LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY))
            } else {
                null
            }

        override fun toString(): String = "LocalDate"
    }

    private val localTimeQuery: TemporalQuery<LocalTime?> = object : TemporalQuery<LocalTime?> {
        override fun queryFrom(temporal: TemporalAccessor): LocalTime? =
            if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY))
            } else {
                null
            }

        override fun toString(): String = "LocalTime"
    }

    private val precisionQuery: TemporalQuery<TemporalUnit?> =
        object : TemporalQuery<TemporalUnit?> {
            override fun queryFrom(temporal: TemporalAccessor): TemporalUnit? =
                temporal.query(this)

            override fun toString(): String = "Precision"
        }

    private val chronologyQuery: TemporalQuery<Chronology?> =
        object : TemporalQuery<Chronology?> {
            override fun queryFrom(temporal: TemporalAccessor): Chronology? =
                temporal.query(this)

            override fun toString(): String = "Chronology"
        }

    /** Returns a query that obtains the zone offset, or `null` when unavailable. */
    public fun offset(): TemporalQuery<ZoneOffset?> = offsetQuery

    /** Returns a query that obtains an explicit zone ID, excluding bare offsets. */
    public fun zoneId(): TemporalQuery<ZoneId?> = zoneIdQuery

    /** Returns a query that obtains a zone ID, falling back to a zone offset. */
    public fun zone(): TemporalQuery<ZoneId?> = zoneQuery

    /** Returns a query that obtains the local date, or `null` when unavailable. */
    public fun localDate(): TemporalQuery<LocalDate?> = localDateQuery

    /** Returns a query that obtains the local time, or `null` when unavailable. */
    public fun localTime(): TemporalQuery<LocalTime?> = localTimeQuery

    /** Returns a query that obtains the smallest supported unit. */
    public fun precision(): TemporalQuery<TemporalUnit?> = precisionQuery

    /** Returns a query that obtains the chronology. */
    public fun chronology(): TemporalQuery<Chronology?> = chronologyQuery
}
