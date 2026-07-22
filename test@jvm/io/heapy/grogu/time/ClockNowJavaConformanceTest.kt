package io.heapy.grogu.time

import io.heapy.grogu.time.zone.ZoneRules
import java.time.Clock as JavaClock
import java.time.Instant as JavaInstant
import java.time.ZoneId as JavaZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockNowJavaConformanceTest {
    @Test
    fun deterministicNowFactoriesMatchJavaTime() {
        val instantText = "2024-10-27T01:30:45.123456789Z"
        val zone = TestZoneId("Europe/Paris", ZonedDateTimeTest.europeanRules())
        val clock = Clock.fixed(Instant.parse(instantText), zone)
        val javaClock = JavaClock.fixed(
            JavaInstant.parse(instantText),
            JavaZoneId.of("Europe/Paris"),
        )

        assertEquals(java.time.Instant.now(javaClock).toString(), Instant.now(clock).toString())
        assertEquals(java.time.LocalDate.now(javaClock).toString(), LocalDate.now(clock).toString())
        assertEquals(java.time.LocalTime.now(javaClock).toString(), LocalTime.now(clock).toString())
        assertEquals(java.time.LocalDateTime.now(javaClock).toString(), LocalDateTime.now(clock).toString())
        assertEquals(java.time.OffsetTime.now(javaClock).toString(), OffsetTime.now(clock).toString())
        assertEquals(java.time.OffsetDateTime.now(javaClock).toString(), OffsetDateTime.now(clock).toString())
        assertEquals(java.time.ZonedDateTime.now(javaClock).toString(), ZonedDateTime.now(clock).toString())
        assertEquals(java.time.Year.now(javaClock).toString(), Year.now(clock).toString())
        assertEquals(java.time.YearMonth.now(javaClock).toString(), YearMonth.now(clock).toString())
        assertEquals(java.time.MonthDay.now(javaClock).toString(), MonthDay.now(clock).toString())
    }

    private class TestZoneId(
        override val id: String,
        override val rules: ZoneRules,
    ) : ZoneId()
}
