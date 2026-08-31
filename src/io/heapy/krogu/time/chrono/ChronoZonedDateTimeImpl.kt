package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAmount
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalUnit
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

internal class ChronoZonedDateTimeImpl<D : ChronoLocalDate> private constructor(
    override val dateTime: ChronoLocalDateTimeImpl<D>,
    override val offset: ZoneOffset,
    override val zone: ZoneId,
) : ChronoZonedDateTime<D> {
    override fun isSupported(field: TemporalField?): Boolean =
        field is ChronoField || field != null && field.isSupportedBy(this)

    override fun withEarlierOffsetAtOverlap(): ChronoZonedDateTime<D> {
        val transition = zone.rules.getTransition(dateTime.toIsoLocalDateTime())
        if (transition?.isOverlap == true && transition.offsetBefore != offset) {
            return ChronoZonedDateTimeImpl(dateTime, transition.offsetBefore, zone)
        }
        return this
    }

    override fun withLaterOffsetAtOverlap(): ChronoZonedDateTime<D> {
        val transition = zone.rules.getTransition(dateTime.toIsoLocalDateTime())
        if (transition != null && transition.offsetAfter != offset) {
            return ChronoZonedDateTimeImpl(dateTime, transition.offsetAfter, zone)
        }
        return this
    }

    override fun withZoneSameLocal(zone: ZoneId): ChronoZonedDateTime<D> =
        ofBest(dateTime, zone, offset)

    override fun withZoneSameInstant(zone: ZoneId): ChronoZonedDateTime<D> =
        if (this.zone == zone) this else create(dateTime.toInstant(offset), zone)

    override fun with(field: TemporalField, newValue: Long): ChronoZonedDateTime<D> {
        if (field !is ChronoField) return ensureValid(chronology, field.adjustInto(this, newValue))
        return when (field) {
            ChronoField.INSTANT_SECONDS -> plus(newValue - toEpochSecond(), ChronoUnit.SECONDS)
            ChronoField.OFFSET_SECONDS -> {
                val newOffset = ZoneOffset.ofTotalSeconds(field.checkValidIntValue(newValue))
                create(dateTime.toInstant(newOffset), zone)
            }
            else -> ofBest(dateTime.with(field, newValue), zone, offset)
        }
    }

    override fun plus(amount: TemporalAmount): ChronoZonedDateTime<D> =
        ensureValid(chronology, amount.addTo(this))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): ChronoZonedDateTime<D> =
        if (unit is ChronoUnit) {
            ofBest(dateTime.plus(amountToAdd, unit), zone, offset)
        } else {
            ensureValid(chronology, unit.addTo(this, amountToAdd))
        }

    override fun minus(amount: TemporalAmount): ChronoZonedDateTime<D> =
        ensureValid(chronology, amount.subtractFrom(this))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): ChronoZonedDateTime<D> =
        if (amountToSubtract == Long.MIN_VALUE) {
            plus(Long.MAX_VALUE, unit).plus(1, unit)
        } else {
            plus(-amountToSubtract, unit)
        }

    override fun until(endExclusive: Temporal, unit: TemporalUnit): Long {
        val end = chronology.zonedDateTime(endExclusive)
        if (end.chronology != chronology) throw chronologyMismatch(chronology, end.chronology)
        if (unit !is ChronoUnit) return unit.between(this, end)
        val sameOffsetEnd = end.withZoneSameInstant(offset)
        return dateTime.until(sameOffsetEnd.dateTime, unit)
    }

    @Suppress("UNCHECKED_CAST")
    private fun create(instant: Instant, zone: ZoneId): ChronoZonedDateTimeImpl<D> =
        ofInstant(chronology, instant, zone) as ChronoZonedDateTimeImpl<D>

    override fun equals(other: Any?): Boolean =
        this === other || other is ChronoZonedDateTime<*> && compareTo(other) == 0

    override fun hashCode(): Int =
        dateTime.hashCode() xor offset.hashCode() xor zone.hashCode().rotateLeft(3)

    override fun toString(): String = buildString {
        append(dateTime)
        append(offset)
        if (offset != zone) append('[').append(zone).append(']')
    }

    internal companion object {
        @JvmStatic
        fun <D : ChronoLocalDate> ofBest(
            localDateTime: ChronoLocalDateTimeImpl<D>,
            zone: ZoneId,
            preferredOffset: ZoneOffset?,
        ): ChronoZonedDateTime<D> {
            if (zone is ZoneOffset) return ChronoZonedDateTimeImpl(localDateTime, zone, zone)
            val rules = zone.rules
            val isoDateTime = localDateTime.toIsoLocalDateTime()
            val validOffsets = rules.getValidOffsets(isoDateTime)
            val resolvedDateTime: ChronoLocalDateTimeImpl<D>
            val offset: ZoneOffset
            when (validOffsets.size) {
                1 -> {
                    resolvedDateTime = localDateTime
                    offset = validOffsets[0]
                }
                0 -> {
                    val transition = checkNotNull(rules.getTransition(isoDateTime))
                    resolvedDateTime = localDateTime.plusSeconds(transition.duration.seconds)
                    offset = transition.offsetAfter
                }
                else -> {
                    resolvedDateTime = localDateTime
                    offset = if (preferredOffset != null && preferredOffset in validOffsets) {
                        preferredOffset
                    } else {
                        validOffsets[0]
                    }
                }
            }
            return ChronoZonedDateTimeImpl(resolvedDateTime, offset, zone)
        }

        @JvmStatic
        fun ofInstant(
            chronology: Chronology,
            instant: Instant,
            zone: ZoneId,
        ): ChronoZonedDateTimeImpl<*> {
            val offset = zone.rules.getOffset(instant)
            val isoDateTime = LocalDateTime.ofEpochSecond(instant.epochSecond, instant.nano, offset)
            val date = chronology.dateEpochDay(isoDateTime.date.toEpochDay())
            val localDateTime = ChronoLocalDateTimeImpl.of(date, isoDateTime.time)
            return ChronoZonedDateTimeImpl(localDateTime, offset, zone)
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <D : ChronoLocalDate> ensureValid(
            chronology: Chronology,
            temporal: Temporal,
        ): ChronoZonedDateTimeImpl<D> {
            val dateTime = temporal as? ChronoZonedDateTimeImpl<*>
                ?: throw ClassCastException("Temporal is not a chronology zoned date-time: $temporal")
            if (dateTime.chronology != chronology) {
                throw chronologyMismatch(chronology, dateTime.chronology)
            }
            return dateTime as ChronoZonedDateTimeImpl<D>
        }

        private fun chronologyMismatch(expected: Chronology, actual: Chronology): ClassCastException =
            ClassCastException(
                "Chronology mismatch, expected: ${expected.id}, actual: ${actual.id}",
            )

        private fun ChronoLocalDateTimeImpl<*>.toIsoLocalDateTime(): LocalDateTime =
            LocalDateTime.of(LocalDate.ofEpochDay(date.toEpochDay()), time)
    }
}
