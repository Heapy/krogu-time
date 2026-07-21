package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeExceptionTest {
    @Test
    fun retainsMessage() {
        assertEquals("Invalid date", DateTimeException("Invalid date").message)
    }

    @Test
    fun retainsCause() {
        val cause = IllegalArgumentException("bad value")
        val error = DateTimeException("Invalid date", cause)

        assertEquals("Invalid date", error.message)
        assertSame(cause, error.cause)
    }

    @Test
    fun canBeSpecialized() {
        class ParseFailure : DateTimeException("Unable to parse")

        assertEquals("Unable to parse", ParseFailure().message)
    }
}
