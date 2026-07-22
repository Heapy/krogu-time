package io.heapy.grogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterZoneOverrideJavaConformanceTest {
    @Test
    fun formattingAndParsingWithZoneOverridesMatchesJavaTime() {
        val instantText = "2024-02-29T23:30:00Z"
        val javaInstant = java.time.Instant.parse(instantText)
        val groguInstant = io.heapy.grogu.time.Instant.parse(instantText)
        val zoneIds = listOf("+02:00", "Europe/Paris")

        zoneIds.forEach { zoneId ->
            val javaZone = java.time.ZoneId.of(zoneId)
            val groguZone = io.heapy.grogu.time.ZoneId.of(zoneId)
            assertEquals(
                java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(javaZone)
                    .format(javaInstant),
                DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(groguZone)
                    .format(groguInstant),
                zoneId,
            )
        }

        val javaZone = java.time.ZoneId.of("Europe/Paris")
        val groguZone = io.heapy.grogu.time.ZoneId.of("Europe/Paris")
        val javaParsed = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(javaZone)
            .parse("2024-02-29T12:30")
        val groguParsed = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(groguZone)
            .parse("2024-02-29T12:30")
        assertEquals(
            java.time.ZonedDateTime.from(javaParsed).toString(),
            io.heapy.grogu.time.ZonedDateTime.from(groguParsed).toString(),
        )
    }
}
