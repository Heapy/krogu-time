package io.heapy.krogu.time.format

internal expect fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    dateStyle: FormatStyle?,
    timeStyle: FormatStyle?,
): String

internal expect fun localizedDateTimePattern(
    languageTag: String,
    chronologyId: String,
    requestedTemplate: String,
): String
