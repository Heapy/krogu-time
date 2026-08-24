package io.heapy.krogu.time.zone

import io.heapy.krogu.time.Duration
import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TzdbZoneRulesProviderTest {
    @Test
    fun registersBundledRegionIdsAndVersionedRules() {
        val availableIds = ZoneId.getAvailableZoneIds()
        assertTrue("Europe/Paris" in availableIds)
        assertTrue("America/New_York" in availableIds)
        assertTrue("Asia/Kathmandu" in availableIds)
        assertTrue("Pacific/Apia" in availableIds)

        val versions = ZoneRulesProvider.getVersions("Europe/Paris")
        assertEquals(1, versions.size)
        assertEquals("2025a", versions.keys.single())
        assertSameRules(ZoneId.of("Europe/Paris").rules, versions.values.single())
    }

    @Test
    fun resolvesHistoricOffsetsAndRecurringDaylightSavingRules() {
        val paris = ZoneId.of("Europe/Paris").rules
        assertEquals(ZoneOffset.ofHours(1), paris.getOffset(Instant.parse("2024-01-15T12:00:00Z")))
        assertEquals(ZoneOffset.ofHours(2), paris.getOffset(Instant.parse("2024-07-15T12:00:00Z")))
        assertEquals(ZoneOffset.ofHours(1), paris.getStandardOffset(Instant.parse("2024-07-15T12:00:00Z")))
        assertEquals(Duration.ofHours(1), paris.getDaylightSavings(Instant.parse("2024-07-15T12:00:00Z")))
        assertFalse(paris.isFixedOffset)
        assertTrue(paris.getTransitions().isNotEmpty())
        assertEquals(2, paris.getTransitionRules().size)

        val gap = LocalDateTime.parse("2024-03-31T02:30")
        assertTrue(paris.getValidOffsets(gap).isEmpty())
        assertTrue(assertNotNull(paris.getTransition(gap)).isGap)

        val newYork = ZoneId.of("America/New_York").rules
        assertEquals(ZoneOffset.ofHours(-5), newYork.getOffset(Instant.parse("2024-01-15T12:00:00Z")))
        assertEquals(ZoneOffset.ofHours(-4), newYork.getOffset(Instant.parse("2024-07-15T12:00:00Z")))
        assertEquals(
            ZoneOffset.ofHoursMinutes(5, 45),
            ZoneId.of("Asia/Kathmandu").rules.getOffset(Instant.parse("2024-01-15T12:00:00Z")),
        )
    }

    private fun assertSameRules(first: ZoneRules, second: ZoneRules) {
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }
}
