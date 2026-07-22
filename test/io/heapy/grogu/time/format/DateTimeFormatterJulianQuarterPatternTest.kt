package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.IsoFields
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateTimeFormatterJulianQuarterPatternTest {
    @Test
    fun formatsAndParsesModifiedJulianDayPatterns() {
        listOf(
            LocalDate.of(1858, 11, 16) to "-1",
            LocalDate.of(1858, 11, 17) to "0",
            LocalDate.EPOCH to "40587",
            LocalDate.of(2024, 2, 29) to "60369",
        ).forEach { (date, text) ->
            val formatter = DateTimeFormatter.ofPattern("g")
            assertEquals(text, formatter.format(date))
            assertEquals(date, formatter.parse(text, LocalDate::from))
        }

        val padded = DateTimeFormatter.ofPattern("gggggg")
        assertEquals("040587", padded.format(LocalDate.EPOCH))
        assertEquals(LocalDate.EPOCH, padded.parse("040587", LocalDate::from))
    }

    @Test
    fun formatsNumericQuarterPatterns() {
        val date = LocalDate.of(2024, 5, 31)

        mapOf(
            "Q" to "2",
            "QQ" to "02",
            "q" to "2",
            "qq" to "02",
        ).forEach { (pattern, expected) ->
            assertEquals(expected, DateTimeFormatter.ofPattern(pattern).format(date), pattern)
        }
    }

    @Test
    fun resolvesYearQuarterAndDayOfQuarter() {
        fun formatter(): DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu-'Q'Q-")
            .appendValue(IsoFields.DAY_OF_QUARTER)
            .toFormatter()

        assertEquals(
            LocalDate.of(2024, 2, 29),
            formatter().parse("2024-Q1-60", LocalDate::from),
        )
        assertFailsWith<DateTimeParseException> {
            formatter().withResolverStyle(ResolverStyle.STRICT)
                .parse("2023-Q1-91", LocalDate::from)
        }
        assertEquals(
            LocalDate.of(2023, 4, 1),
            formatter().withResolverStyle(ResolverStyle.SMART)
                .parse("2023-Q1-91", LocalDate::from),
        )
        assertEquals(
            LocalDate.of(2024, 1, 1),
            formatter().withResolverStyle(ResolverStyle.LENIENT)
                .parse("2023-Q5-1", LocalDate::from),
        )
    }

    @Test
    fun crossChecksResolvedJulianAndCalendarDates() {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("g uuuu-MM-dd")
            .toFormatter()

        assertEquals(
            LocalDate.EPOCH,
            formatter.parse("40587 1970-01-01", LocalDate::from),
        )
        assertFailsWith<DateTimeParseException> {
            formatter.parse("40587 1970-01-02", LocalDate::from)
        }
    }

    @Test
    fun exposesNumericFieldDescriptionsAndWidthValidation() {
        assertEquals("Value(ModifiedJulianDay)", DateTimeFormatter.ofPattern("g").toString())
        assertEquals(
            "Value(ModifiedJulianDay,2,19,NORMAL)",
            DateTimeFormatter.ofPattern("gg").toString(),
        )
        assertEquals("Value(QuarterOfYear)", DateTimeFormatter.ofPattern("Q").toString())
        assertEquals("Value(QuarterOfYear,2)", DateTimeFormatter.ofPattern("qq").toString())

        listOf("g".repeat(20), "Q".repeat(6), "q".repeat(6)).forEach { pattern ->
            assertFailsWith<IllegalArgumentException>(pattern) {
                DateTimeFormatter.ofPattern(pattern)
            }
        }
    }

    @Test
    fun retainsAnUnresolvedQuarterWithoutTheOtherDateFields() {
        assertEquals(
            3,
            DateTimeFormatter.ofPattern("Q").parse("3").getLong(IsoFields.QUARTER_OF_YEAR),
        )
        assertEquals(
            2024,
            DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR)
                .toFormatter()
                .parse("2024")
                .getLong(ChronoField.YEAR),
        )
    }

    @Test
    fun repeatsCustomFieldResolutionWhenAResolverChangesTheFieldSet() {
        val formatter = DateTimeFormatterBuilder()
            .appendValue(FirstResolvingField)
            .toFormatter()

        assertEquals(LocalDate.EPOCH, formatter.parse("0", LocalDate::from))
    }

    private object FirstResolvingField : TestField("FirstResolvingField") {
        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor? {
            fieldValues[EpochDayResolvingField] = requireNotNull(fieldValues.remove(this))
            return null
        }
    }

    private object EpochDayResolvingField : TestField("EpochDayResolvingField") {
        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor = LocalDate.ofEpochDay(requireNotNull(fieldValues.remove(this)))
    }

    private abstract class TestField(
        private val displayName: String,
    ) : TemporalField {
        override val baseUnit: TemporalUnit = ChronoUnit.DAYS
        override val rangeUnit: TemporalUnit = ChronoUnit.FOREVER
        override val range: ValueRange = ValueRange.of(0, 100)
        override val isDateBased: Boolean = true
        override val isTimeBased: Boolean = false

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = false

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = range

        override fun getFrom(temporal: TemporalAccessor): Long = 0

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R = temporal

        override fun toString(): String = displayName
    }
}
