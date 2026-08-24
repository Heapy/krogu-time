package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderParseControlJavaConformanceTest {
    @Test
    fun sequentialCaseAndStrictnessControlsMatchJavaTime() {
        val javaCase = java.time.format.DateTimeFormatterBuilder()
            .appendLiteral("Ab")
            .parseCaseInsensitive()
            .appendLiteral("Cd")
            .parseCaseSensitive()
            .appendLiteral("Ef")
            .toFormatter()
        val kroguCase = DateTimeFormatterBuilder()
            .appendLiteral("Ab")
            .parseCaseInsensitive()
            .appendLiteral("Cd")
            .parseCaseSensitive()
            .appendLiteral("Ef")
            .toFormatter()
        listOf("AbCdEf", "AbcDEf", "abcDEf", "AbcDef").forEach { text ->
            assertEquals(
                runCatching { javaCase.parse(text) }.isSuccess,
                runCatching { kroguCase.parse(text) }.isSuccess,
                text,
            )
        }

        val javaNumeric = java.time.format.DateTimeFormatterBuilder()
            .parseLenient()
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .parseStrict()
            .appendLiteral('/')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val kroguNumeric = DateTimeFormatterBuilder()
            .parseLenient()
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .parseStrict()
            .appendLiteral('/')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        listOf("3/01", "03/01", "003/01", "03/1").forEach { text ->
            assertEquals(
                runCatching { javaNumeric.parse(text) }.isSuccess,
                runCatching { kroguNumeric.parse(text) }.isSuccess,
                text,
            )
        }
    }

    @Test
    fun parseDefaultsMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .parseDefaulting(java.time.temporal.ChronoField.HOUR_OF_DAY, 12)
            .parseDefaulting(java.time.temporal.ChronoField.MINUTE_OF_HOUR, 34)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .parseDefaulting(io.heapy.krogu.time.temporal.ChronoField.HOUR_OF_DAY, 12)
            .parseDefaulting(io.heapy.krogu.time.temporal.ChronoField.MINUTE_OF_HOUR, 34)
            .toFormatter()

        assertEquals(
            java.time.LocalDateTime.parse("2024-03-01", javaFormatter).toString(),
            kroguFormatter.parse(
                "2024-03-01",
                io.heapy.krogu.time.LocalDateTime::from,
            ).toString(),
        )

        val javaDefaultOffset = java.time.format.DateTimeFormatterBuilder()
            .parseDefaulting(java.time.temporal.ChronoField.OFFSET_SECONDS, 3_600)
            .toFormatter()
        val kroguDefaultOffset = DateTimeFormatterBuilder()
            .parseDefaulting(io.heapy.krogu.time.temporal.ChronoField.OFFSET_SECONDS, 3_600)
            .toFormatter()
        assertEquals(
            java.time.ZoneOffset.from(javaDefaultOffset.parse("")).id,
            io.heapy.krogu.time.ZoneOffset.from(kroguDefaultOffset.parse("")).id,
        )
    }

    @Test
    fun caseInsensitiveZoneParsingMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendZoneId()
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendZoneId()
            .toFormatter()

        listOf("eUrOpE/pArIs", "utc+01:30", "z").forEach { text ->
            assertEquals(
                java.time.ZoneId.from(javaFormatter.parse(text)).id,
                io.heapy.krogu.time.ZoneId.from(kroguFormatter.parse(text)).id,
            )
        }
    }
}
