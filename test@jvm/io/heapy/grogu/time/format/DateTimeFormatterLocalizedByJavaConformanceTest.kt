package io.heapy.grogu.time.format

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.UNICODE_TIME_ZONE_IDS
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.chrono.MinguoChronology
import java.time.Instant as JavaInstant
import java.time.ZoneOffset as JavaZoneOffset
import java.time.chrono.MinguoChronology as JavaMinguoChronology
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterLocalizedByJavaConformanceTest {
    @Test
    fun localeExtensionOverridesAndFormattingMatchJavaTime() {
        val javaBase = JavaDateTimeFormatter.ofPattern("G y-MM-dd HH:mm:ss XXX", JavaLocale.US)
            .withChronology(JavaMinguoChronology.INSTANCE)
            .withDecimalStyle(java.time.format.DecimalStyle.STANDARD.withZeroDigit('\u0966'))
            .withZone(JavaZoneOffset.UTC)
        val base = DateTimeFormatter.ofPattern("G y-MM-dd HH:mm:ss XXX", Locale.US)
            .withChronology(MinguoChronology)
            .withDecimalStyle(DecimalStyle.STANDARD.withZeroDigit('\u0966'))
            .withZone(ZoneOffset.UTC)
        val javaInstant = JavaInstant.parse("2024-02-29T00:00:00Z")
        val instant = Instant.parse("2024-02-29T00:00:00Z")

        listOf(
            "en-US",
            "en-US-u-ca-japanese",
            "en-US-u-nu-arab",
            "en-US-u-tz-usnyc",
            "en-GB-u-ca-buddhist-nu-thai-tz-gblon",
            "ar-SA-u-ca-islamic-umalqura-nu-latn-tz-utc",
        ).forEach { tag ->
            val javaFormatter = javaBase.localizedBy(JavaLocale.forLanguageTag(tag))
            val formatter = base.localizedBy(Locale.forLanguageTag(tag))

            assertEquals(javaFormatter.locale.toLanguageTag(), formatter.locale.toLanguageTag(), tag)
            assertEquals(javaFormatter.chronology.id, formatter.chronology?.id, tag)
            assertEquals(javaFormatter.decimalStyle.toString(), formatter.decimalStyle.toString(), tag)
            assertEquals(javaFormatter.zone?.id, formatter.zone?.id, tag)
            assertEquals(javaFormatter.format(javaInstant), formatter.format(instant), tag)
        }
    }

    @Test
    fun methodChainingOrderMatchesJavaTime() {
        val tag = "en-US-u-ca-japanese-nu-arab-tz-usnyc"
        val javaLocale = JavaLocale.forLanguageTag(tag)
        val locale = Locale.forLanguageTag(tag)

        val javaAfter = JavaDateTimeFormatter.ISO_LOCAL_DATE.localizedBy(javaLocale)
            .withZone(JavaZoneOffset.UTC)
        val after = DateTimeFormatter.ISO_LOCAL_DATE.localizedBy(locale)
            .withZone(ZoneOffset.UTC)
        val javaBefore = JavaDateTimeFormatter.ISO_LOCAL_DATE.withZone(JavaZoneOffset.UTC)
            .localizedBy(javaLocale)
        val before = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)
            .localizedBy(locale)

        assertEquals(javaAfter.zone.id, after.zone?.id)
        assertEquals(javaBefore.zone.id, before.zone?.id)
    }

    @Test
    fun bundledUnicodeTimezoneTableMatchesJavaClDrData() {
        UNICODE_TIME_ZONE_IDS.forEach { (shortId, expectedZoneId) ->
            val tag = "en-u-tz-$shortId"
            val javaResult = runCatching {
                JavaDateTimeFormatter.ISO_INSTANT
                    .localizedBy(JavaLocale.forLanguageTag(tag))
                    .zone
                    ?.id
            }
            val result = runCatching {
                DateTimeFormatter.ISO_INSTANT
                    .localizedBy(Locale.forLanguageTag(tag))
                    .zone
                    ?.id
            }
            assertEquals(
                javaResult.getOrNull(),
                result.getOrNull(),
                "$shortId -> $expectedZoneId",
            )
            assertEquals(
                javaResult.exceptionOrNull()?.message,
                result.exceptionOrNull()?.message,
                "$shortId -> $expectedZoneId",
            )
        }
    }
}
