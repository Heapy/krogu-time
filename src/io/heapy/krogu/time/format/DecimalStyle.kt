package io.heapy.krogu.time.format

import io.heapy.krogu.time.Locale
import io.heapy.krogu.time.availableFormatLocaleTags
import io.heapy.krogu.time.localeDecimalSymbols
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** The characters used to print and parse decimal date-time values. */
public class DecimalStyle private constructor(
    public val zeroDigit: Char,
    public val positiveSign: Char,
    public val negativeSign: Char,
    public val decimalSeparator: Char,
) {
    /** Returns a copy using [zeroDigit] as the first character of the digit sequence. */
    public fun withZeroDigit(zeroDigit: Char): DecimalStyle =
        if (zeroDigit == this.zeroDigit) {
            this
        } else {
            DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator)
        }

    /** Returns a copy using [positiveSign] for positive values. */
    public fun withPositiveSign(positiveSign: Char): DecimalStyle =
        if (positiveSign == this.positiveSign) {
            this
        } else {
            DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator)
        }

    /** Returns a copy using [negativeSign] for negative values. */
    public fun withNegativeSign(negativeSign: Char): DecimalStyle =
        if (negativeSign == this.negativeSign) {
            this
        } else {
            DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator)
        }

    /** Returns a copy using [decimalSeparator] for fractional values. */
    public fun withDecimalSeparator(decimalSeparator: Char): DecimalStyle =
        if (decimalSeparator == this.decimalSeparator) {
            this
        } else {
            DecimalStyle(zeroDigit, positiveSign, negativeSign, decimalSeparator)
        }

    internal fun convertToDigit(character: Char): Int =
        (character.code - zeroDigit.code).takeIf { it in 0..9 } ?: -1

    internal fun convertNumberToI18N(numericText: String): String {
        if (zeroDigit == '0') return numericText
        val difference = zeroDigit.code - '0'.code
        return buildString(numericText.length) {
            numericText.forEach { character -> append((character.code + difference).toChar()) }
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is DecimalStyle &&
            zeroDigit == other.zeroDigit &&
            positiveSign == other.positiveSign &&
            negativeSign == other.negativeSign &&
            decimalSeparator == other.decimalSeparator

    override fun hashCode(): Int =
        zeroDigit.code + positiveSign.code + negativeSign.code + decimalSeparator.code

    override fun toString(): String =
        "DecimalStyle[$zeroDigit$positiveSign$negativeSign$decimalSeparator]"

    public companion object {
        /** The standard ASCII decimal symbols used by ISO formatters. */
        @JvmField
        public val STANDARD: DecimalStyle = DecimalStyle('0', '+', '-', '.')

        /** Returns the locales for which platform decimal symbols are available. */
        @JvmStatic
        public fun getAvailableLocales(): Set<Locale> =
            availableFormatLocaleTags().mapTo(mutableSetOf(), Locale::forLanguageTag)

        /** Returns the decimal symbols for the default formatting locale. */
        @JvmStatic
        public fun ofDefaultLocale(): DecimalStyle = of(Locale.getDefault())

        /** Returns the decimal symbols for [locale], including its `nu` and `rg` extensions. */
        @JvmStatic
        public fun of(locale: Locale): DecimalStyle {
            val symbols = localeDecimalSymbols(locale.toLanguageTag())
            return if (
                symbols.zeroDigit == '0' &&
                symbols.negativeSign == '-' &&
                symbols.decimalSeparator == '.'
            ) {
                STANDARD
            } else {
                DecimalStyle(
                    zeroDigit = symbols.zeroDigit,
                    positiveSign = '+',
                    negativeSign = symbols.negativeSign,
                    decimalSeparator = symbols.decimalSeparator,
                )
            }
        }
    }
}
