package io.heapy.grogu.time.zone

import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ZoneRulesProviderTest {
    @Test
    fun registersLooksUpVersionsAndRefreshesProviders() {
        val provider = TestProvider("Test/Fixed-${nextId()}")
        ZoneRulesProvider.registerProvider(provider)

        assertTrue(provider.zoneId in ZoneRulesProvider.getAvailableZoneIds())
        assertTrue(provider.zoneId in ZoneId.getAvailableZoneIds())
        assertSame(provider.rules, ZoneRulesProvider.getRules(provider.zoneId, false))
        assertEquals(mapOf("2026a" to provider.rules), ZoneRulesProvider.getVersions(provider.zoneId))
        assertTrue(ZoneRulesProvider.refresh())
        assertFalse(ZoneRulesProvider.refresh())

        val zone = ZoneId.of(provider.zoneId)
        assertEquals(provider.zoneId, zone.id)
        assertSame(provider.rules, zone.rules)
        assertEquals(ZoneOffset.ofHours(3), zone.normalized())
        assertTrue(provider.cachingRequests.contains(true))
    }

    @Test
    fun supportsDynamicProvidersThatPreventZoneRuleCaching() {
        val provider = TestProvider("Test/Dynamic-${nextId()}", dynamic = true)
        ZoneRulesProvider.registerProvider(provider)
        val zone = ZoneId.of(provider.zoneId)
        assertSame(provider.rules, zone.rules)
        assertEquals(listOf(true, false), provider.cachingRequests.takeLast(2))
    }

    @Test
    fun rejectsDuplicateZoneIdsAndUnknownLookups() {
        val zoneId = "Test/Conflict-${nextId()}"
        ZoneRulesProvider.registerProvider(TestProvider(zoneId))
        assertFailsWith<ZoneRulesException> {
            ZoneRulesProvider.registerProvider(TestProvider(zoneId))
        }
        assertFailsWith<ZoneRulesException> {
            ZoneRulesProvider.getRules("Test/Missing-${nextId()}", false)
        }
    }

    private class TestProvider(
        val zoneId: String,
        private val dynamic: Boolean = false,
    ) : ZoneRulesProvider() {
        val rules: ZoneRules = ZoneRules.of(ZoneOffset.ofHours(3))
        val cachingRequests: MutableList<Boolean> = mutableListOf()
        private var refreshAvailable: Boolean = true

        override fun provideZoneIds(): Set<String> = setOf(zoneId)

        override fun provideRules(zoneId: String, forCaching: Boolean): ZoneRules? {
            if (zoneId != this.zoneId) throw ZoneRulesException("Unknown time-zone ID: $zoneId")
            cachingRequests += forCaching
            return if (dynamic && forCaching) null else rules
        }

        override fun provideVersions(zoneId: String): Map<String, ZoneRules> {
            if (zoneId != this.zoneId) throw ZoneRulesException("Unknown time-zone ID: $zoneId")
            return mapOf("2026a" to rules)
        }

        override fun provideRefresh(): Boolean {
            val refreshed = refreshAvailable
            refreshAvailable = false
            return refreshed
        }
    }

    private companion object {
        private var identifier: Int = 0

        fun nextId(): Int = identifier++
    }
}
