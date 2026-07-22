package io.heapy.grogu.time.format

import android.os.Build
import java.text.DateFormatSymbols
import java.util.Locale

internal actual fun localeTextValues(
    languageTag: String,
    chronologyId: String,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    val locale = Locale.forLanguageTag(languageTag)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        localeTextValuesFromIcu(locale, field, style)
    } else {
        localeTextValuesFromJava(locale, field, style)
    }
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun localeTextValuesFromIcu(
    locale: Locale,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    val symbols = android.icu.text.DateFormatSymbols.getInstance(locale)
    val context = if (style.isStandalone) {
        android.icu.text.DateFormatSymbols.STANDALONE
    } else {
        android.icu.text.DateFormatSymbols.FORMAT
    }
    val width = when (style.asNormal()) {
        TextStyle.FULL -> android.icu.text.DateFormatSymbols.WIDE
        TextStyle.SHORT -> android.icu.text.DateFormatSymbols.ABBREVIATED
        TextStyle.NARROW -> android.icu.text.DateFormatSymbols.NARROW
        else -> error("Unreachable text style: $style")
    }
    return when (field) {
        LocaleTextField.ERA -> when (style.asNormal()) {
            TextStyle.FULL -> symbols.eraNames.toLocaleTextValues(0)
            TextStyle.NARROW -> symbols.narrowEras.toLocaleTextValues(0)
            else -> symbols.eras.toLocaleTextValues(0)
        }
        LocaleTextField.MONTH_OF_YEAR -> symbols.getMonths(context, width).toLocaleTextValues(1)
        LocaleTextField.DAY_OF_WEEK -> symbols.getWeekdays(context, width)
            .drop(1)
            .mapIndexedNotNull { index, text ->
                text.takeIf(String::isNotEmpty)?.let {
                    LocaleTextValue(((index + 6) % 7 + 1).toLong(), it)
                }
            }
        LocaleTextField.AMPM_OF_DAY -> symbols.amPmStrings.toLocaleTextValues(0)
        LocaleTextField.QUARTER_OF_YEAR -> symbols.getQuarters(context, width).toLocaleTextValues(1)
    }
}

private fun localeTextValuesFromJava(
    locale: Locale,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    val symbols = DateFormatSymbols.getInstance(locale)
    val normalStyle = style.asNormal()
    return when (field) {
        LocaleTextField.ERA -> symbols.eras.toLocaleTextValues(0).forStyle(normalStyle)
        LocaleTextField.MONTH_OF_YEAR -> {
            val values = if (normalStyle == TextStyle.FULL) symbols.months else symbols.shortMonths
            values.toLocaleTextValues(1).forStyle(normalStyle)
        }
        LocaleTextField.DAY_OF_WEEK -> {
            val values = if (normalStyle == TextStyle.FULL) symbols.weekdays else symbols.shortWeekdays
            values.drop(1).mapIndexedNotNull { index, text ->
                text.takeIf(String::isNotEmpty)?.let {
                    LocaleTextValue(((index + 6) % 7 + 1).toLong(), it)
                }
            }.forStyle(normalStyle)
        }
        LocaleTextField.AMPM_OF_DAY -> symbols.amPmStrings.toLocaleTextValues(0)
        LocaleTextField.QUARTER_OF_YEAR -> (1L..4L).map { value ->
            LocaleTextValue(value, if (normalStyle == TextStyle.NARROW) "$value" else "Q$value")
        }
    }
}

private fun Array<String>.toLocaleTextValues(firstValue: Int): List<LocaleTextValue> =
    mapIndexedNotNull { index, text ->
        text.takeIf(String::isNotEmpty)?.let { LocaleTextValue((index + firstValue).toLong(), it) }
    }

private fun List<LocaleTextValue>.forStyle(style: TextStyle): List<LocaleTextValue> =
    if (style == TextStyle.NARROW) {
        map { value -> value.copy(text = value.text.take(1)) }
    } else {
        this
    }
