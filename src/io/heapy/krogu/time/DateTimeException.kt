package io.heapy.krogu.time

/**
 * Indicates a problem while calculating or working with a date-time value.
 */
public open class DateTimeException : RuntimeException {
    public constructor(message: String) : super(message)

    public constructor(message: String, cause: Throwable?) : super(message, cause)
}
