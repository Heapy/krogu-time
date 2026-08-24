package io.heapy.krogu.time.temporal

import io.heapy.krogu.time.DateTimeException

/** Indicates that a temporal field or unit is not supported. */
public class UnsupportedTemporalTypeException : DateTimeException {
    public constructor(message: String) : super(message)

    public constructor(message: String, cause: Throwable?) : super(message, cause)
}
