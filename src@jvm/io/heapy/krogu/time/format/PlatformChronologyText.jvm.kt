package io.heapy.krogu.time.format

internal actual fun platformChronologyText(
    languageTag: String,
    chronologyId: String,
    calendarType: String?,
): String? = runCatching {
    java.time.chrono.Chronology.of(chronologyId).getDisplayName(
        java.time.format.TextStyle.FULL,
        java.util.Locale.forLanguageTag(languageTag),
    )
}.getOrNull()
