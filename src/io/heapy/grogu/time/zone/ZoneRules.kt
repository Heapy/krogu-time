package io.heapy.grogu.time.zone

import io.heapy.grogu.time.Duration
import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.Year
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.internal.floorDiv

/** The offset rules for a time-zone. */
public class ZoneRules private constructor(
    private val standardTransitions: List<Long>,
    private val standardOffsets: List<ZoneOffset>,
    private val savingsInstantTransitions: List<Long>,
    private val savingsLocalTransitions: List<LocalDateTime>,
    private val wallOffsets: List<ZoneOffset>,
    private val lastRules: List<ZoneOffsetTransitionRule>,
) {
    /** Whether the offset never changes. */
    public val isFixedOffset: Boolean
        get() = standardOffsets[0] == wallOffsets[0] &&
            standardTransitions.isEmpty() &&
            savingsInstantTransitions.isEmpty() &&
            lastRules.isEmpty()

    /**
     * Returns the offset applicable at [instant].
     *
     * [instant] may be `null` when these rules have a single offset for every
     * instant, because the value is then never consulted; variable rules require
     * a value, as in Java.
     */
    public fun getOffset(instant: Instant?): ZoneOffset {
        if (savingsInstantTransitions.isEmpty()) return wallOffsets[0]
        val epochSecond = instant!!.epochSecond
        if (lastRules.isNotEmpty() && epochSecond > savingsInstantTransitions.last()) {
            val transitions = findTransitionArray(findYear(epochSecond, wallOffsets.last()))
            transitions.forEach { transition ->
                if (epochSecond < transition.toEpochSecond()) return transition.offsetBefore
            }
            return transitions.last().offsetAfter
        }

        var index = savingsInstantTransitions.binarySearch(epochSecond)
        if (index < 0) index = -index - 2
        return wallOffsets[index + 1]
    }

    /** Returns the best available offset for [localDateTime]. */
    public fun getOffset(localDateTime: LocalDateTime?): ZoneOffset =
        when (val info = getOffsetInfo(localDateTime)) {
            is ZoneOffsetTransition -> info.offsetBefore
            else -> info as ZoneOffset
        }

    /** Returns the valid offsets for [localDateTime]. */
    public fun getValidOffsets(localDateTime: LocalDateTime?): List<ZoneOffset> =
        when (val info = getOffsetInfo(localDateTime)) {
            is ZoneOffsetTransition ->
                if (info.isGap) emptyList() else listOf(info.offsetBefore, info.offsetAfter)
            else -> listOf(info as ZoneOffset)
        }

    /** Returns a transition affecting [localDateTime], or `null`. */
    public fun getTransition(localDateTime: LocalDateTime?): ZoneOffsetTransition? =
        getOffsetInfo(localDateTime) as? ZoneOffsetTransition

    /** Returns the standard offset applicable at [instant]. */
    public fun getStandardOffset(instant: Instant?): ZoneOffset {
        if (standardTransitions.isEmpty()) return standardOffsets[0]
        var index = standardTransitions.binarySearch(instant!!.epochSecond)
        if (index < 0) index = -index - 2
        return standardOffsets[index + 1]
    }

    /** Returns the daylight-saving adjustment applicable at [instant]. */
    public fun getDaylightSavings(instant: Instant?): Duration {
        if (isFixedOffset) return Duration.ZERO
        return Duration.ofSeconds(
            (getOffset(instant).totalSeconds - getStandardOffset(instant).totalSeconds).toLong(),
        )
    }

    /** Whether daylight saving is active at [instant]. */
    public fun isDaylightSavings(instant: Instant?): Boolean =
        getStandardOffset(instant) != getOffset(instant)

    /** Whether [offset] is valid for [localDateTime]. */
    public fun isValidOffset(localDateTime: LocalDateTime?, offset: ZoneOffset?): Boolean {
        // The offsets are resolved first so that variable rules reject a null
        // local date-time before the offset is examined, matching Java.
        val validOffsets = getValidOffsets(localDateTime)
        return offset != null && offset in validOffsets
    }

    /** Returns the next transition strictly after [instant]. */
    public fun nextTransition(instant: Instant?): ZoneOffsetTransition? {
        if (savingsInstantTransitions.isEmpty()) return null
        val epochSecond = instant!!.epochSecond
        if (epochSecond >= savingsInstantTransitions.last()) {
            if (lastRules.isEmpty()) return null
            val year = findYear(epochSecond, wallOffsets.last())
            findTransitionArray(year).forEach { transition ->
                if (epochSecond < transition.toEpochSecond()) return transition
            }
            return if (year < Year.MAX_VALUE) findTransitionArray(year + 1).first() else null
        }

        var index = savingsInstantTransitions.binarySearch(epochSecond)
        index = if (index < 0) -index - 1 else index + 1
        return transitionAt(index)
    }

    /** Returns the previous transition strictly before [instant]. */
    public fun previousTransition(instant: Instant?): ZoneOffsetTransition? {
        if (savingsInstantTransitions.isEmpty()) return null
        var epochSecond = instant!!.epochSecond
        if (instant.nano > 0 && epochSecond < Long.MAX_VALUE) epochSecond++

        val lastHistoric = savingsInstantTransitions.last()
        if (lastRules.isNotEmpty() && epochSecond > lastHistoric) {
            val lastHistoricOffset = wallOffsets.last()
            var year = findYear(epochSecond, lastHistoricOffset)
            val transitions = findTransitionArray(year)
            for (index in transitions.indices.reversed()) {
                if (epochSecond > transitions[index].toEpochSecond()) return transitions[index]
            }
            val lastHistoricYear = findYear(lastHistoric, lastHistoricOffset)
            year--
            if (year > lastHistoricYear) return findTransitionArray(year).last()
        }

        var index = savingsInstantTransitions.binarySearch(epochSecond)
        if (index < 0) index = -index - 1
        return if (index <= 0) null else transitionAt(index - 1)
    }

    /** Returns the complete historic transition list. */
    public fun getTransitions(): List<ZoneOffsetTransition> =
        savingsInstantTransitions.indices.map(::transitionAt)

    /** Returns the recurring transition rules used beyond the historic list. */
    public fun getTransitionRules(): List<ZoneOffsetTransitionRule> = lastRules

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ZoneRules &&
            standardTransitions == other.standardTransitions &&
            standardOffsets == other.standardOffsets &&
            savingsInstantTransitions == other.savingsInstantTransitions &&
            wallOffsets == other.wallOffsets &&
            lastRules == other.lastRules

    override fun hashCode(): Int =
        standardTransitions.hashCode() xor
            standardOffsets.hashCode() xor
            savingsInstantTransitions.hashCode() xor
            wallOffsets.hashCode() xor
            lastRules.hashCode()

    override fun toString(): String =
        "ZoneRules[currentStandardOffset=${standardOffsets.last()}]"

    private fun getOffsetInfo(localDateTime: LocalDateTime?): Any {
        if (savingsLocalTransitions.isEmpty()) return wallOffsets[0]
        val dateTime = localDateTime!!
        if (lastRules.isNotEmpty() && dateTime.isAfter(savingsLocalTransitions.last())) {
            var info: Any = wallOffsets.last()
            findTransitionArray(dateTime.year).forEach { transition ->
                info = findOffsetInfo(dateTime, transition)
                if (info is ZoneOffsetTransition || info == transition.offsetBefore) return info
            }
            return info
        }

        var index = savingsLocalTransitions.binarySearch(dateTime)
        if (index == -1) return wallOffsets[0]
        if (index < 0) {
            index = -index - 2
        } else if (
            index < savingsLocalTransitions.lastIndex &&
            savingsLocalTransitions[index] == savingsLocalTransitions[index + 1]
        ) {
            index++
        }
        if (index and 1 == 0) {
            val dateTimeBefore = savingsLocalTransitions[index]
            val dateTimeAfter = savingsLocalTransitions[index + 1]
            val offsetBefore = wallOffsets[index / 2]
            val offsetAfter = wallOffsets[index / 2 + 1]
            return if (offsetAfter.totalSeconds > offsetBefore.totalSeconds) {
                ZoneOffsetTransition(dateTimeBefore, offsetBefore, offsetAfter)
            } else {
                ZoneOffsetTransition(dateTimeAfter, offsetBefore, offsetAfter)
            }
        }
        return wallOffsets[index / 2 + 1]
    }

    private fun findOffsetInfo(
        dateTime: LocalDateTime,
        transition: ZoneOffsetTransition,
    ): Any {
        val localTransition = transition.dateTimeBefore
        return if (transition.isGap) {
            when {
                dateTime.isBefore(localTransition) -> transition.offsetBefore
                dateTime.isBefore(transition.dateTimeAfter) -> transition
                else -> transition.offsetAfter
            }
        } else {
            when {
                !dateTime.isBefore(localTransition) -> transition.offsetAfter
                dateTime.isBefore(transition.dateTimeAfter) -> transition.offsetBefore
                else -> transition
            }
        }
    }

    private fun findTransitionArray(year: Int): List<ZoneOffsetTransition> =
        lastRules.map { rule -> rule.createTransition(year) }

    private fun transitionAt(index: Int): ZoneOffsetTransition = ZoneOffsetTransition(
        LocalDateTime.ofEpochSecond(
            savingsInstantTransitions[index],
            0,
            wallOffsets[index],
        ),
        wallOffsets[index],
        wallOffsets[index + 1],
    )

    private fun findYear(epochSecond: Long, offset: ZoneOffset): Int {
        val localSecond = epochSecond + offset.totalSeconds
        var zeroDay = floorDiv(localSecond, SECONDS_PER_DAY) + DAYS_0000_TO_1970
        zeroDay -= 60
        var adjustment = 0L
        if (zeroDay < 0) {
            val adjustmentCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
            adjustment = adjustmentCycles * 400
            zeroDay += -adjustmentCycles * DAYS_PER_CYCLE
        }
        var yearEstimate = (400 * zeroDay + 591) / DAYS_PER_CYCLE
        var dayOfYearEstimate = zeroDay - (
            365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400
        )
        if (dayOfYearEstimate < 0) {
            yearEstimate--
            dayOfYearEstimate = zeroDay - (
                365 * yearEstimate + yearEstimate / 4 - yearEstimate / 100 + yearEstimate / 400
            )
        }
        yearEstimate += adjustment
        if (dayOfYearEstimate >= 306) yearEstimate++
        return minOf(yearEstimate, Year.MAX_VALUE.toLong()).toInt()
    }

    public companion object {
        private const val DAYS_PER_CYCLE: Long = 146_097
        private const val DAYS_0000_TO_1970: Long = 719_528
        private const val SECONDS_PER_DAY: Long = 86_400

        /** Creates rules from historic standard/wall transitions and recurring future rules. */
        public fun of(
            baseStandardOffset: ZoneOffset,
            baseWallOffset: ZoneOffset,
            standardOffsetTransitionList: List<ZoneOffsetTransition>,
            transitionList: List<ZoneOffsetTransition>,
            lastRules: List<ZoneOffsetTransitionRule>,
        ): ZoneRules {
            require(lastRules.size <= 16) { "Too many transition rules" }
            val standardTransitions = standardOffsetTransitionList.map { it.toEpochSecond() }
            val standardOffsets = buildList {
                add(baseStandardOffset)
                standardOffsetTransitionList.forEach { add(it.offsetAfter) }
            }
            val savingsLocalTransitions = buildList {
                transitionList.forEach { transition ->
                    if (transition.isGap) {
                        add(transition.dateTimeBefore)
                        add(transition.dateTimeAfter)
                    } else {
                        add(transition.dateTimeAfter)
                        add(transition.dateTimeBefore)
                    }
                }
            }
            val wallOffsets = buildList {
                add(baseWallOffset)
                transitionList.forEach { add(it.offsetAfter) }
            }
            return ZoneRules(
                standardTransitions,
                standardOffsets,
                transitionList.map { it.toEpochSecond() },
                savingsLocalTransitions,
                wallOffsets,
                lastRules.toList(),
            )
        }

        /** Creates rules for a fixed [offset]. */
        public fun of(offset: ZoneOffset): ZoneRules = ZoneRules(
            emptyList(),
            listOf(offset),
            emptyList(),
            emptyList(),
            listOf(offset),
            emptyList(),
        )
    }
}
