package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.Year
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.IsoFields
import io.heapy.grogu.time.temporal.JulianFields
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.WeekFields
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

            assertEquals(javaFormatter.toString(), groguFormatter.toString(), pattern)
            assertEquals(javaText, groguText, pattern)
            assertEquals(
                java.time.LocalDateTime.parse(javaText, javaFormatter).toString(),
                groguFormatter.parse(groguText, LocalDateTime::from).toString(),
                pattern,
            )
        }
    }

    @Test
    fun offsetPatternsMatchJavaTime() {
        val javaDateTime = java.time.OffsetDateTime.of(
            java.time.LocalDateTime.of(2024, 3, 1, 5, 6, 7),
            java.time.ZoneOffset.ofHoursMinutesSeconds(2, 30, 15),
        )
        val groguDateTime = OffsetDateTime.of(
            LocalDateTime.of(2024, 3, 1, 5, 6, 7),
            ZoneOffset.ofHoursMinutesSeconds(2, 30, 15),
        )
        val patterns = listOf("X", "XX", "XXX", "XXXX", "XXXXX", "x", "xx", "xxx", "Z", "ZZZ", "ZZZZZ")

        patterns.forEach { pattern ->
            assertEquals(
                java.time.format.DateTimeFormatter.ofPattern(pattern).format(javaDateTime),
                DateTimeFormatter.ofPattern(pattern).format(groguDateTime),
                pattern,
            )
        }
    }

    @Test
    fun everyPatternLetterWidthMatchesJavaTime() {
        val letters = "GyYuQqMLwWdDFEecabBhHkKmsSAnNgzvVOXxZ"
        val mismatches = mutableListOf<String>()

        letters.forEach { letter ->
            (1..20).forEach { width ->
                val pattern = letter.toString().repeat(width)
                comparePatternOutcome(pattern)?.let(mismatches::add)
            }
        }
        (1..20).forEach { width ->
            comparePatternOutcome("p".repeat(width) + "H")?.let(mismatches::add)
        }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun everyValidPatternLetterWidthFormatsLikeJavaTime() {
        val mismatches = everyPatternAndWidth().mapNotNull { pattern ->
            val javaResult = runCatching {
                java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.US)
                    .format(JAVA_DATE_TIME)
            }
            val kotlinResult = runCatching {
                DateTimeFormatter.ofPattern(pattern, Locale.US).format(KOTLIN_DATE_TIME)
            }
            val javaOutcome = javaResult.getOrNull() to
                javaResult.exceptionOrNull()?.javaClass?.simpleName
            val kotlinOutcome = kotlinResult.getOrNull() to
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName
            if (javaOutcome == kotlinOutcome) {
                null
            } else {
                "$pattern: Java=$javaOutcome, Kotlin=$kotlinOutcome"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun variableWidthYearParsingMatchesJavaTime() {
        listOf("uuu" to "2024", "yyy" to "2024").forEach { (pattern, text) ->
            assertEquals(
                java.time.Year.parse(text, java.time.format.DateTimeFormatter.ofPattern(pattern)).toString(),
                Year.parse(text, DateTimeFormatter.ofPattern(pattern)).toString(),
                pattern,
            )
        }
        listOf("uuuMMdd", "yyyMMdd").forEach { pattern ->
            assertEquals(
                java.time.LocalDate.parse(
                    "20240229",
                    java.time.format.DateTimeFormatter.ofPattern(pattern),
                ).toString(),
                io.heapy.grogu.time.LocalDate.parse(
                    "20240229",
                    DateTimeFormatter.ofPattern(pattern),
                ).toString(),
                pattern,
            )
        }
    }

    @Test
    fun everyFormattedPatternLetterWidthParsesLikeJavaTime() {
        val fields = comparableFields()
        val mismatches = everyPatternAndWidth().mapNotNull { pattern ->
            val javaFormatter = runCatching {
                java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.US)
            }.getOrNull() ?: return@mapNotNull null
            val kotlinFormatter = runCatching {
                DateTimeFormatter.ofPattern(pattern, Locale.US)
            }.getOrNull() ?: return@mapNotNull null
            val text = runCatching { javaFormatter.format(JAVA_DATE_TIME) }
                .getOrNull() ?: return@mapNotNull null
            val javaResult = runCatching { javaSnapshot(javaFormatter.parse(text), fields) }
            val kotlinResult = runCatching { kotlinSnapshot(kotlinFormatter.parse(text), fields) }
            val javaOutcome = javaResult.getOrNull() to
                javaResult.exceptionOrNull()?.javaClass?.simpleName
            val kotlinOutcome = kotlinResult.getOrNull() to
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName
            if (javaOutcome == kotlinOutcome) {
                null
            } else {
                "$pattern ($text): Java=$javaOutcome, Kotlin=$kotlinOutcome"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun comparePatternOutcome(pattern: String): String? {
        val javaResult = runCatching {
            java.time.format.DateTimeFormatter.ofPattern(pattern).toString()
        }
        val kotlinResult = runCatching {
            DateTimeFormatter.ofPattern(pattern).toString()
        }
        val javaOutcome = javaResult.getOrNull() to
            javaResult.exceptionOrNull()?.javaClass?.simpleName
        val kotlinOutcome = kotlinResult.getOrNull() to
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName
        return if (javaOutcome == kotlinOutcome) {
            null
        } else {
            "$pattern: Java=$javaOutcome, Kotlin=$kotlinOutcome"
        }
    }

    private fun everyPatternAndWidth(): List<String> = buildList {
        "GyYuQqMLwWdDFEecabBhHkKmsSAnNgzvVOXxZ".forEach { letter ->
            (1..20).forEach { width -> add(letter.toString().repeat(width)) }
        }
        (1..20).forEach { width -> add("p".repeat(width) + "H") }
    }

    private fun comparableFields(): List<Pair<java.time.temporal.TemporalField, TemporalField>> = buildList {
        ChronoField.entries.forEach { field ->
            add(java.time.temporal.ChronoField.valueOf(field.name) to field)
        }
        add(java.time.temporal.IsoFields.DAY_OF_QUARTER to IsoFields.DAY_OF_QUARTER)
        add(java.time.temporal.IsoFields.QUARTER_OF_YEAR to IsoFields.QUARTER_OF_YEAR)
        add(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR to IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        add(java.time.temporal.IsoFields.WEEK_BASED_YEAR to IsoFields.WEEK_BASED_YEAR)
        add(java.time.temporal.JulianFields.JULIAN_DAY to JulianFields.JULIAN_DAY)
        add(java.time.temporal.JulianFields.MODIFIED_JULIAN_DAY to JulianFields.MODIFIED_JULIAN_DAY)
        add(java.time.temporal.JulianFields.RATA_DIE to JulianFields.RATA_DIE)
        val javaWeekFields = java.time.temporal.WeekFields.of(java.util.Locale.US)
        val kotlinWeekFields = WeekFields.of(Locale.US)
        add(javaWeekFields.dayOfWeek() to kotlinWeekFields.dayOfWeek)
        add(javaWeekFields.weekOfMonth() to kotlinWeekFields.weekOfMonth)
        add(javaWeekFields.weekOfYear() to kotlinWeekFields.weekOfYear)
        add(javaWeekFields.weekOfWeekBasedYear() to kotlinWeekFields.weekOfWeekBasedYear)
        add(javaWeekFields.weekBasedYear() to kotlinWeekFields.weekBasedYear)
    }

    private fun javaSnapshot(
        parsed: java.time.temporal.TemporalAccessor,
        fields: List<Pair<java.time.temporal.TemporalField, TemporalField>>,
    ): ParsedSnapshot = ParsedSnapshot(
        fields = fields.mapNotNull { (javaField, kotlinField) ->
            if (parsed.isSupported(javaField)) kotlinField.toString() to parsed.getLong(javaField) else null
        }.toMap(),
        chronology = parsed.query(java.time.temporal.TemporalQueries.chronology())?.id,
        localDate = parsed.query(java.time.temporal.TemporalQueries.localDate())?.toString(),
        localTime = parsed.query(java.time.temporal.TemporalQueries.localTime())?.toString(),
        zone = parsed.query(java.time.temporal.TemporalQueries.zoneId())?.toString(),
        offset = parsed.query(java.time.temporal.TemporalQueries.offset())?.toString(),
        excessDays = parsed.query(java.time.format.DateTimeFormatter.parsedExcessDays()).toString(),
        leapSecond = parsed.query(java.time.format.DateTimeFormatter.parsedLeapSecond()),
    )

    private fun kotlinSnapshot(
        parsed: TemporalAccessor,
        fields: List<Pair<java.time.temporal.TemporalField, TemporalField>>,
    ): ParsedSnapshot = ParsedSnapshot(
        fields = fields.mapNotNull { (_, kotlinField) ->
            if (parsed.isSupported(kotlinField)) kotlinField.toString() to parsed.getLong(kotlinField) else null
        }.toMap(),
        chronology = parsed.query(TemporalQueries.chronology())?.id,
        localDate = parsed.query(TemporalQueries.localDate())?.toString(),
        localTime = parsed.query(TemporalQueries.localTime())?.toString(),
        zone = parsed.query(TemporalQueries.zoneId())?.toString(),
        offset = parsed.query(TemporalQueries.offset())?.toString(),
        excessDays = parsed.query(DateTimeFormatter.parsedExcessDays()).toString(),
        leapSecond = parsed.query(DateTimeFormatter.parsedLeapSecond()),
    )

    private data class ParsedSnapshot(
        val fields: Map<String, Long>,
        val chronology: String?,
        val localDate: String?,
        val localTime: String?,
        val zone: String?,
        val offset: String?,
        val excessDays: String,
        val leapSecond: Boolean,
    )

    private companion object {
        val JAVA_DATE_TIME: java.time.ZonedDateTime = java.time.ZonedDateTime.of(
            2024,
            2,
            29,
            23,
            58,
            59,
            123_456_789,
            java.time.ZoneId.of("Europe/Paris"),
        )
        val KOTLIN_DATE_TIME: ZonedDateTime = ZonedDateTime.of(
            LocalDateTime.of(2024, 2, 29, 23, 58, 59, 123_456_789),
            ZoneId.of("Europe/Paris"),
        )
    }
}
