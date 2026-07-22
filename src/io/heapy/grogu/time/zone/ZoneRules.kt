package io.heapy.grogu.time.zone

import io.heapy.grogu.time.Duration
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneOffset

/** The offset rules for a time-zone. */
public class ZoneRules private constructor(
    private val offset: ZoneOffset,
) {
    /** Whether the offset never changes. */
    public val isFixedOffset: Boolean
        get() = true

    /** Returns the offset applicable at [instant]. */
    @Suppress("UNUSED_PARAMETER")
    public fun getOffset(instant: Instant): ZoneOffset = offset

    /** Returns the best available offset for [localDateTime]. */
    @Suppress("UNUSED_PARAMETER")
    public fun getOffset(localDateTime: LocalDateTime): ZoneOffset = offset

    /** Returns the valid offsets for [localDateTime]. */
    @Suppress("UNUSED_PARAMETER")
    public fun getValidOffsets(localDateTime: LocalDateTime): List<ZoneOffset> = listOf(offset)

    /** Returns a transition affecting [localDateTime], or `null` for fixed rules. */
    @Suppress("UNUSED_PARAMETER")
    public fun getTransition(localDateTime: LocalDateTime): ZoneOffsetTransition? = null

    /** Returns the standard offset applicable at [instant]. */
    @Suppress("UNUSED_PARAMETER")
    public fun getStandardOffset(instant: Instant): ZoneOffset = offset

    /** Returns the daylight-saving adjustment applicable at [instant]. */
    @Suppress("UNUSED_PARAMETER")
    public fun getDaylightSavings(instant: Instant): Duration = Duration.ZERO

    /** Whether daylight saving is active at [instant]. */
    @Suppress("UNUSED_PARAMETER")
    public fun isDaylightSavings(instant: Instant): Boolean = false

    /** Whether [offset] is valid for [localDateTime]. */
    @Suppress("UNUSED_PARAMETER")
    public fun isValidOffset(localDateTime: LocalDateTime, offset: ZoneOffset): Boolean =
        this.offset == offset

    /** Returns the next transition after [instant], or `null` for fixed rules. */
    @Suppress("UNUSED_PARAMETER")
    public fun nextTransition(instant: Instant): ZoneOffsetTransition? = null

    /** Returns the previous transition before [instant], or `null` for fixed rules. */
    @Suppress("UNUSED_PARAMETER")
    public fun previousTransition(instant: Instant): ZoneOffsetTransition? = null

    /** Returns the complete historic transition list. */
    public fun getTransitions(): List<ZoneOffsetTransition> = emptyList()

    /** Returns the recurring transition rules used beyond the historic list. */
    public fun getTransitionRules(): List<ZoneOffsetTransitionRule> = emptyList()

    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneRules && offset == other.offset

    override fun hashCode(): Int = 1

    override fun toString(): String = "ZoneRules[currentStandardOffset=$offset]"

    public companion object {
        /** Creates rules for a fixed [offset]. */
        public fun of(offset: ZoneOffset): ZoneRules = ZoneRules(offset)
    }
}
