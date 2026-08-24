package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterZoneOverrideJavaConformanceTest {
    @Test
    fun formattingAndParsingWithZoneOverridesMatchesJavaTime() {
        val instantText = "2024-02-29T23:30:00Z"
        val javaInstant = java.time.Instant.parse(instantText)
        val kroguInstant = io.heapy.krogu.time.Instant.parse(instantText)
        val zoneIds = listOf("+02:00", "Europe/Paris")

        zoneIds.forEach { zoneId ->
            val javaZone = java.time.ZoneId.of(zoneId)
            val kroguZone = io.heapy.krogu.time.ZoneId.of(zoneId)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(javaZone)
                    .format(javaInstant),
                DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(kroguZone)
                    .format(kroguInstant),
                zoneId,
            )
        }

        val javaZone = java.time.ZoneId.of("Europe/Paris")
        val kroguZone = io.heapy.krogu.time.ZoneId.of("Europe/Paris")
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(javaZone)
            .parse("2024-02-29T12:30")
        val kroguParsed = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(kroguZone)
            .parse("2024-02-29T12:30")
        assertEquals(
            java.time.ZonedDateTime.from(javaParsed).toString(),
            io.heapy.krogu.time.ZonedDateTime.from(kroguParsed).toString(),
        )
    }
}
