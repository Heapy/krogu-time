package io.heapy.grogu.time.format

import android.os.Build
import java.util.Locale

internal actual fun platformChronologyText(
    languageTag: String,
    chronologyId: String,
    calendarType: String?,
): String? = when {
    chronologyId == "ISO" || calendarType == null -> null
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> chronologyTextFromIcu(
        languageTag,
        calendarType,
    )
    else -> null
}

@android.annotation.TargetApi(Build.VERSION_CODES.N)
private fun chronologyTextFromIcu(languageTag: String, calendarType: String): String? =
    android.icu.text.LocaleDisplayNames
        .getInstance(Locale.forLanguageTag(languageTag))
        .keyValueDisplayName("calendar", calendarType)
        .takeUnless { name -> name.isBlank() || name == calendarType }
