package io.heapy.grogu.time

internal actual fun defaultFormatLocaleTag(): String =
    java.util.Locale.getDefault(java.util.Locale.Category.FORMAT).toLanguageTag()

internal actual fun localeWeekRules(languageTag: String): LocaleWeekRules {
    val fields = java.time.temporal.WeekFields.of(java.util.Locale.forLanguageTag(languageTag))
    return LocaleWeekRules(
        firstDayOfWeek = fields.firstDayOfWeek.value,
        minimalDaysInFirstWeek = fields.minimalDaysInFirstWeek,
    )
}

internal actual fun localeDecimalSymbols(languageTag: String): LocaleDecimalSymbols {
    val symbols = java.text.DecimalFormatSymbols.getInstance(
        java.util.Locale.forLanguageTag(languageTag),
    )
    return LocaleDecimalSymbols(
        zeroDigit = symbols.zeroDigit,
        negativeSign = symbols.minusSign,
        decimalSeparator = symbols.decimalSeparator,
    )
}

internal actual fun availableFormatLocaleTags(): Set<String> =
    java.text.DecimalFormatSymbols.getAvailableLocales()
        .mapTo(mutableSetOf(), java.util.Locale::toLanguageTag)
