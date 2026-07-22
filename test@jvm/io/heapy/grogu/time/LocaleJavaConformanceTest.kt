package io.heapy.grogu.time

import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleJavaConformanceTest {
    @Test
    fun representativeLanguageTagsCanonicalizeLikeJava() {
        listOf(
            "",
            "und",
            "en",
            "en-us",
            "sr-latn-rs",
            "zh-Hant-TW",
            "de-DE-u-ca-gregory",
        ).forEach { tag ->
            assertEquals(
                JavaLocale.forLanguageTag(tag).toLanguageTag(),
                Locale.forLanguageTag(tag).toLanguageTag(),
                tag,
            )
        }
    }

    @Test
    fun defaultLocaleUsesJavasFormatCategory() {
        assertEquals(
            JavaLocale.getDefault(JavaLocale.Category.FORMAT).toLanguageTag(),
            Locale.getDefault().toLanguageTag(),
        )
    }
}
