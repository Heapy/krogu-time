package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterResolverFieldsJavaConformanceTest {
    @Test
    fun configurationAndCalendarSelectionMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('/')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_YEAR, 3)
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('/')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_YEAR, 3)
            .toFormatter()

        val javaCalendar = javaFormatter.withResolverFields(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.MONTH_OF_YEAR,
            java.time.temporal.ChronoField.DAY_OF_MONTH,
        )
        val kroguCalendar = kroguFormatter.withResolverFields(
            io.heapy.krogu.time.temporal.ChronoField.YEAR,
            io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH,
        )
        val javaOrdinal = javaFormatter.withResolverFields(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.DAY_OF_YEAR,
        )
        val kroguOrdinal = kroguFormatter.withResolverFields(
            io.heapy.krogu.time.temporal.ChronoField.YEAR,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_YEAR,
        )

        listOf("2024-02-29/060", "2024-02-29/061").forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { kroguFormatter.parse(text) }.isSuccess,
                text,
            )
            assertEquals(
                java.time.LocalDate.from(javaCalendar.parse(text)).toEpochDay(),
                io.heapy.krogu.time.LocalDate.from(kroguCalendar.parse(text)).toEpochDay(),
                "calendar:$text",
            )
            assertEquals(
                java.time.LocalDate.from(javaOrdinal.parse(text)).toEpochDay(),
                io.heapy.krogu.time.LocalDate.from(kroguOrdinal.parse(text)).toEpochDay(),
                "ordinal:$text",
            )
        }

        assertEquals(
            javaCalendar.resolverFields?.map { it.toString() }?.toSet(),
            kroguCalendar.resolverFields?.map { it.toString() }?.toSet(),
        )
    }

    @Test
    fun filteredDateTimeOffsetSupportMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm")
            .appendOffsetId()
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm")
            .appendOffsetId()
            .toFormatter()
        val text = "2024-02-29T23:45+02:30"
        val fieldGroups = listOf(
            listOf("YEAR", "MONTH_OF_YEAR", "DAY_OF_MONTH"),
            listOf("HOUR_OF_DAY", "MINUTE_OF_HOUR"),
            listOf("OFFSET_SECONDS"),
            emptyList(),
        )

        fieldGroups.forEach { names ->
            val javaFields = names.map(java.time.temporal.ChronoField::valueOf).toTypedArray()
            val kroguFields = names.map(io.heapy.krogu.time.temporal.ChronoField::valueOf).toTypedArray()
            val javaParsed = javaFormatter.withResolverFields(*javaFields).parse(text)
            val kroguParsed = kroguFormatter.withResolverFields(*kroguFields).parse(text)

            io.heapy.krogu.time.temporal.ChronoField.entries.forEach { kroguField ->
                val javaField = java.time.temporal.ChronoField.valueOf(kroguField.name)
                assertEquals(
                    javaParsed.isSupported(javaField),
                    kroguParsed.isSupported(kroguField),
                    "$names:$kroguField",
                )
                if (javaParsed.isSupported(javaField)) {
                    assertEquals(
                        javaParsed.getLong(javaField),
                        kroguParsed.getLong(kroguField),
                        "$names:$kroguField",
                    )
                }
            }
        }
    }

    @Test
    fun alternativeDateConstantsAndWeekdayCrossChecksMatchJavaTime() {
        val javaOrdinal = java.time.format.DateTimeFormatter.ISO_ORDINAL_DATE.withResolverFields(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.DAY_OF_YEAR,
        )
        val kroguOrdinal = DateTimeFormatter.ISO_ORDINAL_DATE.withResolverFields(
            io.heapy.krogu.time.temporal.ChronoField.YEAR,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_YEAR,
        )
        assertEquals(
            java.time.LocalDate.from(javaOrdinal.parse("2024-060")).toEpochDay(),
            io.heapy.krogu.time.LocalDate.from(kroguOrdinal.parse("2024-060")).toEpochDay(),
        )

        val javaWeek = java.time.format.DateTimeFormatter.ISO_WEEK_DATE.withResolverFields(
            java.time.temporal.IsoFields.WEEK_BASED_YEAR,
            java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            java.time.temporal.ChronoField.DAY_OF_WEEK,
        )
        val kroguWeek = DateTimeFormatter.ISO_WEEK_DATE.withResolverFields(
            io.heapy.krogu.time.temporal.IsoFields.WEEK_BASED_YEAR,
            io.heapy.krogu.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_WEEK,
        )
        assertEquals(
            java.time.LocalDate.from(javaWeek.parse("2024-W09-4")).toEpochDay(),
            io.heapy.krogu.time.LocalDate.from(kroguWeek.parse("2024-W09-4")).toEpochDay(),
        )

        val text = "Mon, 3 Jun 2008 11:05:30 GMT"
        val javaFields = arrayOf(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.MONTH_OF_YEAR,
            java.time.temporal.ChronoField.DAY_OF_MONTH,
            java.time.temporal.ChronoField.DAY_OF_WEEK,
            java.time.temporal.ChronoField.HOUR_OF_DAY,
            java.time.temporal.ChronoField.MINUTE_OF_HOUR,
            java.time.temporal.ChronoField.SECOND_OF_MINUTE,
            java.time.temporal.ChronoField.OFFSET_SECONDS,
        )
        val kroguFields = arrayOf(
            io.heapy.krogu.time.temporal.ChronoField.YEAR,
            io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH,
            io.heapy.krogu.time.temporal.ChronoField.DAY_OF_WEEK,
            io.heapy.krogu.time.temporal.ChronoField.HOUR_OF_DAY,
            io.heapy.krogu.time.temporal.ChronoField.MINUTE_OF_HOUR,
            io.heapy.krogu.time.temporal.ChronoField.SECOND_OF_MINUTE,
            io.heapy.krogu.time.temporal.ChronoField.OFFSET_SECONDS,
        )
        assertEquals(
            runCatching {
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                    .withResolverFields(*javaFields)
                    .parse(text)
            }.isSuccess,
            runCatching {
                DateTimeFormatter.RFC_1123_DATE_TIME
                    .withResolverFields(*kroguFields)
                    .parse(text)
            }.isSuccess,
        )
    }

    @Test
    fun chronologyStandardFieldCombinationsMatchJavaTime() {
        val scenarios = listOf(
            FormatterScenario(
                text = "24289/29",
                fields = listOf("PROLEPTIC_MONTH", "DAY_OF_MONTH"),
            ),
            FormatterScenario(
                text = "2024/2/2/3",
                fields = listOf(
                    "YEAR",
                    "MONTH_OF_YEAR",
                    "ALIGNED_WEEK_OF_MONTH",
                    "ALIGNED_DAY_OF_WEEK_IN_MONTH",
                ),
            ),
            FormatterScenario(
                text = "2024/2/2/4",
                fields = listOf(
                    "YEAR",
                    "MONTH_OF_YEAR",
                    "ALIGNED_WEEK_OF_MONTH",
                    "DAY_OF_WEEK",
                ),
            ),
            FormatterScenario(
                text = "2024/9/4",
                fields = listOf("YEAR", "ALIGNED_WEEK_OF_YEAR", "ALIGNED_DAY_OF_WEEK_IN_YEAR"),
            ),
            FormatterScenario(
                text = "2024/9/4",
                fields = listOf("YEAR", "ALIGNED_WEEK_OF_YEAR", "DAY_OF_WEEK"),
            ),
        )
        scenarios.forEach { scenario ->
            val javaFormatter = javaFormatter(scenario.fields)
            val kroguFormatter = kroguFormatter(scenario.fields)
            assertEquals(
                java.time.LocalDate.from(javaFormatter.parse(scenario.text)).toEpochDay(),
                io.heapy.krogu.time.LocalDate.from(kroguFormatter.parse(scenario.text)).toEpochDay(),
                scenario.toString(),
            )
        }

        val javaJapanese = javaFormatter(listOf("ERA", "YEAR_OF_ERA", "DAY_OF_YEAR"))
            .withChronology(java.time.chrono.JapaneseChronology.INSTANCE)
        val kroguJapanese = kroguFormatter(listOf("ERA", "YEAR_OF_ERA", "DAY_OF_YEAR"))
            .withChronology(io.heapy.krogu.time.chrono.JapaneseChronology)
        assertEquals(
            java.time.chrono.JapaneseDate.from(javaJapanese.parse("3/1/1")).toEpochDay(),
            io.heapy.krogu.time.chrono.JapaneseDate.from(kroguJapanese.parse("3/1/1")).toEpochDay(),
        )
    }

    private fun javaFormatter(fields: List<String>): java.time.format.DateTimeFormatter {
        val builder = java.time.format.DateTimeFormatterBuilder()
        fields.forEachIndexed { index, name ->
            if (index != 0) builder.appendLiteral('/')
            builder.appendValue(java.time.temporal.ChronoField.valueOf(name))
        }
        return builder.toFormatter().withResolverStyle(java.time.format.ResolverStyle.STRICT)
    }

    private fun kroguFormatter(fields: List<String>): DateTimeFormatter {
        val builder = DateTimeFormatterBuilder()
        fields.forEachIndexed { index, name ->
            if (index != 0) builder.appendLiteral('/')
            builder.appendValue(io.heapy.krogu.time.temporal.ChronoField.valueOf(name))
        }
        return builder.toFormatter().withResolverStyle(ResolverStyle.STRICT)
    }

    private data class FormatterScenario(
        val text: String,
        val fields: List<String>,
    )
}
