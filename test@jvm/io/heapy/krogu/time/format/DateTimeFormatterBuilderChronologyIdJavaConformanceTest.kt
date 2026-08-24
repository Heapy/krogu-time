package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderChronologyIdJavaConformanceTest {
    @Test
    fun localizedChronologyTextAndDisplayNamesMatchJavaTime() {
        val javaChronologies = listOf(
            java.time.chrono.IsoChronology.INSTANCE,
            java.time.chrono.JapaneseChronology.INSTANCE,
            java.time.chrono.HijrahChronology.INSTANCE,
            java.time.chrono.MinguoChronology.INSTANCE,
            java.time.chrono.ThaiBuddhistChronology.INSTANCE,
        )
        val kroguChronologies = listOf(
            io.heapy.krogu.time.chrono.IsoChronology,
            io.heapy.krogu.time.chrono.JapaneseChronology,
            io.heapy.krogu.time.chrono.HijrahChronology,
            io.heapy.krogu.time.chrono.MinguoChronology,
            io.heapy.krogu.time.chrono.ThaiBuddhistChronology,
        )
        val javaDates = listOf(
            java.time.LocalDate.of(2024, 2, 29),
            java.time.chrono.JapaneseDate.of(2024, 2, 29),
            java.time.chrono.HijrahDate.of(1_445, 8, 19),
            java.time.chrono.MinguoDate.of(113, 2, 29),
            java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
        )
        val kroguDates = listOf(
            io.heapy.krogu.time.LocalDate.of(2024, 2, 29),
            io.heapy.krogu.time.chrono.JapaneseDate.of(2024, 2, 29),
            io.heapy.krogu.time.chrono.HijrahDate.of(1_445, 8, 19),
            io.heapy.krogu.time.chrono.MinguoDate.of(113, 2, 29),
            io.heapy.krogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
        )

        listOf("en-US", "fr-FR", "de-DE", "zh-CN", "ar-SA").forEach { languageTag ->
            val javaLocale = java.util.Locale.forLanguageTag(languageTag)
            val locale = io.heapy.krogu.time.Locale.forLanguageTag(languageTag)
            TextStyle.entries.forEach { style ->
                val javaStyle = java.time.format.TextStyle.valueOf(style.name)
                val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                    .appendChronologyText(javaStyle)
                    .toFormatter(javaLocale)
                val formatter = DateTimeFormatterBuilder()
                    .appendChronologyText(style)
                    .toFormatter(locale)

                javaChronologies.zip(kroguChronologies).forEach { (javaChronology, chronology) ->
                    assertEquals(
                        javaChronology.getDisplayName(javaStyle, javaLocale),
                        chronology.getDisplayName(style, locale),
                        "$languageTag $style ${chronology.id}",
                    )
                }
                javaDates.zip(kroguDates).forEach { (javaDate, date) ->
                    val text = javaFormatter.format(javaDate)
                    assertEquals(text, formatter.format(date), "$languageTag $style $date")
                    assertEquals(
                        java.time.chrono.Chronology.from(javaFormatter.parse(text)).id,
                        io.heapy.krogu.time.chrono.Chronology.from(formatter.parse(text)).id,
                        "$languageTag $style parsing $text",
                    )
                }
            }
        }
    }

    @Test
    fun formattingAndChronologySpecificResolutionMatchJavaTime() {
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
        val kroguFormatter = DateTimeFormatterBuilder()
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
        val kroguDates = listOf(
            io.heapy.krogu.time.LocalDate.of(2024, 2, 29),
            io.heapy.krogu.time.chrono.JapaneseDate.of(2024, 2, 29),
            io.heapy.krogu.time.chrono.HijrahDate.of(1_445, 8, 19),
            io.heapy.krogu.time.chrono.MinguoDate.of(113, 2, 29),
            io.heapy.krogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
        )
        javaDates.zip(kroguDates).forEach { (javaDate, kroguDate) ->
            assertEquals(javaFormatter.format(javaDate), kroguFormatter.format(kroguDate))
        }

        listOf(
            "ISO|2024-02-29",
            "Japanese|2024-02-29",
            "Hijrah-umalqura|1445-08-19",
            "Minguo|0113-02-29",
            "ThaiBuddhist|2567-02-29",
        ).forEach { text ->
            val javaParsed = javaFormatter.parse(text)
            val kroguParsed = kroguFormatter.parse(text)
            assertEquals(
                java.time.chrono.Chronology.from(javaParsed).id,
                io.heapy.krogu.time.chrono.Chronology.from(kroguParsed).id,
                text,
            )
            assertEquals(
                java.time.chrono.Chronology.from(javaParsed).date(javaParsed).toEpochDay(),
                io.heapy.krogu.time.chrono.Chronology.from(kroguParsed)
                    .date(kroguParsed)
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
        val kroguExplicit = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.ERA)
            .appendLiteral('|')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.YEAR_OF_ERA)
            .appendLiteral('-')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(io.heapy.krogu.time.temporal.ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        val explicitText = "Japanese|3|6-02-29"
        assertEquals(
            java.time.chrono.JapaneseChronology.INSTANCE
                .date(javaExplicit.parse(explicitText))
                .toEpochDay(),
            io.heapy.krogu.time.chrono.JapaneseChronology
                .date(kroguExplicit.parse(explicitText))
                .toEpochDay(),
        )

        val javaInferred = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
        val kroguInferred = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
        val inferredText = "Japanese|0006-02-29"
        assertEquals(
            java.time.chrono.JapaneseChronology.INSTANCE
                .date(javaInferred.parse(inferredText))
                .toEpochDay(),
            io.heapy.krogu.time.chrono.JapaneseChronology
                .date(kroguInferred.parse(inferredText))
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
                io.heapy.krogu.time.chrono.JapaneseChronology.date(
                    kroguInferred
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
        fun kroguFormatter(caseInsensitive: Boolean): DateTimeFormatter {
            val builder = DateTimeFormatterBuilder()
            if (caseInsensitive) builder.parseCaseInsensitive()
            return builder.appendChronologyId().toFormatter()
        }
        listOf("ThaiBuddhist", "thaibuddhist", "buddhist", "Unknown").forEach { text ->
            listOf(false, true).forEach { caseInsensitive ->
                assertEquals(
                    runCatching { javaFormatter(caseInsensitive).parse(text) }.isSuccess,
                    runCatching { kroguFormatter(caseInsensitive).parse(text) }.isSuccess,
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
        val kroguOptional = DateTimeFormatterBuilder()
            .optionalStart()
            .appendChronologyId()
            .optionalEnd()
            .appendLiteral('!')
            .toFormatter()
        assertEquals(
            javaOptional.format(java.time.LocalTime.NOON),
            kroguOptional.format(io.heapy.krogu.time.LocalTime.NOON),
        )

        val javaPadded = java.time.format.DateTimeFormatterBuilder()
            .padNext(18, '_')
            .appendChronologyId()
            .toFormatter()
        val kroguPadded = DateTimeFormatterBuilder()
            .padNext(18, '_')
            .appendChronologyId()
            .toFormatter()
        assertEquals(
            javaPadded.format(java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
            kroguPadded.format(io.heapy.krogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
        )
        assertEquals(
            java.time.chrono.Chronology.from(javaPadded.parse("______ThaiBuddhist")).id,
            io.heapy.krogu.time.chrono.Chronology.from(
                kroguPadded.parse("______ThaiBuddhist"),
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
        fun kroguFormatter(chronologyFirst: Boolean): DateTimeFormatter {
            val builder = DateTimeFormatterBuilder()
            if (chronologyFirst) builder.appendChronologyId().appendLiteral('|')
            builder.appendValueReduced(
                io.heapy.krogu.time.temporal.ChronoField.YEAR,
                2,
                2,
                io.heapy.krogu.time.LocalDate.of(1950, 1, 1),
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
                kroguFormatter(chronologyFirst).parse(text)
                    .getLong(io.heapy.krogu.time.temporal.ChronoField.YEAR),
            )
        }
    }

    @Test
    fun overridesAndFormatterCompositionMatchJavaTime() {
        val javaChronology = java.time.format.DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .toFormatter()
        val kroguChronology = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .toFormatter()
        val javaFormatter = java.time.format.DateTimeFormatterBuilder()
            .append(javaChronology)
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withChronology(java.time.chrono.JapaneseChronology.INSTANCE)
        val kroguFormatter = DateTimeFormatterBuilder()
            .append(kroguChronology)
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withChronology(io.heapy.krogu.time.chrono.JapaneseChronology)
        val text = "ThaiBuddhist|2567-02-29"
        val javaParsed = javaFormatter.parse(text)
        val kroguParsed = kroguFormatter.parse(text)

        assertEquals(
            java.time.chrono.Chronology.from(javaParsed).id,
            io.heapy.krogu.time.chrono.Chronology.from(kroguParsed).id,
        )
        assertEquals(
            java.time.chrono.Chronology.from(javaParsed).date(javaParsed).toEpochDay(),
            io.heapy.krogu.time.chrono.Chronology.from(kroguParsed)
                .date(kroguParsed)
                .toEpochDay(),
        )
        assertEquals(
            javaFormatter.format(java.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29)),
            kroguFormatter.format(
                io.heapy.krogu.time.chrono.ThaiBuddhistDate.of(2_567, 2, 29),
            ),
        )
    }
}
