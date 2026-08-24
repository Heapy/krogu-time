package io.heapy.krogu.time.format

import io.heapy.krogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DecimalStyleTest {
    @Test
    fun exposesTheStandardDecimalSymbols() {
        val style = DecimalStyle.STANDARD
        assertEquals('0', style.zeroDigit)
        assertEquals('+', style.positiveSign)
        assertEquals('-', style.negativeSign)
        assertEquals('.', style.decimalSeparator)
        assertEquals("DecimalStyle[0+-.]", style.toString())
    }

    @Test
    fun createsImmutableCopiesAndReusesUnchangedInstances() {
        val standard = DecimalStyle.STANDARD
        assertSame(standard, standard.withZeroDigit('0'))
        assertSame(standard, standard.withPositiveSign('+'))
        assertSame(standard, standard.withNegativeSign('-'))
        assertSame(standard, standard.withDecimalSeparator('.'))

        val localized = standard
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF0B')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')
        assertNotSame(standard, localized)
        assertEquals('\u0660', localized.zeroDigit)
        assertEquals('\uFF0B', localized.positiveSign)
        assertEquals('\u2212', localized.negativeSign)
        assertEquals('\u066B', localized.decimalSeparator)
        assertEquals("DecimalStyle[\u0660\uFF0B\u2212\u066B]", localized.toString())

        val same = DecimalStyle.STANDARD
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF0B')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')
        assertEquals(localized, same)
        assertEquals(localized.hashCode(), same.hashCode())
        assertNotEquals(standard, localized)
    }

    @Test
    fun obtainsSymbolsFromLocalesAndNumberingExtensions() {
        assertEquals(DecimalStyle.of(Locale.getDefault()), DecimalStyle.ofDefaultLocale())
        assertEquals(DecimalStyle.STANDARD, DecimalStyle.of(Locale.US))

        val arabic = DecimalStyle.of(Locale.forLanguageTag("en-US-u-nu-arab"))
        assertEquals('\u0660', arabic.zeroDigit)
        assertEquals('+', arabic.positiveSign)
        assertEquals('-', arabic.negativeSign)
        assertEquals('\u066B', arabic.decimalSeparator)
        assertEquals(DecimalStyle.STANDARD, DecimalStyle.of(Locale.forLanguageTag("ar-SA-u-nu-latn")))
        assertEquals(true, Locale.US in DecimalStyle.getAvailableLocales())
    }
}
