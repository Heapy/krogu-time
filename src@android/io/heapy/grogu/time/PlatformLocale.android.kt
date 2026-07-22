package io.heapy.grogu.time

import android.os.Build

internal actual fun defaultFormatLocaleTag(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        java.util.Locale.getDefault(java.util.Locale.Category.FORMAT).toLanguageTag()
    } else {
        java.util.Locale.getDefault().toLanguageTag()
    }

internal actual fun localeWeekRules(languageTag: String): LocaleWeekRules {
    val calendar = java.util.Calendar.getInstance(java.util.Locale.forLanguageTag(languageTag))
    return LocaleWeekRules(
        firstDayOfWeek = (calendar.firstDayOfWeek + 5) % 7 + 1,
        minimalDaysInFirstWeek = calendar.minimalDaysInFirstWeek,
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
