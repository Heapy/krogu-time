package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
}
