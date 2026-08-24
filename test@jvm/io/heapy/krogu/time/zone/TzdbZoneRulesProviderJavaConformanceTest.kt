package io.heapy.krogu.time.zone

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.ZoneId
import java.time.Instant as JavaInstant
import java.time.ZoneId as JavaZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TzdbZoneRulesProviderJavaConformanceTest {
    @Test
    fun bundledZoneIdsVersionsAndOffsetsMatchJavaTime() {
        val javaZoneIds = JavaZoneId.getAvailableZoneIds()
        val bundledZoneIds = ZoneId.getAvailableZoneIds()
            .filterNot { it.startsWith("Test/") }
            .toSet()
        assertEquals(emptySet(), bundledZoneIds - javaZoneIds, "extra bundled IDs")
        val instants = listOf(
            "1900-01-01T00:00:00Z",
            "1970-01-01T00:00:00Z",
            "2000-06-30T12:00:00Z",
            "2024-01-15T12:00:00Z",
            "2024-07-15T12:00:00Z",
        )

        bundledZoneIds.forEach { zoneId ->
            val javaRules = JavaZoneId.of(zoneId).rules
            val rules = ZoneId.of(zoneId).rules
            assertEquals(javaRules.isFixedOffset, rules.isFixedOffset, zoneId)
            assertEquals(setOf("2025a"), ZoneRulesProvider.getVersions(zoneId).keys, zoneId)
            instants.forEach { text ->
                val javaInstant = JavaInstant.parse(text)
                val instant = Instant.parse(text)
                assertEquals(javaRules.getOffset(javaInstant).toString(), rules.getOffset(instant).toString(), "$zoneId $text")
                assertEquals(javaRules.getStandardOffset(javaInstant).toString(), rules.getStandardOffset(instant).toString(), "$zoneId $text")
                assertEquals(javaRules.getDaylightSavings(javaInstant).toString(), rules.getDaylightSavings(instant).toString(), "$zoneId $text")
            }
        }
    }
}
