package io.heapy.grogu.time.format

/** Controls how parsed date and time fields are resolved. */
public enum class ResolverStyle {
    /** Requires every parsed field value to be valid for the resolved date-time. */
    STRICT,

    /** Resolves fields sensibly while preserving their intended meaning. */
    SMART,

    /** Resolves fields arithmetically without requiring their outer valid ranges. */
    LENIENT,
}
