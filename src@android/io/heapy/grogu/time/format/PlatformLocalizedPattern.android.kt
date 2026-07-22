package io.heapy.grogu.time.format

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

internal actual fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    dateStyle: FormatStyle?,
    timeStyle: FormatStyle?,
): String {
    require(dateStyle != null || timeStyle != null) {
        "Either dateStyle or timeStyle must be non-null"
    }
    val locale = chronologyLocale(languageTag, chronologyId)
    val formatter = when {
        dateStyle != null && timeStyle != null -> DateFormat.getDateTimeInstance(
            dateStyle.toDateFormatStyle(),
            timeStyle.toDateFormatStyle(),
            locale,
        )
        dateStyle != null -> DateFormat.getDateInstance(dateStyle.toDateFormatStyle(), locale)
        else -> DateFormat.getTimeInstance(requireNotNull(timeStyle).toDateFormatStyle(), locale)
    }
    return (formatter as? SimpleDateFormat)?.toPattern()
        ?: error("Platform date formatter does not expose a pattern")
}

private fun FormatStyle.toDateFormatStyle(): Int = when (this) {
    FormatStyle.FULL -> DateFormat.FULL
    FormatStyle.LONG -> DateFormat.LONG
    FormatStyle.MEDIUM -> DateFormat.MEDIUM
    FormatStyle.SHORT -> DateFormat.SHORT
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
