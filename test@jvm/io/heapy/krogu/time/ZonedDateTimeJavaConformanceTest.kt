package io.heapy.krogu.time

import io.heapy.krogu.time.zone.ZoneRules
import io.heapy.krogu.time.zone.ZoneRulesProvider
import io.heapy.krogu.time.temporal.ChronoUnit
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

    @Test
    fun parsingAndOffsetDateTimeZoneCompositionMatchJavaTime() {
        val zoneId = "Test/ZonedDateTimeParserJvm"
        ZoneRulesProvider.registerProvider(TestProvider(zoneId, ZonedDateTimeTest.europeanRules()))
        val ourTexts = listOf(
            "2024-03-31T02:30+01:00[$zoneId]",
            "2024-10-27T02:30+02:00[$zoneId]",
            "2024-10-27T02:30+01:00[$zoneId]",
        )
        ourTexts.forEach { text ->
            val actual = ZonedDateTime.parse(text)
            val expected = JavaZonedDateTime.parse(text.replace(zoneId, "Europe/Paris"))
            assertEquals(expected.toInstant().toString(), actual.toInstant().toString(), text)
            assertEquals(expected.toLocalDateTime().toString(), actual.dateTime.toString(), text)
            assertEquals(expected.offset.toString(), actual.offset.toString(), text)
        }

        val ours = OffsetDateTime.parse("2024-06-01T12:00+01:00")
        val java = java.time.OffsetDateTime.parse("2024-06-01T12:00+01:00")
        val zone = testZone()
        val javaZone = JavaZoneId.of("Europe/Paris")
        assertEquals(java.atZoneSameInstant(javaZone).toString(), ours.atZoneSameInstant(zone).toString())
        assertEquals(java.atZoneSimilarLocal(javaZone).toString(), ours.atZoneSimilarLocal(zone).toString())
        assertEquals(java.toZonedDateTime().toString(), ours.toZonedDateTime().toString())
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

    private companion object {
        val STANDARD: ZoneOffset = ZoneOffset.ofHours(1)
        val SUMMER: ZoneOffset = ZoneOffset.ofHours(2)
    }
}
