package io.heapy.grogu.time.format

/** Controls how a numeric date-time field's sign is printed and parsed. */
public enum class SignStyle {
    NORMAL,
    ALWAYS,
    NEVER,
    NOT_NEGATIVE,
    EXCEEDS_PAD,
}
