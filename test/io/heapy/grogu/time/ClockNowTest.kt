package io.heapy.grogu.time

import io.heapy.grogu.time.zone.ZoneRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ClockNowTest {
    @Test
    fun createsEverySupportedCurrentValueFromOneInjectedClock() {
        val zone = TestZoneId("Test/Europe", ZonedDateTimeTest.europeanRules())
        val instant = Instant.parse("2024-06-01T10:15:30.123456789Z")
        val clock = Clock.fixed(instant, zone)

        assertSame(instant, Instant.now(clock))
        assertEquals(LocalDate.of(2024, 6, 1), LocalDate.now(clock))
        assertEquals(LocalTime.of(12, 15, 30, 123_456_789), LocalTime.now(clock))
        assertEquals(
            LocalDateTime.of(2024, 6, 1, 12, 15, 30, 123_456_789),
            LocalDateTime.now(clock),
        )
        assertEquals(
            OffsetTime.of(LocalTime.of(12, 15, 30, 123_456_789), ZoneOffset.ofHours(2)),
            OffsetTime.now(clock),
        )
        assertEquals(
            OffsetDateTime.of(
                LocalDateTime.of(2024, 6, 1, 12, 15, 30, 123_456_789),
                ZoneOffset.ofHours(2),
            ),
            OffsetDateTime.now(clock),
        )
        assertEquals(
            "2024-06-01T12:15:30.123456789+02:00[Test/Europe]",
            ZonedDateTime.now(clock).toString(),
        )
        assertEquals(Year.of(2024), Year.now(clock))
        assertEquals(YearMonth.of(2024, 6), YearMonth.now(clock))
        assertEquals(MonthDay.of(6, 1), MonthDay.now(clock))
    }

    @Test
    fun explicitZoneFactoriesUseSystemClocksForThatZone() {
        val offset = ZoneOffset.ofHours(2)
        assertEquals(offset, ZonedDateTime.now(offset).zone)
        assertEquals(offset, OffsetDateTime.now(offset).offset)
        assertEquals(offset, OffsetTime.now(offset).offset)
        assertEquals(offset, LocalDateTime.now(offset).atOffset(offset).offset)
        assertEquals(offset, LocalDate.now(offset).atStartOfDay().atOffset(offset).offset)
    }

    private class TestZoneId(
        override val id: String,
        override val rules: ZoneRules,
    ) : ZoneId()
}
