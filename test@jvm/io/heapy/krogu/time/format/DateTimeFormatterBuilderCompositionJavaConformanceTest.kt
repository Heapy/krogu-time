package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderCompositionJavaConformanceTest {
    @Test
    fun requiredAndOptionalCompositionMatchesJavaTime() {
        val javaTime = java.time.format.DateTimeFormatter.ofPattern("'T'HH:mm:ss")
        val kroguTime = DateTimeFormatter.ofPattern("'T'HH:mm:ss")
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(javaTime)
            .appendLiteral('>')
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(kroguTime)
            .appendLiteral('>')
            .toFormatter()

        assertEquals(javaFormatter.toString(), kroguFormatter.toString())
        assertEquals(
            javaFormatter.format(java.time.LocalDate.of(2024, 2, 29)),
            kroguFormatter.format(io.heapy.krogu.time.LocalDate.of(2024, 2, 29)),
        )
        assertEquals(
            javaFormatter.format(java.time.LocalDateTime.of(2024, 2, 29, 12, 30, 5)),
            kroguFormatter.format(io.heapy.krogu.time.LocalDateTime.of(2024, 2, 29, 12, 30, 5)),
        )

        listOf(
            "<2024-02-29>",
            "<2024-02-29T12:30:05>",
            "<2024-02-29Txx>",
            "<2024-02-30>",
        ).forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { kroguFormatter.parse(text) }.isSuccess,
                text,
            )
        }
    }

    @Test
    fun outerResolutionOfEndOfDayMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(
                java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT),
            )
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.STRICT))
            .toFormatter()
        val text = "2024-02-29T24:00"

        assertEquals(
            java.time.LocalDateTime.from(javaFormatter.parse(text)).toString(),
            io.heapy.krogu.time.LocalDateTime.from(kroguFormatter.parse(text)).toString(),
        )
        assertEquals(
            javaFormatter.parse(text)
                .query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            kroguFormatter.parse(text).query(DateTimeFormatter.parsedExcessDays()).toString(),
        )
        assertEquals(
            runCatching {
                javaFormatter.withResolverStyle(java.time.format.ResolverStyle.STRICT).parse(text)
            }.isSuccess,
            runCatching { kroguFormatter.withResolverStyle(ResolverStyle.STRICT).parse(text) }.isSuccess,
        )
    }

    @Test
    fun parsedZoneInstantAndLeapSecondStateMatchJavaTime() {
        val javaZoned = java.time.format.DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendLiteral('>')
            .toFormatter()
        val kroguZoned = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendLiteral('>')
            .toFormatter()
        val zonedText = "<2024-03-31T01:30:00+01:00[Europe/Paris]>"
        assertEquals(
            java.time.ZonedDateTime.from(javaZoned.parse(zonedText)).toString(),
            io.heapy.krogu.time.ZonedDateTime.from(kroguZoned.parse(zonedText)).toString(),
        )

        val javaInstant = java.time.format.DateTimeFormatterBuilder()
            .append(java.time.format.DateTimeFormatter.ISO_INSTANT)
            .toFormatter()
        val kroguInstant = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_INSTANT)
            .toFormatter()
        val leapText = "2016-12-31t23:59:60z"
        val javaParsed = javaInstant.parse(leapText)
        val kroguParsed = kroguInstant.parse(leapText)
        assertEquals(
            java.time.Instant.from(javaParsed).toString(),
            io.heapy.krogu.time.Instant.from(kroguParsed).toString(),
        )
        assertEquals(
            javaParsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
            kroguParsed.query(DateTimeFormatter.parsedLeapSecond()),
        )
    }

    @Test
    fun appendedFormatterOverridesAndPaddingMatchJavaTime() {
        val javaOverridden = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(java.time.chrono.ThaiBuddhistChronology.INSTANCE)
            .withZone(java.time.ZoneId.of("Europe/Paris"))
            .withResolverStyle(java.time.format.ResolverStyle.LENIENT)
        val kroguOverridden = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(io.heapy.krogu.time.chrono.ThaiBuddhistChronology)
            .withZone(io.heapy.krogu.time.ZoneId.of("Europe/Paris"))
            .withResolverStyle(ResolverStyle.LENIENT)
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .padNext(12, '_')
            .append(javaOverridden)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .padNext(12, '_')
            .append(kroguOverridden)
            .toFormatter()

        assertEquals(
            javaFormatter.format(java.time.LocalDate.of(2024, 2, 29)),
            kroguFormatter.format(io.heapy.krogu.time.LocalDate.of(2024, 2, 29)),
        )
        assertEquals(javaFormatter.toString(), kroguFormatter.toString())
    }

    @Test
    fun defaultsAndParserSettingsCrossFormatterBoundariesLikeJavaTime() {
        val javaDefaulted = java.time.format.DateTimeFormatterBuilder()
            .append(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
            .parseDefaulting(java.time.temporal.ChronoField.SECOND_OF_MINUTE, 45)
            .toFormatter()
        val kroguDefaulted = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .parseDefaulting(io.heapy.krogu.time.temporal.ChronoField.SECOND_OF_MINUTE, 45)
            .toFormatter()
        val timeText = "12:30"
        assertEquals(
            java.time.LocalTime.from(javaDefaulted.parse(timeText)).toString(),
            io.heapy.krogu.time.LocalTime.from(kroguDefaulted.parse(timeText)).toString(),
        )

        val javaPrefix = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("Ab")
            .toFormatter()
        val kroguPrefix = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("Ab")
            .toFormatter()
        val javaShared = java.time.format.DateTimeFormatterBuilder()
            .append(javaPrefix)
            .appendLiteral("Cd")
            .toFormatter()
        val kroguShared = DateTimeFormatterBuilder()
            .append(kroguPrefix)
            .appendLiteral("Cd")
            .toFormatter()
        listOf("AbCd", "abcd", "ABCD", "abCE").forEach { text ->
            assertEquals(
                runCatching { javaShared.parse(text) }.isSuccess,
                runCatching { kroguShared.parse(text) }.isSuccess,
                text,
            )
        }
    }

    @Test
    fun predefinedFormatterSettingsFlowToFollowingElementsLikeJavaTime() {
        val javaCases = listOf(
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE to "2024-02-29Z",
            java.time.format.DateTimeFormatter.ISO_DATE to "2024-02-29",
            java.time.format.DateTimeFormatter.ISO_TIME to "12:30",
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME to "2024-02-29T12:30",
            java.time.format.DateTimeFormatter.ISO_DATE_TIME to "2024-02-29T12:30",
            java.time.format.DateTimeFormatter.ISO_ORDINAL_DATE to "2024-060",
            java.time.format.DateTimeFormatter.ISO_WEEK_DATE to "2024-W09-4",
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE to "20240229",
            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME to
                "Thu, 29 Feb 2024 12:30 GMT",
            java.time.format.DateTimeFormatter.ISO_OFFSET_TIME to "12:30Z",
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME to
                "2024-02-29T12:30Z",
            java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME to
                "2024-02-29T12:30Z",
        )
        val kroguCases = listOf(
            DateTimeFormatter.ISO_OFFSET_DATE to "2024-02-29Z",
            DateTimeFormatter.ISO_DATE to "2024-02-29",
            DateTimeFormatter.ISO_TIME to "12:30",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME to "2024-02-29T12:30",
            DateTimeFormatter.ISO_DATE_TIME to "2024-02-29T12:30",
            DateTimeFormatter.ISO_ORDINAL_DATE to "2024-060",
            DateTimeFormatter.ISO_WEEK_DATE to "2024-W09-4",
            DateTimeFormatter.BASIC_ISO_DATE to "20240229",
            DateTimeFormatter.RFC_1123_DATE_TIME to "Thu, 29 Feb 2024 12:30 GMT",
            DateTimeFormatter.ISO_OFFSET_TIME to "12:30Z",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME to "2024-02-29T12:30Z",
            DateTimeFormatter.ISO_ZONED_DATE_TIME to "2024-02-29T12:30Z",
        )

        javaCases.zip(kroguCases).forEach { (javaCase, kroguCase) ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .append(javaCase.first)
                .appendLiteral('x')
                .toFormatter()
            val kroguFormatter = DateTimeFormatterBuilder()
                .append(kroguCase.first)
                .appendLiteral('x')
                .toFormatter()
            val javaText = javaCase.second + "X"
            val kroguText = kroguCase.second + "X"

            assertEquals(javaFormatter.toString(), kroguFormatter.toString(), javaCase.second)
            assertEquals(
                runCatching { javaFormatter.parse(javaText) }.isSuccess,
                runCatching { kroguFormatter.parse(kroguText) }.isSuccess,
                javaCase.second,
            )
        }
    }
}
