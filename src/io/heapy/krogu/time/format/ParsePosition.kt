@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.heapy.krogu.time.format

/**
 * Tracks the current index and most recent error index while parsing text.
 *
 * On JVM and Android this is backed directly by [java.text.ParsePosition].
 */
public expect class ParsePosition(index: Int) {
    public fun getIndex(): Int

    public fun setIndex(index: Int)

    public fun getErrorIndex(): Int

    public fun setErrorIndex(errorIndex: Int)
}

/** The current parse index. */
public var ParsePosition.index: Int
    get() = getIndex()
    set(value) = setIndex(value)

/** The parse error index, or `-1` when no error has been recorded. */
public var ParsePosition.errorIndex: Int
    get() = getErrorIndex()
    set(value) = setErrorIndex(value)
