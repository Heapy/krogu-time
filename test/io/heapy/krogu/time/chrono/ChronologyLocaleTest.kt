package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ChronologyLocaleTest {
    @Test
    fun selectsCalendarsFromUnicodeLocaleExtensions() {
        mapOf(
            "en-US" to IsoChronology,
            "en-US-u-ca-iso8601" to IsoChronology,
            "en-US-u-ca-japanese" to JapaneseChronology,
            "en-US-u-ca-buddhist" to ThaiBuddhistChronology,
            "en-US-u-ca-roc" to MinguoChronology,
            "en-US-u-ca-islamic-umalqura" to HijrahChronology,
        ).forEach { (tag, chronology) ->
            assertSame(chronology, Chronology.ofLocale(Locale.forLanguageTag(tag)), tag)
        }
    }

    @Test
    fun rejectsUnavailableLocaleCalendars() {
        assertFailsWith<DateTimeException> {
            Chronology.ofLocale(Locale.forLanguageTag("en-US-u-ca-hebrew"))
        }
    }
}
