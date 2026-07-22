package io.heapy.grogu.time

internal expect fun defaultFormatLocaleTag(): String

internal data class LocaleWeekRules(
    val firstDayOfWeek: Int,
    val minimalDaysInFirstWeek: Int,
)

internal expect fun localeWeekRules(languageTag: String): LocaleWeekRules
