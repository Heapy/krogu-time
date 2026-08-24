package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.DayOfWeek
import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.internal.subtractExact
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import io.heapy.krogu.time.temporal.TemporalAdjusters
import io.heapy.krogu.time.temporal.TemporalField

internal fun resolveChronologyDate(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
    yearOfEraResolver: (MutableMap<TemporalField, Long>, ResolverStyle) -> ChronoLocalDate? =
        { values, style -> resolveYearOfEra(chronology, values, style) },
): ChronoLocalDate? {
    fieldValues.remove(ChronoField.EPOCH_DAY)?.let(chronology::dateEpochDay)?.let { return it }

    resolveProlepticMonth(chronology, fieldValues, resolverStyle)
    yearOfEraResolver(fieldValues, resolverStyle)?.let { return it }

    if (ChronoField.YEAR in fieldValues) {
        if (ChronoField.MONTH_OF_YEAR in fieldValues) {
            if (ChronoField.DAY_OF_MONTH in fieldValues) {
                return resolveYmd(chronology, fieldValues, resolverStyle)
            }
            if (ChronoField.ALIGNED_WEEK_OF_MONTH in fieldValues) {
                if (ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH in fieldValues) {
                    return resolveAlignedDayInMonth(chronology, fieldValues, resolverStyle)
                }
                if (ChronoField.DAY_OF_WEEK in fieldValues) {
                    return resolveDayOfWeekInMonth(chronology, fieldValues, resolverStyle)
                }
            }
        }
        if (ChronoField.DAY_OF_YEAR in fieldValues) {
            return resolveYearDay(chronology, fieldValues, resolverStyle)
        }
        if (ChronoField.ALIGNED_WEEK_OF_YEAR in fieldValues) {
            if (ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR in fieldValues) {
                return resolveAlignedDayInYear(chronology, fieldValues, resolverStyle)
            }
            if (ChronoField.DAY_OF_WEEK in fieldValues) {
                return resolveDayOfWeekInYear(chronology, fieldValues, resolverStyle)
            }
        }
    }
    return null
}

private fun resolveProlepticMonth(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
) {
    val prolepticMonth = fieldValues.remove(ChronoField.PROLEPTIC_MONTH) ?: return
    if (resolverStyle != ResolverStyle.LENIENT) {
        ChronoField.PROLEPTIC_MONTH.checkValidValue(prolepticMonth)
    }
    val date = chronology.dateNow()
        .with(ChronoField.DAY_OF_MONTH, 1)
        .with(ChronoField.PROLEPTIC_MONTH, prolepticMonth)
    fieldValues.addFieldValue(ChronoField.MONTH_OF_YEAR, date.getLong(ChronoField.MONTH_OF_YEAR))
    fieldValues.addFieldValue(ChronoField.YEAR, date.getLong(ChronoField.YEAR))
}

private fun resolveYearOfEra(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate? {
    val yearOfEraValue = fieldValues.remove(ChronoField.YEAR_OF_ERA)
    if (yearOfEraValue != null) {
        val eraValue = fieldValues.remove(ChronoField.ERA)
        val yearOfEra = if (resolverStyle == ResolverStyle.LENIENT) {
            yearOfEraValue.toIntExact()
        } else {
            chronology.range(ChronoField.YEAR_OF_ERA)
                .checkValidIntValue(yearOfEraValue, ChronoField.YEAR_OF_ERA)
        }
        when {
            eraValue != null -> {
                val era = chronology.eraOf(
                    chronology.range(ChronoField.ERA)
                        .checkValidIntValue(eraValue, ChronoField.ERA),
                )
                fieldValues.addFieldValue(
                    ChronoField.YEAR,
                    chronology.prolepticYear(era, yearOfEra).toLong(),
                )
            }
            ChronoField.YEAR in fieldValues -> {
                val year = chronology.range(ChronoField.YEAR)
                    .checkValidIntValue(fieldValues.getValue(ChronoField.YEAR), ChronoField.YEAR)
                val era = chronology.dateYearDay(year, 1).era
                fieldValues.addFieldValue(
                    ChronoField.YEAR,
                    chronology.prolepticYear(era, yearOfEra).toLong(),
                )
            }
            resolverStyle == ResolverStyle.STRICT -> {
                fieldValues[ChronoField.YEAR_OF_ERA] = yearOfEraValue
            }
            else -> {
                val year = chronology.eras().lastOrNull()
                    ?.let { era -> chronology.prolepticYear(era, yearOfEra) }
                    ?: yearOfEra
                fieldValues.addFieldValue(ChronoField.YEAR, year.toLong())
            }
        }
    } else if (ChronoField.ERA in fieldValues) {
        chronology.range(ChronoField.ERA)
            .checkValidValue(fieldValues.getValue(ChronoField.ERA), ChronoField.ERA)
    }
    return null
}

private fun resolveYmd(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val months = subtractExact(fieldValues.take(ChronoField.MONTH_OF_YEAR), 1)
        val days = subtractExact(fieldValues.take(ChronoField.DAY_OF_MONTH), 1)
        return chronology.date(year, 1, 1)
            .plus(months, ChronoUnit.MONTHS)
            .plus(days, ChronoUnit.DAYS)
    }
    val month = chronology.range(ChronoField.MONTH_OF_YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.MONTH_OF_YEAR), ChronoField.MONTH_OF_YEAR)
    val day = chronology.range(ChronoField.DAY_OF_MONTH)
        .checkValidIntValue(fieldValues.take(ChronoField.DAY_OF_MONTH), ChronoField.DAY_OF_MONTH)
    if (resolverStyle == ResolverStyle.SMART) {
        return try {
            chronology.date(year, month, day)
        } catch (_: DateTimeException) {
            chronology.date(year, month, 1).with(TemporalAdjusters.lastDayOfMonth())
        }
    }
    return chronology.date(year, month, day)
}

private fun resolveYearDay(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val days = subtractExact(fieldValues.take(ChronoField.DAY_OF_YEAR), 1)
        return chronology.dateYearDay(year, 1).plus(days, ChronoUnit.DAYS)
    }
    val dayOfYear = chronology.range(ChronoField.DAY_OF_YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.DAY_OF_YEAR), ChronoField.DAY_OF_YEAR)
    return chronology.dateYearDay(year, dayOfYear)
}

private fun resolveAlignedDayInMonth(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val months = subtractExact(fieldValues.take(ChronoField.MONTH_OF_YEAR), 1)
        val weeks = subtractExact(fieldValues.take(ChronoField.ALIGNED_WEEK_OF_MONTH), 1)
        val days = subtractExact(fieldValues.take(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), 1)
        return chronology.date(year, 1, 1)
            .plus(months, ChronoUnit.MONTHS)
            .plus(weeks, ChronoUnit.WEEKS)
            .plus(days, ChronoUnit.DAYS)
    }
    val month = chronology.range(ChronoField.MONTH_OF_YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.MONTH_OF_YEAR), ChronoField.MONTH_OF_YEAR)
    val week = chronology.range(ChronoField.ALIGNED_WEEK_OF_MONTH).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_WEEK_OF_MONTH),
        ChronoField.ALIGNED_WEEK_OF_MONTH,
    )
    val day = chronology.range(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH),
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH,
    )
    val date = chronology.date(year, month, 1).plus((week - 1L) * 7 + day - 1, ChronoUnit.DAYS)
    if (resolverStyle == ResolverStyle.STRICT && date.get(ChronoField.MONTH_OF_YEAR) != month) {
        throw DateTimeException("Strict mode rejected resolved date as it is in a different month")
    }
    return date
}

private fun resolveDayOfWeekInMonth(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val months = subtractExact(fieldValues.take(ChronoField.MONTH_OF_YEAR), 1)
        val weeks = subtractExact(fieldValues.take(ChronoField.ALIGNED_WEEK_OF_MONTH), 1)
        val dayOfWeek = subtractExact(fieldValues.take(ChronoField.DAY_OF_WEEK), 1)
        return resolveAligned(chronology.date(year, 1, 1), months, weeks, dayOfWeek)
    }
    val month = chronology.range(ChronoField.MONTH_OF_YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.MONTH_OF_YEAR), ChronoField.MONTH_OF_YEAR)
    val week = chronology.range(ChronoField.ALIGNED_WEEK_OF_MONTH).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_WEEK_OF_MONTH),
        ChronoField.ALIGNED_WEEK_OF_MONTH,
    )
    val dayOfWeek = chronology.range(ChronoField.DAY_OF_WEEK)
        .checkValidIntValue(fieldValues.take(ChronoField.DAY_OF_WEEK), ChronoField.DAY_OF_WEEK)
    val date = chronology.date(year, month, 1)
        .plus((week - 1L) * 7, ChronoUnit.DAYS)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
    if (resolverStyle == ResolverStyle.STRICT && date.get(ChronoField.MONTH_OF_YEAR) != month) {
        throw DateTimeException("Strict mode rejected resolved date as it is in a different month")
    }
    return date
}

private fun resolveAlignedDayInYear(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val weeks = subtractExact(fieldValues.take(ChronoField.ALIGNED_WEEK_OF_YEAR), 1)
        val days = subtractExact(fieldValues.take(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), 1)
        return chronology.dateYearDay(year, 1)
            .plus(weeks, ChronoUnit.WEEKS)
            .plus(days, ChronoUnit.DAYS)
    }
    val week = chronology.range(ChronoField.ALIGNED_WEEK_OF_YEAR).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_WEEK_OF_YEAR),
        ChronoField.ALIGNED_WEEK_OF_YEAR,
    )
    val day = chronology.range(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR),
        ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR,
    )
    val date = chronology.dateYearDay(year, 1).plus((week - 1L) * 7 + day - 1, ChronoUnit.DAYS)
    if (resolverStyle == ResolverStyle.STRICT && date.get(ChronoField.YEAR) != year) {
        throw DateTimeException("Strict mode rejected resolved date as it is in a different year")
    }
    return date
}

private fun resolveDayOfWeekInYear(
    chronology: Chronology,
    fieldValues: MutableMap<TemporalField, Long>,
    resolverStyle: ResolverStyle,
): ChronoLocalDate {
    val year = chronology.range(ChronoField.YEAR)
        .checkValidIntValue(fieldValues.take(ChronoField.YEAR), ChronoField.YEAR)
    if (resolverStyle == ResolverStyle.LENIENT) {
        val weeks = subtractExact(fieldValues.take(ChronoField.ALIGNED_WEEK_OF_YEAR), 1)
        val dayOfWeek = subtractExact(fieldValues.take(ChronoField.DAY_OF_WEEK), 1)
        return resolveAligned(chronology.dateYearDay(year, 1), 0, weeks, dayOfWeek)
    }
    val week = chronology.range(ChronoField.ALIGNED_WEEK_OF_YEAR).checkValidIntValue(
        fieldValues.take(ChronoField.ALIGNED_WEEK_OF_YEAR),
        ChronoField.ALIGNED_WEEK_OF_YEAR,
    )
    val dayOfWeek = chronology.range(ChronoField.DAY_OF_WEEK)
        .checkValidIntValue(fieldValues.take(ChronoField.DAY_OF_WEEK), ChronoField.DAY_OF_WEEK)
    val date = chronology.dateYearDay(year, 1)
        .plus((week - 1L) * 7, ChronoUnit.DAYS)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
    if (resolverStyle == ResolverStyle.STRICT && date.get(ChronoField.YEAR) != year) {
        throw DateTimeException("Strict mode rejected resolved date as it is in a different year")
    }
    return date
}

private fun resolveAligned(
    base: ChronoLocalDate,
    months: Long,
    weeks: Long,
    parsedDayOfWeek: Long,
): ChronoLocalDate {
    var date = base.plus(months, ChronoUnit.MONTHS).plus(weeks, ChronoUnit.WEEKS)
    var dayOfWeek = parsedDayOfWeek
    if (dayOfWeek > 7) {
        date = date.plus((dayOfWeek - 1) / 7, ChronoUnit.WEEKS)
        dayOfWeek = (dayOfWeek - 1) % 7 + 1
    } else if (dayOfWeek < 1) {
        date = date.plus(subtractExact(dayOfWeek, 7) / 7, ChronoUnit.WEEKS)
        dayOfWeek = (dayOfWeek + 6) % 7 + 1
    }
    return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek.toInt())))
}

private fun MutableMap<TemporalField, Long>.addFieldValue(field: ChronoField, value: Long) {
    val previous = this[field]
    if (previous != null && previous != value) {
        throw DateTimeException("Conflict found: $field $previous differs from $field $value")
    }
    this[field] = value
}

private fun MutableMap<TemporalField, Long>.take(field: ChronoField): Long =
    requireNotNull(remove(field)) { "Missing field: $field" }

private fun Long.toIntExact(): Int = toInt().also { converted ->
    if (converted.toLong() != this) throw ArithmeticException("integer overflow")
}
