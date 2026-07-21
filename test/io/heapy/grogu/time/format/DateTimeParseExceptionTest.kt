package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeParseExceptionTest {
    @Test
    fun retainsParsedTextErrorIndexAndCause() {
        val cause = IllegalArgumentException("cause")
        val error = DateTimeParseException(
            message = "Unable to parse",
            parsedData = StringBuilder("PT?"),
            errorIndex = 2,
            cause = cause,
        )

        assertEquals("Unable to parse", error.message)
        assertEquals("PT?", error.parsedString)
        assertEquals(2, error.errorIndex)
        assertSame(cause, error.cause)
    }
}
