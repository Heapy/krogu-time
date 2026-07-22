package io.heapy.grogu.time.format

import io.heapy.grogu.time.DateTimeException
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.chrono.JapaneseChronology
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.chrono.MinguoDate
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DateTimeFormatterBuilderChronologyIdTest {
    @Test
    fun formatsEveryAvailableChronologyId() {
        val builder = DateTimeFormatterBuilder()
        assertSame(builder, builder.appendChronologyId())
        val formatter = builder.toFormatter()
        val dates = listOf(
            LocalDate.of(2024, 2, 29),
            JapaneseDate.of(2024, 2, 29),
            HijrahDate.of(1_445, 8, 19),
            MinguoDate.of(113, 2, 29),
            ThaiBuddhistDate.of(2_567, 2, 29),
        )

        assertEquals(
            listOf("ISO", "Japanese", "Hijrah-umalqura", "Minguo", "ThaiBuddhist"),
            dates.map(formatter::format),
        )
        assertFailsWith<DateTimeException> { formatter.format(LocalTime.NOON) }
    }

    @Test
    fun parsedIdControlsChronologySpecificDateResolution() {
        val formatter = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("uuuu-MM-dd")
            .toFormatter()

        val thaiParsed = formatter.parse("ThaiBuddhist|2567-02-29")
        assertSame(ThaiBuddhistChronology, Chronology.from(thaiParsed))
        assertEquals(
            ThaiBuddhistDate.of(2_567, 2, 29),
            ThaiBuddhistChronology.date(thaiParsed),
        )

        val japaneseParsed = formatter.parse("Japanese|2024-02-29")
        assertSame(JapaneseChronology, Chronology.from(japaneseParsed))
        assertEquals(JapaneseDate.of(2024, 2, 29), JapaneseChronology.date(japaneseParsed))
    }

    @Test
    fun resolvesChronologySpecificYearsOfEra() {
        val explicitEra = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendValue(ChronoField.ERA)
            .appendLiteral('|')
            .appendValue(ChronoField.YEAR_OF_ERA)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter()
        assertEquals(
            JapaneseDate.of(2024, 2, 29),
            JapaneseChronology.date(explicitEra.parse("Japanese|3|6-02-29")),
        )

        val inferredEra = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
        assertEquals(
            JapaneseDate.of(2024, 2, 29),
            JapaneseChronology.date(inferredEra.parse("Japanese|0006-02-29")),
        )
        val strict = inferredEra.withResolverStyle(ResolverStyle.STRICT)
        val unresolved = strict.parse("Japanese|0006-02-29")
        assertSame(JapaneseChronology, Chronology.from(unresolved))
        assertFailsWith<DateTimeException> { JapaneseChronology.date(unresolved) }
    }

    @Test
    fun appliesSequentialCaseSettingsAndRejectsAliases() {
        val sensitive = DateTimeFormatterBuilder().appendChronologyId().toFormatter()
        assertSame(ThaiBuddhistChronology, Chronology.from(sensitive.parse("ThaiBuddhist")))
        assertFailsWith<DateTimeParseException> { sensitive.parse("thaibuddhist") }
        assertFailsWith<DateTimeParseException> { sensitive.parse("buddhist") }

        val insensitive = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendChronologyId()
            .toFormatter()
        assertSame(ThaiBuddhistChronology, Chronology.from(insensitive.parse("thaibuddhist")))
        assertSame(IsoChronology, Chronology.from(insensitive.parse("iso")))
    }

    @Test
    fun optionalSectionsAndPaddingTreatTheIdAsOneElement() {
        val optional = DateTimeFormatterBuilder()
            .optionalStart()
            .appendChronologyId()
            .optionalEnd()
            .appendLiteral('!')
            .toFormatter()
        assertEquals("!", optional.format(LocalTime.NOON))
        assertSame(IsoChronology, Chronology.from(optional.parse("!")))
        assertFailsWith<DateTimeParseException> { optional.parse("Unknown!") }

        val padded = DateTimeFormatterBuilder()
            .padNext(18, '_')
            .appendChronologyId()
            .toFormatter()
        assertEquals("______ThaiBuddhist", padded.format(ThaiBuddhistDate.of(2_567, 2, 29)))
        assertSame(
            ThaiBuddhistChronology,
            Chronology.from(padded.parse("______ThaiBuddhist")),
        )
    }

    @Test
    fun chronologyAwareReducedValuesFollowIdsBeforeOrAfterThem() {
        fun formatter(chronologyFirst: Boolean): DateTimeFormatter {
            val builder = DateTimeFormatterBuilder()
            if (chronologyFirst) builder.appendChronologyId().appendLiteral('|')
            builder.appendValueReduced(
                ChronoField.YEAR,
                2,
                2,
                LocalDate.of(1950, 1, 1),
            )
            if (!chronologyFirst) builder.appendLiteral('|').appendChronologyId()
            return builder.toFormatter()
        }

        assertEquals(
            2_493,
            formatter(chronologyFirst = true)
                .parse("ThaiBuddhist|93")
                .getLong(ChronoField.YEAR),
        )
        assertEquals(
            2_493,
            formatter(chronologyFirst = false)
                .parse("93|ThaiBuddhist")
                .getLong(ChronoField.YEAR),
        )
    }

    @Test
    fun parsedIdsWinOverOverridesAndComposeIntoFollowingElements() {
        val chronology = DateTimeFormatterBuilder()
            .appendChronologyId()
            .appendLiteral('|')
            .toFormatter()
        val formatter = DateTimeFormatterBuilder()
            .append(chronology)
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withChronology(JapaneseChronology)

        val parsed = formatter.parse("ThaiBuddhist|2567-02-29")
        assertSame(ThaiBuddhistChronology, Chronology.from(parsed))
        assertEquals(
            ThaiBuddhistDate.of(2_567, 2, 29),
            ThaiBuddhistChronology.date(parsed),
        )
        assertEquals(
            "Japanese|2024-02-29",
            formatter.format(ThaiBuddhistDate.of(2_567, 2, 29)),
        )
    }
}
