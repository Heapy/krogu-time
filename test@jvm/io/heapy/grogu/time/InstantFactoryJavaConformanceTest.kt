package io.heapy.grogu.time

import java.time.Instant as JavaInstant
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.LocalTime as JavaLocalTime
import java.time.OffsetTime as JavaOffsetTime
import java.time.ZoneId as JavaZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class InstantFactoryJavaConformanceTest {
    @Test
    fun instantZoneFactoriesMatchJavaTime() {
        val instants = listOf(
            Instant.MIN,
            Instant.ofEpochSecond(-1, 123),
            Instant.EPOCH,
            Instant.parse("2024-03-31T00:30:00Z"),
            Instant.parse("2024-03-31T01:30:00Z"),
            Instant.MAX,
        )
        val zones = listOf("Z", "+05:45", "Europe/Paris", "America/New_York", "Pacific/Apia")

        instants.forEach { instant ->
            zones.forEach { zoneId ->
                val javaInstant = JavaInstant.ofEpochSecond(instant.epochSecond, instant.nano.toLong())
                val javaZone = JavaZoneId.of(zoneId)
                val zone = ZoneId.of(zoneId)
                assertSameOutcome(
                    javaOperation = { JavaLocalTime.ofInstant(javaInstant, javaZone).toString() },
                    kotlinOperation = { LocalTime.ofInstant(instant, zone).toString() },
                    context = "LocalTime instant=$instant zone=$zoneId",
                )
                assertSameOutcome(
                    javaOperation = { JavaLocalDateTime.ofInstant(javaInstant, javaZone).toString() },
                    kotlinOperation = { LocalDateTime.ofInstant(instant, zone).toString() },
                    context = "LocalDateTime instant=$instant zone=$zoneId",
                )
                assertSameOutcome(
                    javaOperation = { JavaOffsetTime.ofInstant(javaInstant, javaZone).toString() },
                    kotlinOperation = { OffsetTime.ofInstant(instant, zone).toString() },
                    context = "OffsetTime instant=$instant zone=$zoneId",
                )
            }
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), context)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }
}
