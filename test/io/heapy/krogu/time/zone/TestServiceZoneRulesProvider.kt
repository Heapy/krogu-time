package io.heapy.krogu.time.zone

import io.heapy.krogu.time.ZoneOffset

public class TestServiceZoneRulesProvider : ZoneRulesProvider() {
    private val rules: ZoneRules = ZoneRules.of(ZoneOffset.ofHoursMinutes(12, 34))

    override fun provideZoneIds(): Set<String> = setOf(ZONE_ID)

    override fun provideRules(zoneId: String, forCaching: Boolean): ZoneRules =
        rules.takeIf { zoneId == ZONE_ID }
            ?: throw ZoneRulesException("Unknown time-zone ID: $zoneId")

    override fun provideVersions(zoneId: String): Map<String, ZoneRules> =
        mapOf(VERSION to provideRules(zoneId, false))

    public companion object {
        public const val ZONE_ID: String = "Test/ServiceLoaded"
        public const val VERSION: String = "service-test"
    }
}
