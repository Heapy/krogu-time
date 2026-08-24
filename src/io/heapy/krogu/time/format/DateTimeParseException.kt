package io.heapy.krogu.time.format

import io.heapy.krogu.time.DateTimeException

/** Indicates that text could not be parsed into a date-time value. */
public class DateTimeParseException : DateTimeException {
    /** The text that was being parsed. */
    public val parsedString: String

    /** The index at which the parse error was reported. */
    public val errorIndex: Int

    public constructor(
        message: String,
        parsedData: CharSequence,
        errorIndex: Int,
    ) : super(message) {
        this.parsedString = parsedData.toString()
        this.errorIndex = errorIndex
    }

    public constructor(
        message: String,
        parsedData: CharSequence,
        errorIndex: Int,
        cause: Throwable?,
    ) : super(message, cause) {
        this.parsedString = parsedData.toString()
        this.errorIndex = errorIndex
    }
}
