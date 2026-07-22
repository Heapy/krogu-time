package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderOptionalJavaConformanceTest {
    @Test
    fun nestedOptionalFormattingAndParsingMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu[-MM[-dd]]")
        val groguFormatter = DateTimeFormatter.ofPattern("uuuu[-MM[-dd]]")

        listOf(
            java.time.Year.of(2024) to io.heapy.grogu.time.Year.of(2024),
            java.time.YearMonth.of(2024, 3) to io.heapy.grogu.time.YearMonth.of(2024, 3),
            java.time.LocalDate.of(2024, 3, 1) to io.heapy.grogu.time.LocalDate.of(2024, 3, 1),
        ).forEach { (javaTemporal, groguTemporal) ->
            assertEquals(javaFormatter.format(javaTemporal), groguFormatter.format(groguTemporal))
        }

        listOf("2024", "2024-03", "2024-03-01", "2024-").forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { groguFormatter.parse(text) }.isSuccess,
                text,
            )
        }
    }

    @Test
    fun optionalRollbackAndParserSettingsMatchJavaTime() {
        val javaRollback = java.time.format.DateTimeFormatterBuilder()
            .optionalStart()
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('X')
            .optionalEnd()
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
            .parse("03")
        val groguRollback = DateTimeFormatterBuilder()
            .optionalStart()
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('X')
            .optionalEnd()
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
            .parse("03")

        assertEquals(
            javaRollback.isSupported(java.time.temporal.ChronoField.MONTH_OF_YEAR),
            groguRollback.isSupported(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR),
        )
        assertEquals(
            javaRollback.getLong(java.time.temporal.ChronoField.DAY_OF_MONTH),
            groguRollback.getLong(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH),
        )

        val javaSettings = java.time.format.DateTimeFormatterBuilder()
            .optionalStart()
            .parseCaseInsensitive()
            .appendLiteral('X')
            .optionalEnd()
            .appendLiteral('Y')
            .toFormatter()
        val groguSettings = DateTimeFormatterBuilder()
            .optionalStart()
            .parseCaseInsensitive()
            .appendLiteral('X')
            .optionalEnd()
            .appendLiteral('Y')
            .toFormatter()
        assertEquals(
            runCatching { javaSettings.parse("y") }.isSuccess,
            runCatching { groguSettings.parse("y") }.isSuccess,
        )
    }
}
