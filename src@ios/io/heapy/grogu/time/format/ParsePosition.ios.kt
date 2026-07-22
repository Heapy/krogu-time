@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.heapy.grogu.time.format

/** Native parse cursor equivalent to `java.text.ParsePosition`. */
public actual class ParsePosition public actual constructor(
    index: Int,
) {
    private var currentIndex: Int = index
    private var currentErrorIndex: Int = -1

    public actual fun getIndex(): Int = currentIndex

    public actual fun setIndex(index: Int) {
        currentIndex = index
    }

    public actual fun getErrorIndex(): Int = currentErrorIndex

    public actual fun setErrorIndex(errorIndex: Int) {
        currentErrorIndex = errorIndex
    }

    override fun equals(other: Any?): Boolean =
        other is ParsePosition &&
            currentIndex == other.currentIndex &&
            currentErrorIndex == other.currentErrorIndex

    override fun hashCode(): Int = (currentErrorIndex shl 16) or currentIndex

    override fun toString(): String =
        "io.heapy.grogu.time.format.ParsePosition" +
            "[index=$currentIndex,errorIndex=$currentErrorIndex]"
}
