package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderJavaConformanceTest {
    @Test
    fun patternAndLiteralCompositionMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('T')
            .appendPattern("HH:mm:ssXXX")
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('T')
            .appendPattern("HH:mm:ssXXX")
            .toFormatter()
        val javaDateTime = java.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val groguDateTime = io.heapy.grogu.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val javaText = javaFormatter.format(javaDateTime)
        val groguText = groguFormatter.format(groguDateTime)

        assertEquals(javaText, groguText)
        assertEquals(
            java.time.LocalDateTime.from(javaFormatter.parse(javaText)).toString(),
            groguFormatter.parse(groguText, LocalDateTime::from).toString(),
        )
    }
}
