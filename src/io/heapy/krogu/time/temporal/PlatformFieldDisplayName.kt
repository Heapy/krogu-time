package io.heapy.krogu.time.temporal

internal expect fun localizedFieldDisplayName(
    languageTag: String,
    displayNameKey: String,
): String?
