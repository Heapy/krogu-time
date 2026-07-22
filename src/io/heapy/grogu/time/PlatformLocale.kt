package io.heapy.grogu.time

internal expect fun defaultFormatLocaleTag(): String

internal data class LocaleWeekRules(
    val firstDayOfWeek: Int,
    val minimalDaysInFirstWeek: Int,
)

internal expect fun localeWeekRules(languageTag: String): LocaleWeekRules

internal data class LocaleDecimalSymbols(
    val zeroDigit: Char,
    val negativeSign: Char,
    val decimalSeparator: Char,
)

internal expect fun localeDecimalSymbols(languageTag: String): LocaleDecimalSymbols

internal expect fun availableFormatLocaleTags(): Set<String>

internal fun localeTimeZoneId(languageTag: String): String? =
    Locale.forLanguageTag(languageTag)
        .getUnicodeLocaleType("tz")
        ?.let(UNICODE_TIME_ZONE_IDS::get)
