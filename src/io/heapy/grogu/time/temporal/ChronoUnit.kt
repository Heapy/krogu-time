package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.Duration

/** The standard set of date-time units. */
public enum class ChronoUnit(
    override val duration: Duration,
    private val displayName: String,
) : TemporalUnit {
    NANOS(Duration.ofNanos(1), "Nanos"),
    MICROS(Duration.ofNanos(1_000), "Micros"),
    MILLIS(Duration.ofNanos(1_000_000), "Millis"),
    SECONDS(Duration.ofSeconds(1), "Seconds"),
    MINUTES(Duration.ofSeconds(60), "Minutes"),
    HOURS(Duration.ofSeconds(3_600), "Hours"),
    HALF_DAYS(Duration.ofSeconds(43_200), "HalfDays"),
    DAYS(Duration.ofSeconds(86_400), "Days"),
    WEEKS(Duration.ofSeconds(604_800), "Weeks"),
    MONTHS(Duration.ofSeconds(2_629_746), "Months"),
    YEARS(Duration.ofSeconds(31_556_952), "Years"),
    DECADES(Duration.ofSeconds(315_569_520), "Decades"),
    CENTURIES(Duration.ofSeconds(3_155_695_200), "Centuries"),
    MILLENNIA(Duration.ofSeconds(31_556_952_000), "Millennia"),
    ERAS(Duration.ofSeconds(31_556_952_000_000_000), "Eras"),
    FOREVER(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), "Forever");

    override val isDurationEstimated: Boolean
        get() = this >= DAYS

    override val isDateBased: Boolean
        get() = this >= DAYS && this != FOREVER

    override val isTimeBased: Boolean
        get() = this <= HALF_DAYS

    override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
        @Suppress("UNCHECKED_CAST")
        return temporal.plus(amount, this) as R
    }

    override fun between(
        temporal1Inclusive: Temporal,
        temporal2Exclusive: Temporal,
    ): Long = temporal1Inclusive.until(temporal2Exclusive, this)

    override fun toString(): String = displayName
}
