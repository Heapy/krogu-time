package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Locale
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronologyLocaleJavaConformanceTest {
    @Test
    fun localeCalendarSelectionMatchesJavaTime() {
        listOf(
            "en-US",
            "en-US-u-ca-iso8601",
            "en-US-u-ca-japanese",
            "en-US-u-ca-buddhist",
            "en-US-u-ca-roc",
            "en-US-u-ca-islamic-umalqura",
            "en-US-u-ca-hebrew",
        ).forEach { tag ->
            val javaResult = runCatching {
                java.time.chrono.Chronology.ofLocale(JavaLocale.forLanguageTag(tag)).id
            }
            val result = runCatching {
                Chronology.ofLocale(Locale.forLanguageTag(tag)).id
            }
            assertEquals(javaResult.getOrNull(), result.getOrNull(), tag)
            assertEquals(
                javaResult.exceptionOrNull()?.message,
                result.exceptionOrNull()?.message,
                tag,
            )
        }
    }
}
