package io.heapy.grogu.time.format

/** Controls the width and grammatical context of localized date-time text. */
public enum class TextStyle {
    FULL,
    FULL_STANDALONE,
    SHORT,
    SHORT_STANDALONE,
    NARROW,
    NARROW_STANDALONE;

    /** Whether this style is intended for text displayed outside a complete date. */
    public val isStandalone: Boolean
        get() = ordinal and 1 == 1

    /** Returns the standalone form with the same width. */
    public fun asStandalone(): TextStyle = entries[ordinal or 1]

    /** Returns the normal form with the same width. */
    public fun asNormal(): TextStyle = entries[ordinal and 1.inv()]
}
