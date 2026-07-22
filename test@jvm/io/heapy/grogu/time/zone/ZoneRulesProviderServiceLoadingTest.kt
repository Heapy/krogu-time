package io.heapy.grogu.time.zone

import io.heapy.grogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZoneRulesProviderServiceLoadingTest {
    @Test
    fun discoversProvidersFromMetaInfServices() {
        assertTrue(TestServiceZoneRulesProvider.ZONE_ID in ZoneRulesProvider.getAvailableZoneIds())
        assertEquals(
            ZoneOffset.ofHoursMinutes(12, 34),
            ZoneRulesProvider.getRules(TestServiceZoneRulesProvider.ZONE_ID, false)?.getOffset(
                io.heapy.grogu.time.Instant.EPOCH,
            ),
        )
        assertEquals(
            setOf(TestServiceZoneRulesProvider.VERSION),
            ZoneRulesProvider.getVersions(TestServiceZoneRulesProvider.ZONE_ID).keys,
        )
    }
}
