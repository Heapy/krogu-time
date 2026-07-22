package io.heapy.grogu.time.zone

import kotlin.test.Test
import kotlin.test.assertTrue

class ZoneRulesProviderServiceLoadingAndroidTest {
    @Test
    fun discoversProvidersFromMetaInfServices() {
        assertTrue(TestServiceZoneRulesProvider.ZONE_ID in ZoneRulesProvider.getAvailableZoneIds())
    }
}
