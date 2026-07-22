package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.chrono.IsoChronology
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId as JavaZoneId
import java.time.ZonedDateTime as JavaZonedDateTime
import java.time.chrono.IsoChronology as JavaIsoChronology
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import java.time.format.DateTimeFormatterBuilder as JavaDateTimeFormatterBuilder
import java.time.format.FormatStyle as JavaFormatStyle
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterLocalizedFactoryJavaConformanceTest {
    @Test
    fun localizedPatternsMatchJavaTimeAcrossLocalesAndStyles() {
        val locales = listOf("en-US", "en-GB", "fr-FR", "de-DE", "ja-JP", "ar-SA")

        locales.forEach { tag ->
            FormatStyle.entries.forEach { style ->
                val javaStyle = JavaFormatStyle.valueOf(style.name)
                assertEquals(
                    JavaDateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        javaStyle,
                        null,
                        JavaIsoChronology.INSTANCE,
                        JavaLocale.forLanguageTag(tag),
                    ),
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        style,
                        null,
                        IsoChronology,
                        Locale.forLanguageTag(tag),
                    ),
                    "$tag $style date",
                )
                assertEquals(
                    JavaDateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        null,
                        javaStyle,
                        JavaIsoChronology.INSTANCE,
                        JavaLocale.forLanguageTag(tag),
                    ),
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        null,
                        style,
                        IsoChronology,
                        Locale.forLanguageTag(tag),
                    ),
                    "$tag $style time",
                )
            }
        }
    }

    @Test
    fun localizedFactoriesFormatAndParseLikeJavaTime() {
        val javaDate = JavaLocalDate.of(2024, 2, 29)
        val date = LocalDate.of(2024, 2, 29)
        val javaDateTime = JavaLocalDateTime.of(2024, 2, 29, 15, 7, 9)
        val dateTime = LocalDateTime.of(date, LocalTime.of(15, 7, 9))
        val javaZoned = JavaZonedDateTime.of(javaDateTime, JavaZoneId.of("America/New_York"))
        val zoned = ZonedDateTime.of(dateTime, ZoneId.of("America/New_York"))
        val locales = listOf("en-US", "en-GB", "fr-FR", "de-DE", "ja-JP")

        locales.forEach { tag ->
            FormatStyle.entries.forEach { style ->
                val javaStyle = JavaFormatStyle.valueOf(style.name)
                val javaLocale = JavaLocale.forLanguageTag(tag)
                val locale = Locale.forLanguageTag(tag)

                assertFormatter(
                    JavaDateTimeFormatter.ofLocalizedDate(javaStyle).withLocale(javaLocale),
                    DateTimeFormatter.ofLocalizedDate(style).withLocale(locale),
                    javaDate,
                    date,
                    "$tag $style date",
                )
                assertFormatter(
                    JavaDateTimeFormatter.ofLocalizedTime(javaStyle).withLocale(javaLocale),
                    DateTimeFormatter.ofLocalizedTime(style).withLocale(locale),
                    javaZoned,
                    zoned,
                    "$tag $style time",
                )
                assertFormatter(
                    JavaDateTimeFormatter.ofLocalizedDateTime(javaStyle).withLocale(javaLocale),
                    DateTimeFormatter.ofLocalizedDateTime(style).withLocale(locale),
                    javaZoned,
                    zoned,
                    "$tag $style date-time",
                )
            }
        }
    }

    @Test
    fun builderLocalizedSectionsMatchJavaTime() {
        val combinations = listOf(
            FormatStyle.FULL to null,
            null to FormatStyle.SHORT,
            FormatStyle.LONG to FormatStyle.MEDIUM,
        )
        val javaTemporal = JavaZonedDateTime.of(
            2024,
            7,
            1,
            15,
            7,
            9,
            0,
            JavaZoneId.of("Europe/Paris"),
        )
        val temporal = ZonedDateTime.of(
            LocalDateTime.of(LocalDate.of(2024, 7, 1), LocalTime.of(15, 7, 9)),
            ZoneId.of("Europe/Paris"),
        )

        combinations.forEach { (dateStyle, timeStyle) ->
            val javaFormatter = JavaDateTimeFormatterBuilder()
                .appendLocalized(
                    dateStyle?.let { JavaFormatStyle.valueOf(it.name) },
                    timeStyle?.let { JavaFormatStyle.valueOf(it.name) },
                )
                .toFormatter(JavaLocale.FRANCE)
            val formatter = DateTimeFormatterBuilder()
                .appendLocalized(dateStyle, timeStyle)
                .toFormatter(Locale.forLanguageTag("fr-FR"))

            assertFormatter(javaFormatter, formatter, javaTemporal, temporal, "$dateStyle $timeStyle")
        }
    }

    private fun assertFormatter(
        javaFormatter: JavaDateTimeFormatter,
        formatter: DateTimeFormatter,
        javaTemporal: java.time.temporal.TemporalAccessor,
        temporal: io.heapy.grogu.time.temporal.TemporalAccessor,
        message: String,
    ) {
        val javaText = javaFormatter.format(javaTemporal)
        val text = formatter.format(temporal)
        assertEquals(javaFormatter.toString(), formatter.toString(), "$message description")
        assertEquals(javaText, text, message)

        val javaParsed = javaFormatter.parse(javaText)
        val parsed = formatter.parse(text)
        listOf(
            java.time.temporal.ChronoField.YEAR to io.heapy.grogu.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.MONTH_OF_YEAR to io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR,
            java.time.temporal.ChronoField.DAY_OF_MONTH to io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH,
            java.time.temporal.ChronoField.HOUR_OF_DAY to io.heapy.grogu.time.temporal.ChronoField.HOUR_OF_DAY,
            java.time.temporal.ChronoField.MINUTE_OF_HOUR to io.heapy.grogu.time.temporal.ChronoField.MINUTE_OF_HOUR,
            java.time.temporal.ChronoField.SECOND_OF_MINUTE to io.heapy.grogu.time.temporal.ChronoField.SECOND_OF_MINUTE,
        ).forEach { (javaField, field) ->
            if (javaParsed.isSupported(javaField)) {
                assertEquals(javaParsed.getLong(javaField), parsed.getLong(field), "$message $field")
            }
        }
        javaParsed.query(java.time.temporal.TemporalQueries.zone())?.let { javaZone ->
            assertEquals(
                javaZone.id,
                requireNotNull(parsed.query(io.heapy.grogu.time.temporal.TemporalQueries.zone())).id,
                "$message zone",
            )
        }
    }
}
