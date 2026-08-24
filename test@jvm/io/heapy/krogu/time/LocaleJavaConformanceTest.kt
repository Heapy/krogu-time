package io.heapy.krogu.time

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

    @Test
    fun unicodeLocaleTypesAndKeyValidationMatchJava() {
        val tag = "en-US-u-attr-ca-japanese-nu-arab-tz-usnyc"
        val javaLocale = JavaLocale.forLanguageTag(tag)
        val locale = Locale.forLanguageTag(tag)

        listOf("ca", "NU", "tz", "rg", "zz").forEach { key ->
            assertEquals(
                runCatching { javaLocale.getUnicodeLocaleType(key) },
                runCatching { locale.getUnicodeLocaleType(key) },
                key,
            )
        }
        assertEquals(
            JavaLocale.forLanguageTag("en-x-u-ca-japanese").getUnicodeLocaleType("ca"),
            Locale.forLanguageTag("en-x-u-ca-japanese").getUnicodeLocaleType("ca"),
        )
        listOf("c", "calendar", "c!", "\u00E5a").forEach { key ->
            assertEquals(
                runCatching { javaLocale.getUnicodeLocaleType(key) }.isSuccess,
                runCatching { locale.getUnicodeLocaleType(key) }.isSuccess,
                key,
            )
        }
    }
}
