package io.heapy.grogu.time.format

import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalField

internal actual fun localeTextValues(
    languageTag: String,
    chronologyId: String,
    field: LocaleTextField,
    style: TextStyle,
): List<LocaleTextValue> {
    val javaField = field.toJavaField()
    val formatter = java.time.format.DateTimeFormatterBuilder()
        .appendText(javaField, java.time.format.TextStyle.valueOf(style.name))
        .toFormatter(java.util.Locale.forLanguageTag(languageTag))
    return field.values().map { value ->
        LocaleTextValue(value, formatter.format(SingleFieldTemporal(javaField, value)))
    }
}

private fun LocaleTextField.toJavaField(): TemporalField = when (this) {
    LocaleTextField.ERA -> java.time.temporal.ChronoField.ERA
    LocaleTextField.MONTH_OF_YEAR -> java.time.temporal.ChronoField.MONTH_OF_YEAR
    LocaleTextField.DAY_OF_WEEK -> java.time.temporal.ChronoField.DAY_OF_WEEK
    LocaleTextField.AMPM_OF_DAY -> java.time.temporal.ChronoField.AMPM_OF_DAY
    LocaleTextField.QUARTER_OF_YEAR -> java.time.temporal.IsoFields.QUARTER_OF_YEAR
}

private fun LocaleTextField.values(): LongRange = when (this) {
    LocaleTextField.ERA,
    LocaleTextField.AMPM_OF_DAY,
    -> 0L..1L
    LocaleTextField.MONTH_OF_YEAR -> 1L..12L
    LocaleTextField.DAY_OF_WEEK -> 1L..7L
    LocaleTextField.QUARTER_OF_YEAR -> 1L..4L
}

private class SingleFieldTemporal(
    private val field: TemporalField,
    private val value: Long,
) : TemporalAccessor {
    override fun isSupported(field: TemporalField): Boolean = field == this.field

    override fun getLong(field: TemporalField): Long =
        if (field == this.field) value else throw java.time.DateTimeException("Unsupported field: $field")
}
