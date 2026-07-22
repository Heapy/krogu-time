package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.format.DateTimeParseException
import io.heapy.grogu.time.zone.ZoneOffsetTransition
import io.heapy.grogu.time.zone.ZoneOffsetTransitionRule
import io.heapy.grogu.time.zone.ZoneRules
import io.heapy.grogu.time.zone.ZoneRulesProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ZonedDateTimeTest {
    @Test
    fun resolvesNormalGapAndOverlapLocalDateTimes() {
        val zone = testZone()
        val normal = ZonedDateTime.of(LocalDateTime.of(2024, 6, 1, 12, 0), zone)
        assertEquals(SUMMER, normal.offset)
        assertEquals(zone, normal.zone)

        val gap = ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 2, 30), zone)
        assertEquals(LocalDateTime.of(2024, 3, 31, 3, 30), gap.dateTime)
        assertEquals(SUMMER, gap.offset)

        val overlap = LocalDateTime.of(2024, 10, 27, 2, 30)
        assertEquals(SUMMER, ZonedDateTime.ofLocal(overlap, zone, null).offset)
        assertEquals(STANDARD, ZonedDateTime.ofLocal(overlap, zone, STANDARD).offset)
        assertEquals(STANDARD, ZonedDateTime.ofStrict(overlap, STANDARD, zone).offset)
        assertFailsWith<DateTimeException> {
            ZonedDateTime.ofStrict(LocalDateTime.of(2024, 3, 31, 2, 30), STANDARD, zone)
        }
    }

    @Test
    fun createsFromInstantsAndConvertsZonesWithoutChangingTheInstant() {
        val zone = testZone()
        val instant = Instant.parse("2024-06-01T10:00:00.123456789Z")
        val value = ZonedDateTime.ofInstant(instant, zone)
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0, 0, 123_456_789), value.dateTime)
        assertEquals(instant, value.toInstant())
        assertEquals(instant.epochSecond, value.toEpochSecond())
        assertEquals(value, instant.atZone(zone))
        assertEquals(value, value.dateTime.atZone(zone))
        assertEquals(value, value.date.atTime(value.time).atZone(zone))

        val utc = value.withZoneSameInstant(ZoneOffset.UTC)
        assertEquals(instant, utc.toInstant())
        assertEquals(LocalDateTime.of(2024, 6, 1, 10, 0, 0, 123_456_789), utc.dateTime)
        assertEquals(value.dateTime, value.withZoneSameLocal(ZoneOffset.UTC).dateTime)
        assertSame(value.offset, value.withFixedOffsetZone().zone)
    }

    @Test
    fun switchesOffsetsInsideOverlapsAndExposesComponentsQueriesAndText() {
        val zone = testZone()
        val earlier = ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 30), zone)
        val later = earlier.withLaterOffsetAtOverlap()
        assertEquals(SUMMER, earlier.offset)
        assertEquals(STANDARD, later.offset)
        assertEquals(3_600L, later.toEpochSecond() - earlier.toEpochSecond())
        assertEquals(earlier, later.withEarlierOffsetAtOverlap())
        assertEquals(2024, earlier.year)
        assertEquals(Month.OCTOBER, earlier.month)
        assertEquals(2, earlier.hour)
        assertSame(zone, earlier.query(TemporalQueries.zoneId()))
        assertSame(SUMMER, earlier.query(TemporalQueries.offset()))
        assertEquals("2024-10-27T02:30+02:00[Test/Europe]", earlier.toString())
        assertEquals(earlier.toOffsetDateTime(), OffsetDateTime.of(earlier.dateTime, earlier.offset))
    }

    @Test
    fun distinguishesCalendarArithmeticFromElapsedTimeAcrossTransitions() {
        val zone = testZone()
        val spring = ZonedDateTime.of(LocalDateTime.of(2024, 3, 30, 12, 0), zone)
        val nextCalendarDay = spring.plusDays(1)
        val nextTwentyFourHours = spring.plusHours(24)
        assertEquals("2024-03-31T12:00+02:00[Test/Europe]", nextCalendarDay.toString())
        assertEquals("2024-03-31T13:00+02:00[Test/Europe]", nextTwentyFourHours.toString())
        assertEquals(23 * 3_600L, nextCalendarDay.toEpochSecond() - spring.toEpochSecond())
        assertEquals(24 * 3_600L, nextTwentyFourHours.toEpochSecond() - spring.toEpochSecond())
        assertEquals(1, spring.until(nextCalendarDay, ChronoUnit.DAYS))
        assertEquals(23, spring.until(nextCalendarDay, ChronoUnit.HOURS))

        val earlier = ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 30), zone)
        val later = earlier.plusHours(1)
        assertEquals(earlier.dateTime, later.dateTime)
        assertEquals(STANDARD, later.offset)
        assertEquals(later, earlier.with(ChronoField.OFFSET_SECONDS, STANDARD.totalSeconds.toLong()))
        assertEquals(earlier.toInstant().plusSeconds(3_600), later.toInstant())
    }

    @Test
    fun parsesOffsetAndProviderBackedRegionZonesUsingTheParsedInstant() {
        val zoneId = "Test/ZonedDateTimeParser"
        ZoneRulesProvider.registerProvider(TestProvider(zoneId, europeanRules()))

        val parsed = ZonedDateTime.parse("2024-06-01T12:00:00.123456789+02:00[$zoneId]")
        assertEquals("2024-06-01T12:00:00.123456789+02:00[$zoneId]", parsed.toString())

        val conflictingOffset = ZonedDateTime.parse("2024-06-01T12:00+01:00[$zoneId]")
        assertEquals("2024-06-01T13:00+02:00[$zoneId]", conflictingOffset.toString())

        val fixed = ZonedDateTime.parse("2024-06-01T12:00+02:00")
        assertSame(fixed.offset, fixed.zone)
        assertEquals("2024-06-01T12:00+02:00", fixed.toString())
        assertFailsWith<DateTimeParseException> {
            ZonedDateTime.parse("2024-06-01T12:00+02:00[]")
        }
    }

    @Test
    fun composesDatesAndOffsetDateTimesWithZones() {
        val midnightGap = ZoneOffsetTransition.of(
            LocalDateTime.of(2024, 1, 1, 23, 30),
            ZoneOffset.UTC,
            STANDARD,
        )
        val midnightZone = TestZoneId(
            "Test/MidnightGap",
            ZoneRules.of(
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                emptyList(),
                listOf(midnightGap),
                emptyList(),
            ),
        )
        assertEquals(
            "2024-01-02T00:30+01:00[Test/MidnightGap]",
            LocalDate.of(2024, 1, 2).atStartOfDay(midnightZone).toString(),
        )

        val zone = testZone()
        val offsetDateTime = OffsetDateTime.of(
            LocalDateTime.of(2024, 6, 1, 12, 0),
            STANDARD,
        )
        assertEquals("2024-06-01T13:00+02:00[Test/Europe]", offsetDateTime.atZoneSameInstant(zone).toString())
        assertEquals("2024-06-01T12:00+02:00[Test/Europe]", offsetDateTime.atZoneSimilarLocal(zone).toString())
        assertEquals("2024-06-01T12:00+01:00", offsetDateTime.toZonedDateTime().toString())
    }

    private fun testZone(): ZoneId = TestZoneId("Test/Europe", europeanRules())

    private class TestZoneId(
        override val id: String,
        override val rules: ZoneRules,
    ) : ZoneId()

    private class TestProvider(
        private val zoneId: String,
        private val zoneRules: ZoneRules,
    ) : ZoneRulesProvider() {
        override fun provideZoneIds(): Set<String> = setOf(zoneId)

        override fun provideRules(zoneId: String, forCaching: Boolean): ZoneRules {
            require(zoneId == this.zoneId)
            return zoneRules
        }

        override fun provideVersions(zoneId: String): Map<String, ZoneRules> {
            require(zoneId == this.zoneId)
            return mapOf("test" to zoneRules)
        }
    }

    companion object {
        val STANDARD: ZoneOffset = ZoneOffset.ofHours(1)
        val SUMMER: ZoneOffset = ZoneOffset.ofHours(2)

        fun europeanRules(): ZoneRules = ZoneRules.of(
            STANDARD,
            STANDARD,
            emptyList(),
            listOf(
                ZoneOffsetTransition.of(
                    LocalDateTime.of(2023, 3, 26, 2, 0),
                    STANDARD,
                    SUMMER,
                ),
                ZoneOffsetTransition.of(
                    LocalDateTime.of(2023, 10, 29, 3, 0),
                    SUMMER,
                    STANDARD,
                ),
            ),
            listOf(
                recurring(Month.MARCH, STANDARD, SUMMER),
                recurring(Month.OCTOBER, SUMMER, STANDARD),
            ),
        )

        fun recurring(
            month: Month,
            before: ZoneOffset,
            after: ZoneOffset,
        ): ZoneOffsetTransitionRule = ZoneOffsetTransitionRule.of(
            month,
            -1,
            DayOfWeek.SUNDAY,
            LocalTime.of(1, 0),
            false,
            ZoneOffsetTransitionRule.TimeDefinition.UTC,
            STANDARD,
            before,
            after,
        )
    }
}
