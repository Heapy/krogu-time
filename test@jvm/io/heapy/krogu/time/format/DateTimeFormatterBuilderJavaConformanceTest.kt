package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDateTime
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
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('T')
            .appendPattern("HH:mm:ssXXX")
            .toFormatter()
        val javaDateTime = java.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val kroguDateTime = io.heapy.krogu.time.OffsetDateTime.parse("2024-03-01T05:06:07+02:30")
        val javaText = javaFormatter.format(javaDateTime)
        val kroguText = kroguFormatter.format(kroguDateTime)

        assertEquals(javaText, kroguText)
        assertEquals(
            java.time.LocalDateTime.from(javaFormatter.parse(javaText)).toString(),
            kroguFormatter.parse(kroguText, LocalDateTime::from).toString(),
        )
    }

    @Test
    fun numericValueCompositionMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValue(
                java.time.temporal.ChronoField.YEAR,
                4,
                10,
                java.time.format.SignStyle.EXCEEDS_PAD,
            )
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendValue(
                io.heapy.krogu.time.temporal.ChronoField.YEAR,
                4,
                10,
                SignStyle.EXCEEDS_PAD,
            )
            .appendLiteral('-')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH)
            .toFormatter()

        listOf(-1, 1, 9_999, 12_024).forEach { year ->
            val javaDate = java.time.LocalDate.of(year, 3, 1)
            val kroguDate = io.heapy.krogu.time.LocalDate.of(year, 3, 1)
            val javaText = javaFormatter.format(javaDate)
            val kroguText = kroguFormatter.format(kroguDate)

            assertEquals(javaText, kroguText)
            assertEquals(
                java.time.LocalDate.parse(javaText, javaFormatter).toString(),
                kroguFormatter.parse(kroguText, io.heapy.krogu.time.LocalDate::from).toString(),
            )
        }
    }

    @Test
    fun adjacentNumericValueParsingMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValue(
                java.time.temporal.ChronoField.YEAR,
                4,
                10,
                java.time.format.SignStyle.EXCEEDS_PAD,
            )
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendValue(
                io.heapy.krogu.time.temporal.ChronoField.YEAR,
                4,
                10,
                SignStyle.EXCEEDS_PAD,
            )
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()

        listOf("20240301", "+120240301").forEach { text ->
            assertEquals(
                java.time.LocalDate.parse(text, javaFormatter).toString(),
                kroguFormatter.parse(text, io.heapy.krogu.time.LocalDate::from).toString(),
            )
        }
    }

    @Test
    fun reducedValueWindowsMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 4, 1980)
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendValueReduced(io.heapy.krogu.time.temporal.ChronoField.YEAR, 2, 4, 1980)
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()

        listOf(1979, 1980, 2012, 2079, 2100).forEach { year ->
            val javaDate = java.time.LocalDate.of(year, 3, 1)
            val kroguDate = io.heapy.krogu.time.LocalDate.of(year, 3, 1)
            val javaText = javaFormatter.format(javaDate)
            val kroguText = kroguFormatter.format(kroguDate)

            assertEquals(javaText, kroguText)
            assertEquals(
                java.time.LocalDate.parse(javaText, javaFormatter).toString(),
                kroguFormatter.parse(kroguText, io.heapy.krogu.time.LocalDate::from).toString(),
            )
        }
    }

    @Test
    fun chronologyAwareReducedBaseDatesMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendValueReduced(
                java.time.temporal.ChronoField.YEAR,
                2,
                2,
                java.time.LocalDate.of(1950, 1, 1),
            )
            .toFormatter()
            .withChronology(java.time.chrono.ThaiBuddhistChronology.INSTANCE)
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendValueReduced(
                io.heapy.krogu.time.temporal.ChronoField.YEAR,
                2,
                2,
                io.heapy.krogu.time.LocalDate.of(1950, 1, 1),
            )
            .toFormatter()
            .withChronology(io.heapy.krogu.time.chrono.ThaiBuddhistChronology)

        listOf(2492, 2493, 2500, 2592, 2593).forEach { year ->
            val javaDate = java.time.chrono.ThaiBuddhistDate.of(year, 1, 1)
            val kroguDate = io.heapy.krogu.time.chrono.ThaiBuddhistDate.of(year, 1, 1)
            assertEquals(javaFormatter.format(javaDate), kroguFormatter.format(kroguDate))
        }
        listOf("92", "93", "00").forEach { text ->
            assertEquals(
                javaFormatter.parse(text).getLong(java.time.temporal.ChronoField.YEAR),
                kroguFormatter.parse(text).getLong(io.heapy.krogu.time.temporal.ChronoField.YEAR),
            )
        }
    }

    @Test
    fun fractionFormattingAndParsingMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(io.heapy.krogu.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter()

        listOf(0, 120_000_000, 123_456_789).forEach { nano ->
            val javaTime = java.time.LocalTime.of(5, 6, 7, nano)
            val kroguTime = io.heapy.krogu.time.LocalTime.of(5, 6, 7, nano)
            val javaText = javaFormatter.format(javaTime)
            val kroguText = kroguFormatter.format(kroguTime)

            assertEquals(javaText, kroguText)
            assertEquals(
                java.time.LocalTime.parse(javaText, javaFormatter).toString(),
                kroguFormatter.parse(kroguText, io.heapy.krogu.time.LocalTime::from).toString(),
            )
        }

        val javaGeneric = java.time.format.DateTimeFormatterBuilder()
            .appendFraction(java.time.temporal.ChronoField.SECOND_OF_MINUTE, 0, 9, true)
            .toFormatter()
        val kroguGeneric = DateTimeFormatterBuilder()
            .appendFraction(io.heapy.krogu.time.temporal.ChronoField.SECOND_OF_MINUTE, 0, 9, true)
            .toFormatter()
        assertEquals(javaGeneric.format(java.time.LocalTime.of(5, 6, 15)), ".25")
        assertEquals(kroguGeneric.format(io.heapy.krogu.time.LocalTime.of(5, 6, 15)), ".25")
        assertEquals(
            javaGeneric.parse(".25").getLong(java.time.temporal.ChronoField.SECOND_OF_MINUTE),
            kroguGeneric.parse(".25").getLong(io.heapy.krogu.time.temporal.ChronoField.SECOND_OF_MINUTE),
        )
    }
}
