package io.heapy.krogu.time.format

import platform.Foundation.NSCalendarIdentifierBuddhist
import platform.Foundation.NSCalendarIdentifierIslamicUmmAlQura
import platform.Foundation.NSCalendarIdentifierJapanese
import platform.Foundation.NSCalendarIdentifierRepublicOfChina
import platform.Foundation.NSLocale
import platform.Foundation.localizedStringForCalendarIdentifier

internal actual fun platformChronologyText(
    languageTag: String,
    chronologyId: String,
    calendarType: String?,
): String? {
    val calendarIdentifier = when (calendarType) {
        "japanese" -> NSCalendarIdentifierJapanese
        "islamic-umalqura" -> NSCalendarIdentifierIslamicUmmAlQura
        "roc" -> NSCalendarIdentifierRepublicOfChina
        "buddhist" -> NSCalendarIdentifierBuddhist
        else -> return null
    } ?: return null
    return NSLocale(localeIdentifier = languageTag)
        .localizedStringForCalendarIdentifier(calendarIdentifier)
}
