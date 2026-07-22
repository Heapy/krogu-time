package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterPatternJavaConformanceTest {
    @Test
    fun numericPatternsMatchJavaTime() {
        val javaDateTime = java.time.LocalDateTime.of(2024, 3, 1, 5, 6, 7, 8_000_000)
        val groguDateTime = LocalDateTime.of(2024, 3, 1, 5, 6, 7, 8_000_000)
        val patterns = listOf(
            "uuuu/MM/dd HH:mm:ss.SSS",
            "uuuuMMddHHmmss",
            "u-M-d H:m:s.SSSSSSSSS",
            "uuuu-MM-dd 'at' HH:mm 'o''clock'",
        )

        patterns.forEach { pattern ->
            val javaFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
            val groguFormatter = DateTimeFormatter.ofPattern(pattern)
            val javaText = javaFormatter.format(javaDateTime)
            val groguText = groguFormatter.format(groguDateTime)

            assertEquals(javaText, groguText, pattern)
            assertEquals(
                java.time.LocalDateTime.parse(javaText, javaFormatter).toString(),
                groguFormatter.parse(groguText, LocalDateTime::from).toString(),
                pattern,
            )
        }
    }
}
