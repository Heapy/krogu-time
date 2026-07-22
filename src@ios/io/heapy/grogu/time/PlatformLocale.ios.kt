package io.heapy.grogu.time

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSLocale
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
