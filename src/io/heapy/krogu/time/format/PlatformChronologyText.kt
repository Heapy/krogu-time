package io.heapy.krogu.time.format

internal expect fun platformChronologyText(
    languageTag: String,
    chronologyId: String,
    calendarType: String?,
): String?

internal fun localizedChronologyText(
    languageTag: String,
    chronologyId: String,
    calendarType: String?,
): String = platformChronologyText(languageTag, chronologyId, calendarType) ?: chronologyId
