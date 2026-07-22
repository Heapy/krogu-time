package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.format.ResolverStyle
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.ValueRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AbstractChronologyTest {
    @Test
    fun suppliesJavaCompatibleOrderingEqualityHashingAndText() {
        val first = TestChronology("Test")
        val equal = TestChronology("Test")
        val later = TestChronology("Test-Z")
        val direct = object : Chronology by IsoChronology {
            override val id: String = "Test"
        }

        assertEquals(0, first.compareTo(equal))
        assertTrue(first < later)
        assertEquals(first, equal)
        assertEquals(first.hashCode(), equal.hashCode())
        assertEquals(first::class.hashCode() xor first.id.hashCode(), first.hashCode())
        assertNotEquals(first as Any, direct as Any)
        assertEquals("Test", first.toString())
    }

    @Test
    fun resolvesDateFieldsThroughTheChronologyDefault() {
        val chronology = TestChronology("Test")
        val fields: MutableMap<TemporalField, Long> = mutableMapOf(
            ChronoField.YEAR to 2024L,
            ChronoField.MONTH_OF_YEAR to 2L,
            ChronoField.DAY_OF_MONTH to 29L,
        )

        assertEquals(
            LocalDate.of(2024, 2, 29),
            chronology.resolveDate(fields, ResolverStyle.STRICT),
        )
        assertTrue(fields.isEmpty())
    }

    @Test
    fun builtInChronologiesExtendTheRecommendedBaseClass() {
        Chronology.getAvailableChronologies().forEach { chronology ->
            assertIs<AbstractChronology>(chronology)
        }
    }

    @Test
    fun suppliesJavaCompatibleDefaultPeriodAndInstantZonedFactories() {
        val chronology = TestChronology("Test")
        val period = chronology.period(1, 2, 3)
        val instant = Instant.ofEpochSecond(1_718_450_123, 456_789_000)
        val zone = ZoneId.of("Europe/Paris")
        val zoned = chronology.zonedDateTime(instant, zone)

        assertSame(chronology, period.chronology)
        assertEquals(1L, period.get(ChronoUnit.YEARS))
        assertEquals(2L, period.get(ChronoUnit.MONTHS))
        assertEquals(3L, period.get(ChronoUnit.DAYS))
        assertEquals(instant, zoned.toInstant())
        assertEquals(zone, zoned.zone)
    }

    private class TestChronology(
        override val id: String,
    ) : AbstractChronology() {
        override val calendarType: String? = null

        override fun date(
            prolepticYear: Int,
            month: Int,
            dayOfMonth: Int,
        ): ChronoLocalDate = IsoChronology.date(prolepticYear, month, dayOfMonth)

        override fun dateYearDay(prolepticYear: Int, dayOfYear: Int): ChronoLocalDate =
            IsoChronology.dateYearDay(prolepticYear, dayOfYear)

        override fun dateEpochDay(epochDay: Long): ChronoLocalDate =
            IsoChronology.dateEpochDay(epochDay)

        override fun date(temporal: TemporalAccessor): ChronoLocalDate =
            IsoChronology.date(temporal)

        override fun isLeapYear(prolepticYear: Long): Boolean =
            IsoChronology.isLeapYear(prolepticYear)

        override fun prolepticYear(era: Era, yearOfEra: Int): Int =
            IsoChronology.prolepticYear(era, yearOfEra)

        override fun eraOf(eraValue: Int): Era = IsoChronology.eraOf(eraValue)

        override fun eras(): List<Era> = IsoChronology.eras()

        override fun range(field: ChronoField): ValueRange = IsoChronology.range(field)
    }
}
