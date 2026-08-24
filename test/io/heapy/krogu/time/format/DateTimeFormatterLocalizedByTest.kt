package io.heapy.krogu.time.format

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import io.heapy.krogu.time.chrono.IsoChronology
import io.heapy.krogu.time.chrono.JapaneseChronology
import io.heapy.krogu.time.chrono.MinguoChronology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DateTimeFormatterLocalizedByTest {
    @Test
    fun localeExtensionsSupersedeFormatterOverrides() {
        val customStyle = DecimalStyle.STANDARD.withZeroDigit('\u0966')
        val formatter = DateTimeFormatter.ofPattern("G y-MM-dd HH:mm XXX", Locale.US)
            .withChronology(MinguoChronology)
            .withDecimalStyle(customStyle)
            .withZone(ZoneOffset.UTC)
        val locale = Locale.forLanguageTag(
            "en-US-u-ca-japanese-nu-arab-tz-usnyc",
        )

        val localized = formatter.localizedBy(locale)

        assertNotSame(formatter, localized)
        assertEquals(locale, localized.locale)
        assertSame(JapaneseChronology, localized.chronology)
        assertEquals(DecimalStyle.of(locale), localized.decimalStyle)
        assertEquals(ZoneId.of("America/New_York"), localized.zone)
        assertSame(localized, localized.localizedBy(locale))
        assertEquals(
            "Reiwa \u0666-\u0660\u0662-\u0662\u0668 \u0661\u0669:\u0660\u0660 -\u0660\u0665:\u0660\u0660",
            localized.format(Instant.parse("2024-02-29T00:00:00Z")),
        )
    }

    @Test
    fun localeWithoutTimezoneKeepsExistingZoneButResetsOtherLocalizedValues() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(MinguoChronology)
            .withDecimalStyle(DecimalStyle.STANDARD.withZeroDigit('\u0660'))
            .withZone(ZoneOffset.UTC)
        val locale = Locale.UK

        val localized = formatter.localizedBy(locale)

        assertSame(IsoChronology, localized.chronology)
        assertEquals(DecimalStyle.of(locale), localized.decimalStyle)
        assertSame(ZoneOffset.UTC, localized.zone)
        assertEquals(locale, localized.locale)
    }

    @Test
    fun withLocaleDoesNotApplyUnicodeOverrides() {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)
        val locale = Locale.forLanguageTag("en-US-u-ca-japanese-nu-arab-tz-usnyc")

        val changed = formatter.withLocale(locale)

        assertSame(IsoChronology, changed.chronology)
        assertSame(DecimalStyle.STANDARD, changed.decimalStyle)
        assertSame(ZoneOffset.UTC, changed.zone)
    }

    @Test
    fun resolvesUnicodeTimezoneShortIdsAcrossPlatforms() {
        mapOf(
            "en-US-u-tz-usnyc" to "America/New_York",
            "en-GB-u-tz-gblon" to "Europe/London",
            "th-TH-u-tz-thbkk" to "Asia/Bangkok",
            "en-u-tz-utc" to "Etc/UTC",
        ).forEach { (tag, expectedZoneId) ->
            assertEquals(
                ZoneId.of(expectedZoneId),
                DateTimeFormatter.ISO_INSTANT
                    .localizedBy(Locale.forLanguageTag(tag))
                    .zone,
                tag,
            )
        }

        assertSame(
            ZoneOffset.UTC,
            DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .localizedBy(Locale.forLanguageTag("en-US-u-tz-unknown"))
                .zone,
        )
    }
}
