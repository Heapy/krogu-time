package io.heapy.grogu.time

import io.heapy.grogu.time.zone.ZoneRules
import io.heapy.grogu.time.temporal.ChronoUnit
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId as JavaZoneId
import java.time.ZoneOffset as JavaZoneOffset
import java.time.ZonedDateTime as JavaZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ZonedDateTimeJavaConformanceTest {
    @Test
    fun localInstantStrictAndZoneFactoriesMatchJavaTime() {
        val zone = testZone()
        val javaZone = JavaZoneId.of("Europe/Paris")
        val localDateTimes = listOf(
            LocalDateTime.of(2024, 3, 31, 1, 59, 59, 999_999_999),
            LocalDateTime.of(2024, 3, 31, 2, 30),
            LocalDateTime.of(2024, 3, 31, 3, 0),
            LocalDateTime.of(2024, 10, 27, 2, 30),
            LocalDateTime.of(2024, 10, 27, 3, 0),
        )
        localDateTimes.forEach { local ->
            val javaLocal = local.toJava()
            assertEquals(
                JavaZonedDateTime.of(javaLocal, javaZone).toString(),
                ZonedDateTime.of(local, zone).toString(),
                local.toString(),
            )
            listOf(null, STANDARD, SUMMER).forEach { preferred ->
                assertEquals(
                    JavaZonedDateTime.ofLocal(javaLocal, javaZone, preferred?.toJava()).toString(),
                    ZonedDateTime.ofLocal(local, zone, preferred).toString(),
                    "$local preferred=$preferred",
                )
            }
        }

        val instants = listOf(
            Instant.parse("2024-03-31T00:59:59.999999999Z"),
            Instant.parse("2024-03-31T01:00:00Z"),
            Instant.parse("2024-10-27T01:00:00Z"),
        )
        instants.forEach { instant ->
            assertEquals(
                JavaZonedDateTime.ofInstant(java.time.Instant.parse(instant.toString()), javaZone).toString(),
                ZonedDateTime.ofInstant(instant, zone).toString(),
            )
        }
    }

    @Test
    fun arithmeticAndUntilAcrossTransitionsMatchJavaTime() {
        val zone = testZone()
        val javaZone = JavaZoneId.of("Europe/Paris")
        val starts = listOf(
            LocalDateTime.of(2024, 3, 30, 12, 0),
            LocalDateTime.of(2024, 10, 26, 12, 0),
            LocalDateTime.of(2024, 10, 27, 2, 30),
        )
        starts.forEach { local ->
            val value = ZonedDateTime.of(local, zone)
            val javaValue = JavaZonedDateTime.of(local.toJava(), javaZone)
            assertEquals(javaValue.plusDays(1).toString(), value.plusDays(1).toString())
            assertEquals(javaValue.plusHours(24).toString(), value.plusHours(24).toString())
            assertEquals(javaValue.plusHours(1).toString(), value.plusHours(1).toString())
        }

        val spring = ZonedDateTime.of(starts[0], zone)
        val javaSpring = JavaZonedDateTime.of(starts[0].toJava(), javaZone)
        val springEnd = spring.plusDays(1)
        val javaSpringEnd = javaSpring.plusDays(1)
        assertEquals(
            javaSpring.until(javaSpringEnd, java.time.temporal.ChronoUnit.DAYS),
            spring.until(springEnd, ChronoUnit.DAYS),
        )
        assertEquals(
            javaSpring.until(javaSpringEnd, java.time.temporal.ChronoUnit.HOURS),
            spring.until(springEnd, ChronoUnit.HOURS),
        )
    }

    private fun testZone(): ZoneId = object : ZoneId() {
        override val id: String = "Europe/Paris"
        override val rules: ZoneRules = ZonedDateTimeTest.europeanRules()
    }

    private fun LocalDateTime.toJava(): JavaLocalDateTime = JavaLocalDateTime.of(
        year,
        monthValue,
        dayOfMonth,
        hour,
        minute,
        second,
        nano,
    )

    private fun ZoneOffset.toJava(): JavaZoneOffset = JavaZoneOffset.ofTotalSeconds(totalSeconds)

    private companion object {
        val STANDARD: ZoneOffset = ZoneOffset.ofHours(1)
        val SUMMER: ZoneOffset = ZoneOffset.ofHours(2)
    }
}
