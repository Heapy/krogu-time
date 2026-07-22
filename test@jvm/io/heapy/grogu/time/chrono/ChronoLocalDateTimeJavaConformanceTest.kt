package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoUnit
import java.time.ZoneOffset as JavaZoneOffset
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoLocalDateTimeJavaConformanceTest {
    @Test
    fun isoGenericDateTimeBehaviorMatchesJavaTime() {
        val javaDateTime: java.time.chrono.ChronoLocalDateTime<*> =
            java.time.LocalDateTime.of(2024, 2, 29, 23, 59, 59, 123_456_789)
        val dateTime: ChronoLocalDateTime<*> =
            LocalDateTime.of(2024, 2, 29, 23, 59, 59, 123_456_789)

        assertEquals(javaDateTime.chronology.toString(), dateTime.chronology.toString())
        assertEquals(javaDateTime.toLocalDate().toString(), dateTime.toLocalDate().toString())
        assertEquals(javaDateTime.toLocalTime().toString(), dateTime.toLocalTime().toString())
        assertEquals(
            javaDateTime.plus(2, JavaChronoUnit.NANOS).toString(),
            dateTime.plus(2, ChronoUnit.NANOS).toString(),
        )
        assertEquals(
            javaDateTime.toEpochSecond(JavaZoneOffset.ofHours(2)),
            dateTime.toEpochSecond(ZoneOffset.ofHours(2)),
        )
        assertEquals(
            javaDateTime.toInstant(JavaZoneOffset.ofHours(2)).toString(),
            dateTime.toInstant(ZoneOffset.ofHours(2)).toString(),
        )

        val javaOther: java.time.chrono.ChronoLocalDateTime<*> =
            java.time.LocalDateTime.of(2024, 3, 1, 0, 0)
        val other: ChronoLocalDateTime<*> = LocalDateTime.of(2024, 3, 1, 0, 0)
        assertEquals(javaDateTime.compareTo(javaOther), dateTime.compareTo(other))
        assertEquals(javaDateTime.isBefore(javaOther), dateTime.isBefore(other))
        assertEquals(javaDateTime.isAfter(javaOther), dateTime.isAfter(other))
        assertEquals(javaDateTime.isEqual(javaOther), dateTime.isEqual(other))
        assertEquals(
            java.time.chrono.ChronoLocalDateTime.timeLineOrder().compare(javaDateTime, javaOther),
            ChronoLocalDateTime.timeLineOrder().compare(dateTime, other),
        )
    }

    @Test
    fun genericFactoriesMatchJavaTime() {
        val javaSource = java.time.LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val source = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)

        assertEquals(
            java.time.chrono.Chronology.of("ISO").localDateTime(javaSource).toString(),
            Chronology.of("ISO").localDateTime(source).toString(),
        )
        assertEquals(
            java.time.chrono.ChronoLocalDateTime.from(javaSource).toString(),
            ChronoLocalDateTime.from(source).toString(),
        )
    }
}
