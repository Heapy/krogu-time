package io.heapy.grogu.time.format

import io.heapy.grogu.time.Locale
import java.time.format.DecimalStyle as JavaDecimalStyle
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalStyleJavaConformanceTest {
    @Test
    fun standardSymbolsCopiesValuesHashesAndTextMatchJavaTime() {
        val javaStandard = JavaDecimalStyle.STANDARD
        val standard = DecimalStyle.STANDARD
        assertMatches(javaStandard, standard)

        val javaLocalized = javaStandard
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF0B')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')
        val localized = standard
            .withZeroDigit('\u0660')
            .withPositiveSign('\uFF0B')
            .withNegativeSign('\u2212')
            .withDecimalSeparator('\u066B')
        assertMatches(javaLocalized, localized)
    }

    @Test
    fun localeDerivedSymbolsMatchJavaTime() {
        listOf(
            "en-US",
            "ar-SA",
            "fa-IR",
            "bn-BD",
            "en-US-u-nu-arab",
            "ar-SA-u-nu-latn",
            "en-US-u-rg-thzzzz",
        ).forEach { tag ->
            assertMatches(
                JavaDecimalStyle.of(JavaLocale.forLanguageTag(tag)),
                DecimalStyle.of(Locale.forLanguageTag(tag)),
            )
        }

        assertEquals(
            JavaDecimalStyle.ofDefaultLocale().toString(),
            DecimalStyle.ofDefaultLocale().toString(),
        )
        assertEquals(
            true,
            DecimalStyle.getAvailableLocales().map(Locale::toLanguageTag).toSet()
                .containsAll(setOf("und", "en-US")),
        )
    }

    private fun assertMatches(expected: JavaDecimalStyle, actual: DecimalStyle) {
        assertEquals(expected.zeroDigit, actual.zeroDigit)
        assertEquals(expected.positiveSign, actual.positiveSign)
        assertEquals(expected.negativeSign, actual.negativeSign)
        assertEquals(expected.decimalSeparator, actual.decimalSeparator)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }
}
