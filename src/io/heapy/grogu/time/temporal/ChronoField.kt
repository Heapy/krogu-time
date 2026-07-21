package io.heapy.grogu.time.temporal

/** The standard set of date-time fields. */
public enum class ChronoField(
    private val displayName: String,
    override val baseUnit: ChronoUnit,
    override val rangeUnit: ChronoUnit,
    override val range: ValueRange,
) : TemporalField {
    NANO_OF_SECOND(
        "NanoOfSecond",
        ChronoUnit.NANOS,
        ChronoUnit.SECONDS,
        ValueRange.of(0, 999_999_999),
    ),
    NANO_OF_DAY(
        "NanoOfDay",
        ChronoUnit.NANOS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 86_399_999_999_999),
    ),
    MICRO_OF_SECOND(
        "MicroOfSecond",
        ChronoUnit.MICROS,
        ChronoUnit.SECONDS,
        ValueRange.of(0, 999_999),
    ),
    MICRO_OF_DAY(
        "MicroOfDay",
        ChronoUnit.MICROS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 86_399_999_999),
    ),
    MILLI_OF_SECOND(
        "MilliOfSecond",
        ChronoUnit.MILLIS,
        ChronoUnit.SECONDS,
        ValueRange.of(0, 999),
    ),
    MILLI_OF_DAY(
        "MilliOfDay",
        ChronoUnit.MILLIS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 86_399_999),
    ),
    SECOND_OF_MINUTE(
        "SecondOfMinute",
        ChronoUnit.SECONDS,
        ChronoUnit.MINUTES,
        ValueRange.of(0, 59),
    ),
    SECOND_OF_DAY(
        "SecondOfDay",
        ChronoUnit.SECONDS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 86_399),
    ),
    MINUTE_OF_HOUR(
        "MinuteOfHour",
        ChronoUnit.MINUTES,
        ChronoUnit.HOURS,
        ValueRange.of(0, 59),
    ),
    MINUTE_OF_DAY(
        "MinuteOfDay",
        ChronoUnit.MINUTES,
        ChronoUnit.DAYS,
        ValueRange.of(0, 1_439),
    ),
    HOUR_OF_AMPM(
        "HourOfAmPm",
        ChronoUnit.HOURS,
        ChronoUnit.HALF_DAYS,
        ValueRange.of(0, 11),
    ),
    CLOCK_HOUR_OF_AMPM(
        "ClockHourOfAmPm",
        ChronoUnit.HOURS,
        ChronoUnit.HALF_DAYS,
        ValueRange.of(1, 12),
    ),
    HOUR_OF_DAY(
        "HourOfDay",
        ChronoUnit.HOURS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 23),
    ),
    CLOCK_HOUR_OF_DAY(
        "ClockHourOfDay",
        ChronoUnit.HOURS,
        ChronoUnit.DAYS,
        ValueRange.of(1, 24),
    ),
    AMPM_OF_DAY(
        "AmPmOfDay",
        ChronoUnit.HALF_DAYS,
        ChronoUnit.DAYS,
        ValueRange.of(0, 1),
    ),
    DAY_OF_WEEK(
        "DayOfWeek",
        ChronoUnit.DAYS,
        ChronoUnit.WEEKS,
        ValueRange.of(1, 7),
    ),
    ALIGNED_DAY_OF_WEEK_IN_MONTH(
        "AlignedDayOfWeekInMonth",
        ChronoUnit.DAYS,
        ChronoUnit.WEEKS,
        ValueRange.of(1, 7),
    ),
    ALIGNED_DAY_OF_WEEK_IN_YEAR(
        "AlignedDayOfWeekInYear",
        ChronoUnit.DAYS,
        ChronoUnit.WEEKS,
        ValueRange.of(1, 7),
    ),
    DAY_OF_MONTH(
        "DayOfMonth",
        ChronoUnit.DAYS,
        ChronoUnit.MONTHS,
        ValueRange.of(1, 28, 31),
    ),
    DAY_OF_YEAR(
        "DayOfYear",
        ChronoUnit.DAYS,
        ChronoUnit.YEARS,
        ValueRange.of(1, 365, 366),
    ),
    EPOCH_DAY(
        "EpochDay",
        ChronoUnit.DAYS,
        ChronoUnit.FOREVER,
        ValueRange.of(-365_243_219_162, 365_241_780_471),
    ),
    ALIGNED_WEEK_OF_MONTH(
        "AlignedWeekOfMonth",
        ChronoUnit.WEEKS,
        ChronoUnit.MONTHS,
        ValueRange.of(1, 4, 5),
    ),
    ALIGNED_WEEK_OF_YEAR(
        "AlignedWeekOfYear",
        ChronoUnit.WEEKS,
        ChronoUnit.YEARS,
        ValueRange.of(1, 53),
    ),
    MONTH_OF_YEAR(
        "MonthOfYear",
        ChronoUnit.MONTHS,
        ChronoUnit.YEARS,
        ValueRange.of(1, 12),
    ),
    PROLEPTIC_MONTH(
        "ProlepticMonth",
        ChronoUnit.MONTHS,
        ChronoUnit.FOREVER,
        ValueRange.of(-11_999_999_988, 11_999_999_999),
    ),
    YEAR_OF_ERA(
        "YearOfEra",
        ChronoUnit.YEARS,
        ChronoUnit.FOREVER,
        ValueRange.of(1, 999_999_999, 1_000_000_000),
    ),
    YEAR(
        "Year",
        ChronoUnit.YEARS,
        ChronoUnit.FOREVER,
        ValueRange.of(-999_999_999, 999_999_999),
    ),
    ERA(
        "Era",
        ChronoUnit.ERAS,
        ChronoUnit.FOREVER,
        ValueRange.of(0, 1),
    ),
    INSTANT_SECONDS(
        "InstantSeconds",
        ChronoUnit.SECONDS,
        ChronoUnit.FOREVER,
        ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE),
    ),
    OFFSET_SECONDS(
        "OffsetSeconds",
        ChronoUnit.SECONDS,
        ChronoUnit.FOREVER,
        ValueRange.of(-64_800, 64_800),
    );

    override val isDateBased: Boolean
        get() = this >= DAY_OF_WEEK && this <= ERA

    override val isTimeBased: Boolean
        get() = this < DAY_OF_WEEK

    override fun isSupportedBy(temporal: TemporalAccessor): Boolean = temporal.isSupported(this)

    override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = temporal.range(this)

    override fun getFrom(temporal: TemporalAccessor): Long = temporal.getLong(this)

    override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R {
        @Suppress("UNCHECKED_CAST")
        return temporal.with(this, newValue) as R
    }

    /** Validates [value] against this field's outer range. */
    public fun checkValidValue(value: Long): Long = range.checkValidValue(value, this)

    /** Validates [value] as an integer against this field's outer range. */
    public fun checkValidIntValue(value: Long): Int = range.checkValidIntValue(value, this)

    override fun toString(): String = displayName
}
