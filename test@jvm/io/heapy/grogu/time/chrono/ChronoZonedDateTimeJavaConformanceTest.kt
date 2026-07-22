package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import java.time.ZoneOffset as JavaZoneOffset
import java.time.chrono.MinguoDate as JavaMinguoDate
import java.time.chrono.ThaiBuddhistDate as JavaThaiBuddhistDate
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoZonedDateTimeJavaConformanceTest {
    @Test
    fun isoGenericZonedDateTimeBehaviorMatchesJavaTime() {
        val javaZoned: java.time.chrono.ChronoZonedDateTime<*> = java.time.ZonedDateTime.of(
            java.time.LocalDateTime.of(2024, 2, 29, 12, 0, 0, 123_456_789),
            JavaZoneOffset.ofHours(2),
        )
        val zoned: ChronoZonedDateTime<*> = ZonedDateTime.of(
            LocalDateTime.of(2024, 2, 29, 12, 0, 0, 123_456_789),
            ZoneOffset.ofHours(2),
        )

        assertEquals(javaZoned.chronology.toString(), zoned.chronology.toString())
        assertEquals(javaZoned.toLocalDateTime().toString(), zoned.toLocalDateTime().toString())
        assertEquals(javaZoned.offset.toString(), zoned.offset.toString())
        assertEquals(javaZoned.zone.toString(), zoned.zone.toString())
        assertEquals(javaZoned.toEpochSecond(), zoned.toEpochSecond())
        assertEquals(javaZoned.toInstant().toString(), zoned.toInstant().toString())
        assertEquals(
            javaZoned.plus(1, JavaChronoUnit.NANOS).toString(),
            zoned.plus(1, ChronoUnit.NANOS).toString(),
        )

        val javaSameInstant: java.time.chrono.ChronoZonedDateTime<*> = java.time.ZonedDateTime.of(
            java.time.LocalDateTime.of(2024, 2, 29, 10, 0, 0, 123_456_789),
            JavaZoneOffset.UTC,
        )
        val sameInstant: ChronoZonedDateTime<*> = ZonedDateTime.of(
            LocalDateTime.of(2024, 2, 29, 10, 0, 0, 123_456_789),
            ZoneOffset.UTC,
        )
        assertEquals(javaZoned.compareTo(javaSameInstant), zoned.compareTo(sameInstant))
        assertEquals(javaZoned.isBefore(javaSameInstant), zoned.isBefore(sameInstant))
        assertEquals(javaZoned.isAfter(javaSameInstant), zoned.isAfter(sameInstant))
        assertEquals(javaZoned.isEqual(javaSameInstant), zoned.isEqual(sameInstant))
        assertEquals(
            java.time.chrono.ChronoZonedDateTime.timeLineOrder()
                .compare(javaZoned, javaSameInstant),
            ChronoZonedDateTime.timeLineOrder().compare(zoned, sameInstant),
        )

        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            val javaResult = runCatching { javaZoned.get(javaField) }
            val kotlinResult = runCatching { zoned.get(field) }
            assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), field.toString())
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                field.toString(),
            )
            assertEquals(
                javaResult.exceptionOrNull()?.message,
                kotlinResult.exceptionOrNull()?.message,
                field.toString(),
            )
        }
    }

    @Test
    fun genericFactoriesMatchJavaTime() {
        val javaInstant = java.time.Instant.parse("2024-02-29T10:00:00.123456789Z")
        val instant = Instant.parse("2024-02-29T10:00:00.123456789Z")

        assertEquals(
            java.time.chrono.Chronology.of("ISO")
                .zonedDateTime(javaInstant, JavaZoneOffset.ofHours(2))
                .toString(),
            Chronology.of("ISO").zonedDateTime(instant, ZoneOffset.ofHours(2)).toString(),
        )
    }

    @Test
    fun crossChronologyLocalDateTimeAdjustmentMatchesJavaTime() {
        val javaBase = JavaMinguoDate.of(113, 1, 1)
            .atTime(java.time.LocalTime.NOON)
            .atZone(JavaZoneOffset.UTC)
        val javaAdjuster = JavaThaiBuddhistDate.of(2567, 2, 29)
            .atTime(java.time.LocalTime.of(13, 14, 15, 123_456_789))
        val base = MinguoDate.of(113, 1, 1)
            .atTime(LocalTime.NOON)
            .atZone(ZoneOffset.UTC)
        val adjuster = ThaiBuddhistDate.of(2567, 2, 29)
            .atTime(LocalTime.of(13, 14, 15, 123_456_789))

        val javaResult = runCatching { javaBase.with(javaAdjuster) }
        val kotlinResult = runCatching { base.with(adjuster) }

        assertEquals(javaResult.getOrNull()?.toString(), kotlinResult.getOrNull()?.toString())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
        assertEquals(
            javaResult.exceptionOrNull()?.message,
            kotlinResult.exceptionOrNull()?.message,
        )
    }
}
