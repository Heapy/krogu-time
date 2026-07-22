package io.heapy.grogu.time.temporal

internal expect fun localizedFieldDisplayName(
    languageTag: String,
    displayNameKey: String,
): String?
