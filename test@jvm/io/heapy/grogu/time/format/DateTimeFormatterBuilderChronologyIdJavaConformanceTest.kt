package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderChronologyIdJavaConformanceTest {
    @Test
    fun formattingAndChronologySpecificResolutionMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
        val groguFormatter = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
        val javaDates = listOf(
            java.time.LocalDate.of(2024, 2, 29),
            java.time.chrono.JapaneseDate.of(2024, 2, 29),
            java.time.chrono.HijrahDate.of(1_445, 8, 19),
            java.time.chrono.MinguoDate.of(113, 2, 29),
            java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
        )
        val groguDates = listOf(
            io.heapy.grogu.time.LocalDate.of(2024, 2, 29),
            io.heapy.grogu.time.chrono.JapaneseDate.of(2024, 2, 29),
            io.heapy.grogu.time.chrono.HijrahDate.of(1_445, 8, 19),
            io.heapy.grogu.time.chrono.MinguoDate.of(113, 2, 29),
            io.heapy.grogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
        )
        javaDates.zip(groguDates).forEach { (javaDate, groguDate) ->
            assertEquals(javaFormatter.format(javaDate), groguFormatter.format(groguDate))
        }

        listOf(
            "ISO|2024-02-29",
            "Japanese|2024-02-29",
            "Hijrah-umalqura|1445-08-19",
            "Minguo|0113-02-29",
            "ThaiBuddhist|2567-02-29",
        ).forEach { text ->
            val javaParsed = javaFormatter.parse(text)
            val groguParsed = groguFormatter.parse(text)
            assertEquals(
                java.time.chrono.Chronology.from(javaParsed).id,
                io.heapy.grogu.time.chrono.Chronology.from(groguParsed).id,
                text,
            )
            assertEquals(
                java.time.chrono.Chronology.from(javaParsed).date(javaParsed).toEpochDay(),
                io.heapy.grogu.time.chrono.Chronology.from(groguParsed)
                    .date(groguParsed)
                    .toEpochDay(),
                text,
            )
        }
    }

    @Test
    fun yearOfEraResolutionMatchesJavaTime() {
        val javaExplicit = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendValue(java.time.temporal.ChronoField.ERA)
            .appendLiteral('|')
            .appendValue(java.time.temporal.ChronoField.YEAR_OF_ERA)
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(java.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val groguExplicit = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.ERA)
            .appendLiteral('|')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.YEAR_OF_ERA)
            .appendLiteral('-')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(io.heapy.grogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val explicitText = "Japanese|3|6-02-29"
        assertEquals(
            java.time.chrono.JapaneseChronology.INSTANCE
                .date(javaExplicit.parse(explicitText))
                .toEpochDay(),
            io.heapy.grogu.time.chrono.JapaneseChronology
                .date(groguExplicit.parse(explicitText))
                .toEpochDay(),
        )

        val javaInferred = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
        val groguInferred = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
        val inferredText = "Japanese|0006-02-29"
        assertEquals(
            java.time.chrono.JapaneseChronology.INSTANCE
                .date(javaInferred.parse(inferredText))
                .toEpochDay(),
            io.heapy.grogu.time.chrono.JapaneseChronology
                .date(groguInferred.parse(inferredText))
                .toEpochDay(),
        )
        assertEquals(
            runCatching {
                java.time.chrono.JapaneseChronology.INSTANCE.date(
                    javaInferred
                        .withResolverStyle(java.time.format.ResolverStyle.STRICT)
                        .parse(inferredText),
                )
            }.isSuccess,
            runCatching {
                io.heapy.grogu.time.chrono.JapaneseChronology.date(
                    groguInferred
                        .withResolverStyle(ResolverStyle.STRICT)
                        .parse(inferredText),
                )
            }.isSuccess,
        )
    }

    @Test
    fun caseSensitivityUnknownIdsOptionalSectionsAndPaddingMatchJavaTime() {
        fun javaFormatter(caseInsensitive: Boolean): java.time.format.DateTimeFormatter {
            val builder = java.time.format.DateTimeFormatterBuilder()
            if (caseInsensitive) builder.parseCaseInsensitive()
            return builder.appendChronologyId().toFormatter()
        }
        fun groguFormatter(caseInsensitive: Boolean): DateTimeFormatter {
            val builder = DateTimeFormatterBuilder()
            if (caseInsensitive) builder.parseCaseInsensitive()
            return builder.appendChronologyId().toFormatter()
        }
        listOf("ThaiBuddhist", "thaibuddhist", "buddhist", "Unknown").forEach { text ->
            listOf(false, true).forEach { caseInsensitive ->
                assertEquals(
                    runCatching { javaFormatter(caseInsensitive).parse(text) }.isSuccess,
                    runCatching { groguFormatter(caseInsensitive).parse(text) }.isSuccess,
                    "$caseInsensitive:$text",
                )
            }
        }

        val javaOptional = java.time.format.DateTimeFormatterBuilder()
            .optionalStart()
            .appendChronologyId()
            .optionalEnd()
            .appendLiteral('!')
            .toFormatter()
        val groguOptional = DateTimeFormatterBuilder()
            .optionalStart()
            .appendChronologyId()
            .optionalEnd()
            .appendLiteral('!')
            .toFormatter()
        assertEquals(
            javaOptional.format(java.time.LocalTime.NOON),
            groguOptional.format(io.heapy.grogu.time.LocalTime.NOON),
        )

        val javaPadded = java.time.format.DateTimeFormatterBuilder()
            .padNext(18, '_')
            .appendChronologyId()
            .toFormatter()
        val groguPadded = DateTimeFormatterBuilder()
            .padNext(18, '_')
            .appendChronologyId()
            .toFormatter()
        assertEquals(
            javaPadded.format(java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
            groguPadded.format(io.heapy.grogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
        )
        assertEquals(
            java.time.chrono.Chronology.from(javaPadded.parse("______ThaiBuddhist")).id,
            io.heapy.grogu.time.chrono.Chronology.from(
                groguPadded.parse("______ThaiBuddhist"),
            ).id,
        )
    }

    @Test
    fun chronologyChangesRecalculateReducedValuesLikeJavaTime() {
        fun javaFormatter(chronologyFirst: Boolean): java.time.format.DateTimeFormatter {
            val builder = java.time.format.DateTimeFormatterBuilder()
            if (chronologyFirst) builder.appendChronologyId().appendLiteral('|')
            builder.appendValueReduced(
                java.time.temporal.ChronoField.YEAR,
                2,
                2,
                java.time.LocalDate.of(1950, 1, 1),
            )
            if (!chronologyFirst) builder.appendLiteral('|').appendChronologyId()
            return builder.toFormatter()
        }
        fun groguFormatter(chronologyFirst: Boolean): DateTimeFormatter {
            val builder = DateTimeFormatterBuilder()
            if (chronologyFirst) builder.appendChronologyId().appendLiteral('|')
            builder.appendValueReduced(
                io.heapy.grogu.time.temporal.ChronoField.YEAR,
                2,
                2,
                io.heapy.grogu.time.LocalDate.of(1950, 1, 1),
            )
            if (!chronologyFirst) builder.appendLiteral('|').appendChronologyId()
            return builder.toFormatter()
        }
        listOf(
            true to "ThaiBuddhist|93",
            false to "93|ThaiBuddhist",
        ).forEach { (chronologyFirst, text) ->
            assertEquals(
                javaFormatter(chronologyFirst).parse(text)
                    .getLong(java.time.temporal.ChronoField.YEAR),
                groguFormatter(chronologyFirst).parse(text)
                    .getLong(io.heapy.grogu.time.temporal.ChronoField.YEAR),
            )
        }
    }

    @Test
    fun overridesAndFormatterCompositionMatchJavaTime() {
        val javaChronology = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .toFormatter()
        val groguChronology = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .toFormatter()
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .append(javaChronology)
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withChronology(java.time.chrono.JapaneseChronology.INSTANCE)
        val groguFormatter = DateTimeFormatterBuilder()
            .append(groguChronology)
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withChronology(io.heapy.grogu.time.chrono.JapaneseChronology)
        val text = "ThaiBuddhist|2567-02-29"
        val javaParsed = javaFormatter.parse(text)
        val groguParsed = groguFormatter.parse(text)

        assertEquals(
            java.time.chrono.Chronology.from(javaParsed).id,
            io.heapy.grogu.time.chrono.Chronology.from(groguParsed).id,
        )
        assertEquals(
            java.time.chrono.Chronology.from(javaParsed).date(javaParsed).toEpochDay(),
            io.heapy.grogu.time.chrono.Chronology.from(groguParsed)
                .date(groguParsed)
                .toEpochDay(),
        )
        assertEquals(
            javaFormatter.format(java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
            groguFormatter.format(
                io.heapy.grogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
            ),
        )
    }
}
