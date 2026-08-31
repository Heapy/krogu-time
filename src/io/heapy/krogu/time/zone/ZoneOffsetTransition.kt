package io.heapy.krogu.time.zone

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.ZoneOffset
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** A discontinuity in a local timeline caused by an offset change. */
public class ZoneOffsetTransition internal constructor(
    public val dateTimeBefore: LocalDateTime,
    public val offsetBefore: ZoneOffset,
    public val offsetAfter: ZoneOffset,
) : Comparable<ZoneOffsetTransition> {
    private val epochSecond: Long = dateTimeBefore.toEpochSecond(offsetBefore)

    /** The instant at which this transition occurs. */
    public val instant: Instant
        get() = Instant.ofEpochSecond(epochSecond)

    /** The first local date-time after the transition. */
    public val dateTimeAfter: LocalDateTime
        get() = dateTimeBefore.plusSeconds(durationSeconds.toLong())

    /** The signed duration of the local timeline discontinuity. */
    public val duration: Duration
        get() = Duration.ofSeconds(durationSeconds.toLong())

    /** Whether local date-times are skipped by this transition. */
    public val isGap: Boolean
        get() = offsetAfter.totalSeconds > offsetBefore.totalSeconds

    /** Whether local date-times occur twice because of this transition. */
    public val isOverlap: Boolean
        get() = offsetAfter.totalSeconds < offsetBefore.totalSeconds

    private val durationSeconds: Int
        get() = offsetAfter.totalSeconds - offsetBefore.totalSeconds

    /** Returns the transition instant as an epoch-second value. */
    public fun toEpochSecond(): Long = epochSecond

    /** Whether [offset] is valid in the local discontinuity. */
    public fun isValidOffset(offset: ZoneOffset): Boolean =
        !isGap && (offset == offsetBefore || offset == offsetAfter)

    override fun compareTo(other: ZoneOffsetTransition): Int =
        epochSecond.compareTo(other.epochSecond)

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ZoneOffsetTransition &&
            epochSecond == other.epochSecond &&
            offsetBefore == other.offsetBefore &&
            offsetAfter == other.offsetAfter

    override fun hashCode(): Int =
        dateTimeBefore.hashCode() xor offsetBefore.hashCode() xor offsetAfter.hashCode().rotateLeft(16)

    override fun toString(): String = buildString {
        append("Transition[")
        append(if (isGap) "Gap" else "Overlap")
        append(" at ")
        append(dateTimeBefore)
        append(offsetBefore)
        append(" to ")
        append(offsetAfter)
        append(']')
    }

    public companion object {
        /** Creates a transition at [transition] between two different offsets. */
        @JvmStatic
        public fun of(
            transition: LocalDateTime,
            offsetBefore: ZoneOffset,
            offsetAfter: ZoneOffset,
        ): ZoneOffsetTransition {
            require(offsetBefore != offsetAfter) { "Offsets must not be equal" }
            require(transition.nano == 0) { "Nano-of-second must be zero" }
            return ZoneOffsetTransition(transition, offsetBefore, offsetAfter)
        }
    }
}
