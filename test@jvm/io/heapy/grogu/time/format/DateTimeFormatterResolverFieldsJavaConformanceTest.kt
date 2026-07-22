package io.heapy.grogu.time.format

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
        val groguFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .appendLiteral('/')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_YEAR, 3)
            .toFormatter()

        val javaCalendar = javaFormatter.withResolverFields(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.MONTH_OF_YEAR,
            java.time.temporal.ChronoField.DAY_OF_MONTH,
        )
        val groguCalendar = groguFormatter.withResolverFields(
            io.heapy.grogu.time.temporal.ChronoField.YEAR,
            io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH,
        )
        val javaOrdinal = javaFormatter.withResolverFields(
            java.time.temporal.ChronoField.YEAR,
            java.time.temporal.ChronoField.DAY_OF_YEAR,
        )
        val groguOrdinal = groguFormatter.withResolverFields(
            io.heapy.grogu.time.temporal.ChronoField.YEAR,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_YEAR,
        )

        listOf("2024-02-29/060", "2024-02-29/061").forEach { text ->
            assertEquals(
                runCatching { javaFormatter.parse(text) }.isSuccess,
                runCatching { groguFormatter.parse(text) }.isSuccess,
                text,
            )
            assertEquals(
                java.time.LocalDate.from(javaCalendar.parse(text)).toEpochDay(),
                io.heapy.grogu.time.LocalDate.from(groguCalendar.parse(text)).toEpochDay(),
                "calendar:$text",
            )
            assertEquals(
                java.time.LocalDate.from(javaOrdinal.parse(text)).toEpochDay(),
                io.heapy.grogu.time.LocalDate.from(groguOrdinal.parse(text)).toEpochDay(),
                "ordinal:$text",
            )
        }

        assertEquals(
            javaCalendar.resolverFields?.map { it.toString() }?.toSet(),
            groguCalendar.resolverFields?.map { it.toString() }?.toSet(),
        )
    }

    @Test
    fun filteredDateTimeOffsetSupportMatchesJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm")
            .appendOffsetId()
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
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
            val groguFields = names.map(io.heapy.grogu.time.temporal.ChronoField::valueOf).toTypedArray()
            val javaParsed = javaFormatter.withResolverFields(*javaFields).parse(text)
            val groguParsed = groguFormatter.withResolverFields(*groguFields).parse(text)

            io.heapy.grogu.time.temporal.ChronoField.entries.forEach { groguField ->
                val javaField = java.time.temporal.ChronoField.valueOf(groguField.name)
                assertEquals(
                    javaParsed.isSupported(javaField),
                    groguParsed.isSupported(groguField),
                    "$names:$groguField",
                )
                if (javaParsed.isSupported(javaField)) {
                    assertEquals(
                        javaParsed.getLong(javaField),
                        groguParsed.getLong(groguField),
                        "$names:$groguField",
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
        val groguOrdinal = DateTimeFormatter.ISO_ORDINAL_DATE.withResolverFields(
            io.heapy.grogu.time.temporal.ChronoField.YEAR,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_YEAR,
        )
        assertEquals(
            java.time.LocalDate.from(javaOrdinal.parse("2024-060")).toEpochDay(),
            io.heapy.grogu.time.LocalDate.from(groguOrdinal.parse("2024-060")).toEpochDay(),
        )

        val javaWeek = java.time.format.DateTimeFormatter.ISO_WEEK_DATE.withResolverFields(
            java.time.temporal.IsoFields.WEEK_BASED_YEAR,
            java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            java.time.temporal.ChronoField.DAY_OF_WEEK,
        )
        val groguWeek = DateTimeFormatter.ISO_WEEK_DATE.withResolverFields(
            io.heapy.grogu.time.temporal.IsoFields.WEEK_BASED_YEAR,
            io.heapy.grogu.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_WEEK,
        )
        assertEquals(
            java.time.LocalDate.from(javaWeek.parse("2024-W09-4")).toEpochDay(),
            io.heapy.grogu.time.LocalDate.from(groguWeek.parse("2024-W09-4")).toEpochDay(),
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
        val groguFields = arrayOf(
            io.heapy.grogu.time.temporal.ChronoField.YEAR,
            io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH,
            io.heapy.grogu.time.temporal.ChronoField.DAY_OF_WEEK,
            io.heapy.grogu.time.temporal.ChronoField.HOUR_OF_DAY,
            io.heapy.grogu.time.temporal.ChronoField.MINUTE_OF_HOUR,
            io.heapy.grogu.time.temporal.ChronoField.SECOND_OF_MINUTE,
            io.heapy.grogu.time.temporal.ChronoField.OFFSET_SECONDS,
        )
        assertEquals(
            runCatching {
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                    .withResolverFields(*javaFields)
                    .parse(text)
            }.isSuccess,
            runCatching {
                DateTimeFormatter.RFC_1123_DATE_TIME
                    .withResolverFields(*groguFields)
                    .parse(text)
            }.isSuccess,
        )
    }
}
