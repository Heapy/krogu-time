package io.heapy.krogu.time.zone

import io.heapy.krogu.time.DateTimeException

/** Indicates that rules for a requested time-zone ID cannot be obtained. */
public class ZoneRulesException : DateTimeException {
    public constructor(message: String) : super(message)

    public constructor(message: String, cause: Throwable?) : super(message, cause)
}
