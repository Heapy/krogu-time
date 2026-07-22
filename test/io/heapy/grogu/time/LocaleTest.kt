package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class LocaleTest {
    @Test
    fun representsCanonicalLanguageTags() {
        assertEquals("und", Locale.ROOT.toLanguageTag())
        assertEquals("en", Locale.ENGLISH.toLanguageTag())
        assertEquals("en-US", Locale.US.toLanguageTag())
        assertEquals("en-GB", Locale.UK.toLanguageTag())
        assertEquals(
            "sr-Latn-RS-u-ca-gregory",
            Locale.forLanguageTag("SR-latn-rs-U-CA-GREGORY").toLanguageTag(),
        )
    }

    @Test
    fun languageTagsHaveValueSemantics() {
        val first = Locale.forLanguageTag("fr-FR")
        val second = Locale.forLanguageTag("fr-fr")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals("fr-FR", first.toString())
        assertNotEquals(first, Locale.forLanguageTag("fr-CA"))
    }

    @Test
    fun emptyAndUndefinedTagsRepresentTheRootLocale() {
        assertEquals(Locale.ROOT, Locale.forLanguageTag(""))
        assertEquals(Locale.ROOT, Locale.forLanguageTag("und"))
    }

    @Test
    fun defaultFormatLocaleIsAUsableLanguageTag() {
        val defaultLocale = Locale.getDefault()

        assertEquals(defaultLocale, Locale.forLanguageTag(defaultLocale.toLanguageTag()))
    }

    @Test
    fun exposesUnicodeLocaleKeywordValues() {
        val locale = Locale.forLanguageTag(
            "en-US-u-ca-japanese-nu-arab-tz-usnyc",
        )

        assertEquals("japanese", locale.getUnicodeLocaleType("ca"))
        assertEquals("arab", locale.getUnicodeLocaleType("NU"))
        assertEquals("usnyc", locale.getUnicodeLocaleType("tz"))
        assertNull(locale.getUnicodeLocaleType("rg"))
        assertNull(Locale.forLanguageTag("en-x-u-ca-japanese").getUnicodeLocaleType("ca"))
        listOf("c", "calendar", "c!", "\u00E5a").forEach { key ->
            assertFailsWith<IllegalArgumentException>(key) {
                locale.getUnicodeLocaleType(key)
            }
        }
    }
}
