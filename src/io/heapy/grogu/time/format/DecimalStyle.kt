package io.heapy.grogu.time.format

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
        public val STANDARD: DecimalStyle = DecimalStyle('0', '+', '-', '.')
    }
}
