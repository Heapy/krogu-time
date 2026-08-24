package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.Locale

internal actual fun localizedFieldDisplayName(
    languageTag: String,
    displayNameKey: String,
): String? {
    if (displayNameKey == "day") return null
    val language = Locale.forLanguageTag(languageTag).language
    val names = FIELD_NAMES[language] ?: ROOT_FIELD_NAMES
    return names[displayNameKey]
}

private val ROOT_FIELD_NAMES: Map<String, String> = mapOf(
    "second" to "Second",
    "minute" to "Minute",
    "hour" to "Hour",
    "dayperiod" to "Dayperiod",
    "weekday" to "Day of the Week",
    "month" to "Month",
    "year" to "Year",
    "era" to "Era",
    "week" to "Week",
)

private val FIELD_NAMES: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "second" to "second",
        "minute" to "minute",
        "hour" to "hour",
        "dayperiod" to "AM/PM",
        "weekday" to "day of the week",
        "month" to "month",
        "year" to "year",
        "era" to "era",
        "week" to "week",
    ),
    "fr" to mapOf(
        "second" to "seconde",
        "minute" to "minute",
        "hour" to "heure",
        "dayperiod" to "cadran",
        "weekday" to "jour de la semaine",
        "month" to "mois",
        "year" to "année",
        "era" to "ère",
        "week" to "semaine",
    ),
    "de" to mapOf(
        "second" to "Sekunde",
        "minute" to "Minute",
        "hour" to "Stunde",
        "dayperiod" to "Tageshälfte",
        "weekday" to "Wochentag",
        "month" to "Monat",
        "year" to "Jahr",
        "era" to "Epoche",
        "week" to "Woche",
    ),
    "ru" to mapOf(
        "second" to "секунда",
        "minute" to "минута",
        "hour" to "час",
        "dayperiod" to "AM/PM",
        "weekday" to "день недели",
        "month" to "месяц",
        "year" to "год",
        "era" to "эра",
        "week" to "неделя",
    ),
    "ja" to mapOf(
        "second" to "秒",
        "minute" to "分",
        "hour" to "時",
        "dayperiod" to "午前/午後",
        "weekday" to "曜日",
        "month" to "月",
        "year" to "年",
        "era" to "時代",
        "week" to "週",
    ),
    "ar" to mapOf(
        "second" to "الثواني",
        "minute" to "الدقائق",
        "hour" to "الساعات",
        "dayperiod" to "ص/م",
        "weekday" to "اليوم",
        "month" to "الشهر",
        "year" to "السنة",
        "era" to "العصر",
        "week" to "الأسبوع",
    ),
)
