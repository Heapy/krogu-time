package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.internal.addExact
import io.heapy.grogu.time.internal.floorDiv
import io.heapy.grogu.time.internal.floorMod
import io.heapy.grogu.time.internal.multiplyExact
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAdjuster
import io.heapy.grogu.time.temporal.TemporalAmount
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.ValueRange

internal class ChronoLocalDateTimeImpl<D : ChronoLocalDate> private constructor(
    override val date: D,
    override val time: LocalTime,
) : ChronoLocalDateTime<D> {
    override fun isSupported(field: TemporalField?): Boolean =
        if (field is ChronoField) field.isDateBased || field.isTimeBased else field != null && field.isSupportedBy(this)

    override fun range(field: TemporalField): ValueRange = when {
        field is ChronoField && field.isTimeBased -> time.range(field)
        field is ChronoField -> date.range(field)
        else -> field.rangeRefinedBy(this)
    }

    override fun get(field: TemporalField): Int = when {
        field is ChronoField && field.isTimeBased -> time.get(field)
        field is ChronoField -> date.get(field)
        else -> range(field).checkValidIntValue(getLong(field), field)
    }

    override fun getLong(field: TemporalField): Long = when {
        field is ChronoField && field.isTimeBased -> time.getLong(field)
        field is ChronoField -> date.getLong(field)
        else -> field.getFrom(this)
    }

    override fun with(adjuster: TemporalAdjuster): ChronoLocalDateTimeImpl<D> = when (adjuster) {
        is ChronoLocalDate -> with(adjuster, time)
        is LocalTime -> with(date, adjuster)
        is ChronoLocalDateTimeImpl<*> -> ensureValid(chronology, adjuster)
        else -> ensureValid(chronology, adjuster.adjustInto(this))
    }

    override fun with(field: TemporalField, newValue: Long): ChronoLocalDateTimeImpl<D> =
        if (field is ChronoField) {
            if (field.isTimeBased) {
                with(date, time.with(field, newValue))
            } else {
                with(date.with(field, newValue), time)
            }
        } else {
            ensureValid(chronology, field.adjustInto(this, newValue))
        }

    override fun plus(amount: TemporalAmount): ChronoLocalDateTimeImpl<D> =
        ensureValid(chronology, amount.addTo(this))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoLocalDateTimeImpl<D> {
        if (unit !is ChronoUnit) return ensureValid(chronology, unit.addTo(this, amountToAdd))
        return when (unit) {
            ChronoUnit.NANOS -> plusNanos(amountToAdd)
            ChronoUnit.MICROS -> plusDays(amountToAdd / MICROS_PER_DAY)
                .plusNanos(amountToAdd % MICROS_PER_DAY * NANOS_PER_MICRO)
            ChronoUnit.MILLIS -> plusDays(amountToAdd / MILLIS_PER_DAY)
                .plusNanos(amountToAdd % MILLIS_PER_DAY * NANOS_PER_MILLI)
            ChronoUnit.SECONDS -> plusSeconds(amountToAdd)
            ChronoUnit.MINUTES -> plusMinutes(amountToAdd)
            ChronoUnit.HOURS -> plusHours(amountToAdd)
            ChronoUnit.HALF_DAYS -> plusDays(amountToAdd / 256)
                .plusHours(amountToAdd % 256 * 12)
            else -> with(date.plus(amountToAdd, unit), time)
        }
    }

    override fun minus(amount: TemporalAmount): ChronoLocalDateTimeImpl<D> =
        ensureValid(chronology, amount.subtractFrom(this))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): ChronoLocalDateTimeImpl<D> =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = chronology.localDateTime(endExclusive)
        if (end.chronology != chronology) throw chronologyMismatch(chronology, end.chronology)
        if (unit !is ChronoUnit) return unit.between(this, end)
        if (unit.isTimeBased) {
            val days = end.date.toEpochDay() - date.toEpochDay()
            val wholeDays = when (unit) {
                ChronoUnit.NANOS -> multiplyExact(days, NANOS_PER_DAY)
                ChronoUnit.MICROS -> multiplyExact(days, MICROS_PER_DAY)
                ChronoUnit.MILLIS -> multiplyExact(days, MILLIS_PER_DAY)
                ChronoUnit.SECONDS -> multiplyExact(days, SECONDS_PER_DAY)
                ChronoUnit.MINUTES -> multiplyExact(days, MINUTES_PER_DAY)
                ChronoUnit.HOURS -> multiplyExact(days, HOURS_PER_DAY)
                ChronoUnit.HALF_DAYS -> multiplyExact(days, 2)
                else -> 0
            }
            return addExact(wholeDays, time.until(end.time, unit))
        }
        var endDate: ChronoLocalDate = end.date
        if (end.time < time) endDate = endDate.minus(1, ChronoUnit.DAYS)
        return date.until(endDate, unit)
    }

    override fun atZone(zone: ZoneId): ChronoZonedDateTime<D> =
        ChronoZonedDateTimeImpl.ofBest(this, zone, null)

    private fun plusDays(days: Long): ChronoLocalDateTimeImpl<D> = with(
        date.plus(days, ChronoUnit.DAYS),
        time,
    )

    private fun plusHours(hours: Long): ChronoLocalDateTimeImpl<D> =
        plusWithOverflow(date, hours, 0, 0, 0)

    private fun plusMinutes(minutes: Long): ChronoLocalDateTimeImpl<D> =
        plusWithOverflow(date, 0, minutes, 0, 0)

    internal fun plusSeconds(seconds: Long): ChronoLocalDateTimeImpl<D> =
        plusWithOverflow(date, 0, 0, seconds, 0)

    private fun plusNanos(nanos: Long): ChronoLocalDateTimeImpl<D> =
        plusWithOverflow(date, 0, 0, 0, nanos)

    private fun plusWithOverflow(
        newDate: D,
        hours: Long,
        minutes: Long,
        seconds: Long,
        nanos: Long,
    ): ChronoLocalDateTimeImpl<D> {
        if ((hours or minutes or seconds or nanos) == 0L) return with(newDate, time)
        var totalDays = nanos / NANOS_PER_DAY +
            seconds / SECONDS_PER_DAY +
            minutes / MINUTES_PER_DAY +
            hours / HOURS_PER_DAY
        val totalNanos = nanos % NANOS_PER_DAY +
            seconds % SECONDS_PER_DAY * NANOS_PER_SECOND +
            minutes % MINUTES_PER_DAY * NANOS_PER_MINUTE +
            hours % HOURS_PER_DAY * NANOS_PER_HOUR +
            time.toNanoOfDay()
        totalDays += floorDiv(totalNanos, NANOS_PER_DAY)
        val newNanoOfDay = floorMod(totalNanos, NANOS_PER_DAY)
        val newTime = if (newNanoOfDay == time.toNanoOfDay()) time else LocalTime.ofNanoOfDay(newNanoOfDay)
        return with(newDate.plus(totalDays, ChronoUnit.DAYS), newTime)
    }

    private fun with(newDate: Temporal, newTime: LocalTime): ChronoLocalDateTimeImpl<D> {
        val checkedDate = ensureDate<D>(chronology, newDate)
        return if (date === checkedDate && time === newTime) this else ChronoLocalDateTimeImpl(checkedDate, newTime)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is ChronoLocalDateTime<*> && compareTo(other) == 0

    override fun hashCode(): Int = date.hashCode() xor time.hashCode()

    override fun toString(): String = "${date}T$time"

    internal companion object {
        private const val HOURS_PER_DAY: Long = 24
        private const val MINUTES_PER_DAY: Long = 1_440
        private const val SECONDS_PER_DAY: Long = 86_400
        private const val NANOS_PER_MICRO: Long = 1_000
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val NANOS_PER_SECOND: Long = 1_000_000_000
        private const val NANOS_PER_MINUTE: Long = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR: Long = 60 * NANOS_PER_MINUTE
        private const val NANOS_PER_DAY: Long = 24 * NANOS_PER_HOUR
        private const val MICROS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MICRO
        private const val MILLIS_PER_DAY: Long = NANOS_PER_DAY / NANOS_PER_MILLI

        fun <D : ChronoLocalDate> of(date: D, time: LocalTime): ChronoLocalDateTimeImpl<D> =
            ChronoLocalDateTimeImpl(date, time)

        @Suppress("UNCHECKED_CAST")
        fun <D : ChronoLocalDate> ensureDate(chronology: Chronology, temporal: Temporal): D {
            val date = temporal as? ChronoLocalDate
                ?: throw ClassCastException("Temporal is not a ChronoLocalDate: $temporal")
            if (date.chronology != chronology) throw chronologyMismatch(chronology, date.chronology)
            return date as D
        }

        @Suppress("UNCHECKED_CAST")
        fun <D : ChronoLocalDate> ensureValid(
            chronology: Chronology,
            temporal: Temporal,
        ): ChronoLocalDateTimeImpl<D> {
            val dateTime = temporal as? ChronoLocalDateTimeImpl<*>
                ?: throw ClassCastException("Temporal is not a chronology local date-time: $temporal")
            if (dateTime.chronology != chronology) {
                throw chronologyMismatch(chronology, dateTime.chronology)
            }
            return dateTime as ChronoLocalDateTimeImpl<D>
        }

        private fun chronologyMismatch(expected: Chronology, actual: Chronology): ClassCastException =
            ClassCastException(
                "Chronology mismatch, expected: ${expected.id}, actual: ${actual.id}",
            )
    }
}
