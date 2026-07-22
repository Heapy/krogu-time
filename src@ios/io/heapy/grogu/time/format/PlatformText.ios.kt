package io.heapy.grogu.time.format

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

internal actual fun localeTextValues(
    languageTag: String,
    chronologyId: String,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    val formatter = NSDateFormatter()
    formatter.setLocale(NSLocale(localeIdentifier = languageTag))
    return when (field) {
        LocaleTextField.ERA -> when (style.asNormal()) {
            TextStyle.FULL -> formatter.longEraSymbols().toLocaleTextValues(0)
            TextStyle.NARROW -> formatter.eraSymbols().toLocaleTextValues(0).narrowed()
            else -> formatter.eraSymbols().toLocaleTextValues(0)
        }
        LocaleTextField.MONTH_OF_YEAR -> formatter.monthSymbols(style).toLocaleTextValues(1)
        LocaleTextField.DAY_OF_WEEK -> formatter.weekdaySymbols(style)
            .mapIndexedNotNull { index, symbol ->
                (symbol as? String)?.let {
                    LocaleTextValue(((index + 6) % 7 + 1).toLong(), it)
                }
            }
        LocaleTextField.AMPM_OF_DAY -> listOf(
            LocaleTextValue(0, formatter.AMSymbol()),
            LocaleTextValue(1, formatter.PMSymbol()),
        )
        LocaleTextField.QUARTER_OF_YEAR -> formatter.quarterSymbols(style)
            .toLocaleTextValues(1)
    }
}

private fun NSDateFormatter.monthSymbols(style: TextStyle): List<*> = when (style) {
    TextStyle.FULL -> monthSymbols()
    TextStyle.FULL_STANDALONE -> standaloneMonthSymbols()
    TextStyle.SHORT -> shortMonthSymbols()
    TextStyle.SHORT_STANDALONE -> shortStandaloneMonthSymbols()
    TextStyle.NARROW -> veryShortMonthSymbols()
    TextStyle.NARROW_STANDALONE -> veryShortStandaloneMonthSymbols()
}

private fun NSDateFormatter.weekdaySymbols(style: TextStyle): List<*> = when (style) {
    TextStyle.FULL -> weekdaySymbols()
    TextStyle.FULL_STANDALONE -> standaloneWeekdaySymbols()
    TextStyle.SHORT -> shortWeekdaySymbols()
    TextStyle.SHORT_STANDALONE -> shortStandaloneWeekdaySymbols()
    TextStyle.NARROW -> veryShortWeekdaySymbols()
    TextStyle.NARROW_STANDALONE -> veryShortStandaloneWeekdaySymbols()
}

private fun NSDateFormatter.quarterSymbols(style: TextStyle): List<*> = when (style) {
    TextStyle.FULL -> quarterSymbols()
    TextStyle.FULL_STANDALONE -> standaloneQuarterSymbols()
    TextStyle.SHORT -> shortQuarterSymbols()
    TextStyle.SHORT_STANDALONE -> shortStandaloneQuarterSymbols()
    TextStyle.NARROW -> shortQuarterSymbols().map { (it as? String).orEmpty().takeLast(1) }
    TextStyle.NARROW_STANDALONE -> shortStandaloneQuarterSymbols()
        .map { (it as? String).orEmpty().takeLast(1) }
}

private fun List<*>.toLocaleTextValues(firstValue: Int): List<LocaleTextValue> =
    mapIndexedNotNull { index, symbol ->
        (symbol as? String)?.takeIf(String::isNotEmpty)?.let {
            LocaleTextValue((index + firstValue).toLong(), it)
        }
    }

private fun List<LocaleTextValue>.narrowed(): List<LocaleTextValue> =
    map { value -> value.copy(text = value.text.take(1)) }
