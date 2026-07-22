package io.heapy.grogu.time.temporal

internal actual fun localizedFieldDisplayName(
    languageTag: String,
    displayNameKey: String,
): String? {
    val field = when (displayNameKey) {
        "second" -> java.time.temporal.ChronoField.SECOND_OF_MINUTE
        "minute" -> java.time.temporal.ChronoField.MINUTE_OF_HOUR
        "hour" -> java.time.temporal.ChronoField.HOUR_OF_DAY
        "dayperiod" -> java.time.temporal.ChronoField.AMPM_OF_DAY
        "weekday" -> java.time.temporal.ChronoField.DAY_OF_WEEK
        "day" -> java.time.temporal.ChronoField.DAY_OF_MONTH
        "month" -> java.time.temporal.ChronoField.MONTH_OF_YEAR
        "year" -> java.time.temporal.ChronoField.YEAR
        "era" -> java.time.temporal.ChronoField.ERA
        "week" -> java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR
        else -> return null
    }
    return field.getDisplayName(java.util.Locale.forLanguageTag(languageTag))
}
