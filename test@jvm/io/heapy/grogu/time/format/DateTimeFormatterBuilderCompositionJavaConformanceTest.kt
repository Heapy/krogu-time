package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderCompositionJavaConformanceTest {
    @Test
    fun requiredAndOptionalCompositionMatchesJavaTime() {
        val javaTime = java.time.format.DateTimeFormatter.ofPattern("'T'HH:mm:ss")
        val groguTime = DateTimeFormatter.ofPattern("'T'HH:mm:ss")
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(javaTime)
            .appendLiteral('>')
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendOptional(groguTime)
            .appendLiteral('>')
            .toFormatter()

        assertEquals(javaFormatter.toString(), groguFormatter.toString())
        assertEquals(
            javaFormatter.format(java.time.LocalDate.of(2024, 2, 29)),
            groguFormatter.format(io.heapy.grogu.time.LocalDate.of(2024, 2, 29)),
        )
        assertEquals(
            javaFormatter.format(java.time.LocalDateTime.of(2024, 2, 29, 12, 30, 5)),
            groguFormatter.format(io.heapy.grogu.time.LocalDateTime.of(2024, 2, 29, 12, 30, 5)),
        )

        listOf(
            "<2024-02-29>",
            "<2024-02-29T12:30:05>",
            "<2024-02-29Txx>",
            "<2024-02-30>",
        ).forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { groguFormatter.parse(text) }.isSuccess,
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
        val groguFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.STRICT))
            .toFormatter()
        val text = "2024-02-29T24:00"

        assertEquals(
            java.time.LocalDateTime.from(javaFormatter.parse(text)).toString(),
            io.heapy.grogu.time.LocalDateTime.from(groguFormatter.parse(text)).toString(),
        )
        assertEquals(
            javaFormatter.parse(text)
                .query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
            groguFormatter.parse(text).query(DateTimeFormatter.parsedExcessDays()).toString(),
        )
        assertEquals(
            runCatching {
                javaFormatter.withResolverStyle(java.time.format.ResolverStyle.STRICT).parse(text)
            }.isSuccess,
            runCatching { groguFormatter.withResolverStyle(ResolverStyle.STRICT).parse(text) }.isSuccess,
        )
    }

    @Test
    fun parsedZoneInstantAndLeapSecondStateMatchJavaTime() {
        val javaZoned = java.time.format.DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendLiteral('>')
            .toFormatter()
        val groguZoned = DateTimeFormatterBuilder()
            .appendLiteral('<')
            .append(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendLiteral('>')
            .toFormatter()
        val zonedText = "<2024-03-31T01:30:00+01:00[Europe/Paris]>"
        assertEquals(
            java.time.ZonedDateTime.from(javaZoned.parse(zonedText)).toString(),
            io.heapy.grogu.time.ZonedDateTime.from(groguZoned.parse(zonedText)).toString(),
        )

        val javaInstant = java.time.format.DateTimeFormatterBuilder()
            .append(java.time.format.DateTimeFormatter.ISO_INSTANT)
            .toFormatter()
        val groguInstant = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_INSTANT)
            .toFormatter()
        val leapText = "2016-12-31t23:59:60z"
        val javaParsed = javaInstant.parse(leapText)
        val groguParsed = groguInstant.parse(leapText)
        assertEquals(
            java.time.Instant.from(javaParsed).toString(),
            io.heapy.grogu.time.Instant.from(groguParsed).toString(),
        )
        assertEquals(
            javaParsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
            groguParsed.query(DateTimeFormatter.parsedLeapSecond()),
        )
    }

    @Test
    fun appendedFormatterOverridesAndPaddingMatchJavaTime() {
        val javaOverridden = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(java.time.chrono.ThaiBuddhistChronology.INSTANCE)
            .withZone(java.time.ZoneId.of("Europe/Paris"))
            .withResolverStyle(java.time.format.ResolverStyle.LENIENT)
        val groguOverridden = DateTimeFormatter.ISO_LOCAL_DATE
            .withChronology(io.heapy.grogu.time.chrono.ThaiBuddhistChronology)
            .withZone(io.heapy.grogu.time.ZoneId.of("Europe/Paris"))
            .withResolverStyle(ResolverStyle.LENIENT)
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .padNext(12, '_')
            .append(javaOverridden)
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .padNext(12, '_')
            .append(groguOverridden)
            .toFormatter()

        assertEquals(
            javaFormatter.format(java.time.LocalDate.of(2024, 2, 29)),
            groguFormatter.format(io.heapy.grogu.time.LocalDate.of(2024, 2, 29)),
        )
        assertEquals(javaFormatter.toString(), groguFormatter.toString())
    }

    @Test
    fun defaultsAndParserSettingsCrossFormatterBoundariesLikeJavaTime() {
        val javaDefaulted = java.time.format.DateTimeFormatterBuilder()
            .append(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
            .parseDefaulting(java.time.temporal.ChronoField.SECOND_OF_MINUTE, 45)
            .toFormatter()
        val groguDefaulted = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .parseDefaulting(io.heapy.grogu.time.temporal.ChronoField.SECOND_OF_MINUTE, 45)
            .toFormatter()
        val timeText = "12:30"
        assertEquals(
            java.time.LocalTime.from(javaDefaulted.parse(timeText)).toString(),
            io.heapy.grogu.time.LocalTime.from(groguDefaulted.parse(timeText)).toString(),
        )

        val javaPrefix = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("Ab")
            .toFormatter()
        val groguPrefix = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("Ab")
            .toFormatter()
        val javaShared = java.time.format.DateTimeFormatterBuilder()
            .append(javaPrefix)
            .appendLiteral("Cd")
            .toFormatter()
        val groguShared = DateTimeFormatterBuilder()
            .append(groguPrefix)
            .appendLiteral("Cd")
            .toFormatter()
        listOf("AbCd", "abcd", "ABCD", "abCE").forEach { text ->
            assertEquals(
                runCatching { javaShared.parse(text) }.isSuccess,
                runCatching { groguShared.parse(text) }.isSuccess,
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
        val groguCases = listOf(
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

        javaCases.zip(groguCases).forEach { (javaCase, groguCase) ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .append(javaCase.first)
                .appendLiteral('x')
                .toFormatter()
            val groguFormatter = DateTimeFormatterBuilder()
                .append(groguCase.first)
                .appendLiteral('x')
                .toFormatter()
            val javaText = javaCase.second + "X"
            val groguText = groguCase.second + "X"

            assertEquals(javaFormatter.toString(), groguFormatter.toString(), javaCase.second)
            assertEquals(
                runCatching { javaFormatter.parse(javaText) }.isSuccess,
                runCatching { groguFormatter.parse(groguText) }.isSuccess,
                javaCase.second,
            )
        }
    }
}
