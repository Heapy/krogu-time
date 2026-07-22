package io.heapy.grogu.time.format

internal enum class LocaleTextField {
    ERA,
    MONTH_OF_YEAR,
    DAY_OF_WEEK,
    AMPM_OF_DAY,
    QUARTER_OF_YEAR,
}

internal data class LocaleTextValue(
    val value: Long,
    val text: String,
)

internal expect fun localeTextValues(
    languageTag: String,
    chronologyId: String,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue>
