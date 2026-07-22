package io.heapy.grogu.time.format

import java.time.format.DecimalStyle as JavaDecimalStyle
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

    private fun assertMatches(expected: JavaDecimalStyle, actual: DecimalStyle) {
        assertEquals(expected.zeroDigit, actual.zeroDigit)
        assertEquals(expected.positiveSign, actual.positiveSign)
        assertEquals(expected.negativeSign, actual.negativeSign)
        assertEquals(expected.decimalSeparator, actual.decimalSeparator)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }
}
