package io.heapy.grogu.time.zone

import io.heapy.grogu.time.DateTimeException

/** Indicates that rules for a requested time-zone ID cannot be obtained. */
public class ZoneRulesException : DateTimeException {
    public constructor(message: String) : super(message)

    public constructor(message: String, cause: Throwable?) : super(message, cause)
}
