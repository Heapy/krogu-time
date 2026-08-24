package io.heapy.krogu.time

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.availableLocaleIdentifiers
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

internal actual fun defaultFormatLocaleTag(): String =
    NSLocale.currentLocale.localeIdentifier
        .substringBefore('@')
        .replace('_', '-')

internal actual fun localeWeekRules(languageTag: String): LocaleWeekRules {
    val calendar = NSCalendar(NSCalendarIdentifierGregorian)
    calendar.setLocale(NSLocale(localeIdentifier = languageTag))
    return LocaleWeekRules(
        firstDayOfWeek = (calendar.firstWeekday().toInt() + 5) % 7 + 1,
        minimalDaysInFirstWeek = calendar.minimumDaysInFirstWeek().toInt(),
    )
}

internal actual fun localeDecimalSymbols(languageTag: String): LocaleDecimalSymbols {
    val formatter = NSNumberFormatter().apply {
        locale = NSLocale(localeIdentifier = languageTag)
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
    }
    return LocaleDecimalSymbols(
        zeroDigit = formatter.stringFromNumber(NSNumber(int = 0))
            ?.lastOrNull(Char::isDigit) ?: '0',
        negativeSign = formatter.minusSign.lastOrNull() ?: '-',
        decimalSeparator = formatter.decimalSeparator.firstOrNull() ?: '.',
    )
}

internal actual fun availableFormatLocaleTags(): Set<String> =
    NSLocale.availableLocaleIdentifiers
        .mapNotNull { it as? String }
        .mapTo(mutableSetOf()) { it.replace('_', '-') }
