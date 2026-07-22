package io.heapy.grogu.time.format

internal expect fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    dateStyle: FormatStyle?,
    timeStyle: FormatStyle?,
): String
