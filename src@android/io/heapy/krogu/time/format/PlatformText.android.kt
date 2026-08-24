package io.heapy.krogu.time.format

import android.os.Build
import java.text.DateFormatSymbols
import java.util.Locale

internal actual fun localeTextValues(
    languageTag: String,
    chronologyId: String,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    if (field == LocaleTextField.ERA) {
        return localeEraTextValuesFromJavaTime(languageTag, chronologyId, style)
    }
    val locale = chronologyLocale(languageTag, chronologyId)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        localeTextValuesFromIcu(locale, field, style)
    } else {
        localeTextValuesFromJava(locale, field, style)
    }
}

private fun localeEraTextValuesFromJavaTime(
    languageTag: String,
    chronologyId: String,
    style: TextStyle,
): List<LocaleTextValue> {
    val chronology = java.time.chrono.Chronology.of(chronologyId)
    val field = java.time.temporal.ChronoField.ERA
    val formatter = java.time.format.DateTimeFormatterBuilder()
        .appendText(field, java.time.format.TextStyle.valueOf(style.name))
        .toFormatter(Locale.forLanguageTag(languageTag))
    return chronology.eras().map { era ->
        val value = era.value.toLong()
        LocaleTextValue(
            value,
            formatter.format(AndroidEraTemporal(value, chronology)),
        )
    }
}

private class AndroidEraTemporal(
    private val value: Long,
    private val chronology: java.time.chrono.Chronology,
) : java.time.temporal.TemporalAccessor {
    override fun isSupported(field: java.time.temporal.TemporalField): Boolean =
        field == java.time.temporal.ChronoField.ERA

    override fun getLong(field: java.time.temporal.TemporalField): Long =
        if (isSupported(field)) value else throw java.time.DateTimeException("Unsupported field: $field")

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any?> query(query: java.time.temporal.TemporalQuery<R>): R? =
        if (query === java.time.temporal.TemporalQueries.chronology()) {
            chronology as R
        } else {
            super.query(query)
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

private fun chronologyLocale(languageTag: String, chronologyId: String): Locale {
    val locale = Locale.forLanguageTag(languageTag)
    val calendarType = when (chronologyId) {
        "ISO" -> null
        "Japanese" -> "japanese"
        "Hijrah-umalqura" -> "islamic-umalqura"
        "Minguo" -> "roc"
        "ThaiBuddhist" -> "buddhist"
        else -> null
    }
    return calendarType?.let { type ->
        Locale.Builder()
            .setLocale(locale)
            .setUnicodeLocaleKeyword("ca", type)
            .build()
    } ?: locale
}
