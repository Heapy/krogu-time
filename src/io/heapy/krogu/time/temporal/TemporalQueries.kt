package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.chrono.Chronology
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** Common queries for extracting information from temporal objects. */
public object TemporalQueries {
    private val offsetQuery: TemporalQuery<ZoneOffset?> =
        object : TemporalQuery<ZoneOffset?> {
            override fun queryFrom(temporal: TemporalAccessor): ZoneOffset? =
                if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
                    ZoneOffset.ofTotalSeconds(temporal.get(ChronoField.OFFSET_SECONDS))
                } else {
                    null
                }

            override fun toString(): String = "ZoneOffset"
        }

    private val zoneIdQuery: TemporalQuery<ZoneId?> =
        object : TemporalQuery<ZoneId?> {
            override fun queryFrom(temporal: TemporalAccessor): ZoneId? = temporal.query(this)

            override fun toString(): String = "ZoneId"
        }

    private val zoneQuery: TemporalQuery<ZoneId?> =
        object : TemporalQuery<ZoneId?> {
            override fun queryFrom(temporal: TemporalAccessor): ZoneId? =
                temporal.query(zoneIdQuery) ?: temporal.query(offsetQuery)

            override fun toString(): String = "Zone"
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
    @JvmStatic
    public fun offset(): TemporalQuery<ZoneOffset?> = offsetQuery

    /** Returns a query that obtains an explicit zone ID, excluding bare offsets. */
    @JvmStatic
    public fun zoneId(): TemporalQuery<ZoneId?> = zoneIdQuery

    /** Returns a query that obtains a zone ID, falling back to a zone offset. */
    @JvmStatic
    public fun zone(): TemporalQuery<ZoneId?> = zoneQuery

    /** Returns a query that obtains the local date, or `null` when unavailable. */
    @JvmStatic
    public fun localDate(): TemporalQuery<LocalDate?> = localDateQuery

    /** Returns a query that obtains the local time, or `null` when unavailable. */
    @JvmStatic
    public fun localTime(): TemporalQuery<LocalTime?> = localTimeQuery

    /** Returns a query that obtains the smallest supported unit. */
    @JvmStatic
    public fun precision(): TemporalQuery<TemporalUnit?> = precisionQuery

    /** Returns a query that obtains the chronology. */
    @JvmStatic
    public fun chronology(): TemporalQuery<Chronology?> = chronologyQuery
}
