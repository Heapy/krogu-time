package io.heapy.grogu.time.format

internal actual fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    dateStyle: FormatStyle?,
    timeStyle: FormatStyle?,
): String = java.time.format.DateTimeFormatterBuilder.getLocalizedDateTimePattern(
    dateStyle?.let { java.time.format.FormatStyle.valueOf(it.name) },
    timeStyle?.let { java.time.format.FormatStyle.valueOf(it.name) },
    java.time.chrono.Chronology.of(chronologyId),
    java.util.Locale.forLanguageTag(languageTag),
)
