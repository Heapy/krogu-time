package io.heapy.krogu.time.zone

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** Service-provider base class and registry for region-based zone rules. */
public abstract class ZoneRulesProvider protected constructor() {
    /** Returns every region ID supplied by this provider. */
    protected abstract fun provideZoneIds(): Set<String>

    /** Returns rules for [zoneId], or `null` to prevent caching when [forCaching] is true. */
    protected abstract fun provideRules(zoneId: String, forCaching: Boolean): ZoneRules?

    /** Returns the known version history for [zoneId]. */
    protected abstract fun provideVersions(zoneId: String): Map<String, ZoneRules>

    /** Checks for provider updates. */
    protected open fun provideRefresh(): Boolean = false

    public companion object {
        private val providers: MutableList<ZoneRulesProvider> = mutableListOf()
        private val zones: MutableMap<String, ZoneRulesProvider> = mutableMapOf()

        init {
            registerProvider(TzdbZoneRulesProvider())
            loadZoneRulesProviders().forEach(::registerProvider)
        }

        /** Returns a snapshot of all registered region IDs. */
        @JvmStatic
        public fun getAvailableZoneIds(): Set<String> = zones.keys.toSet()

        /** Returns rules supplied for [zoneId]. */
        @JvmStatic
        public fun getRules(zoneId: String, forCaching: Boolean): ZoneRules? =
            getProvider(zoneId).provideRules(zoneId, forCaching)

        /** Returns a copy of the version history supplied for [zoneId]. */
        @JvmStatic
        public fun getVersions(zoneId: String): Map<String, ZoneRules> =
            getProvider(zoneId).provideVersions(zoneId).toMap()

        /** Permanently registers [provider]. */
        @JvmStatic
        public fun registerProvider(provider: ZoneRulesProvider) {
            val zoneIds = provider.provideZoneIds()
            zoneIds.forEach { zoneId ->
                if (zoneId in zones) {
                    throw ZoneRulesException(
                        "Unable to register zone as one already registered with that ID: " +
                            "$zoneId, currently loading from provider: $provider",
                    )
                }
            }
            zoneIds.forEach { zoneId -> zones[zoneId] = provider }
            providers += provider
        }

        /** Requests a refresh from every registered provider. */
        @JvmStatic
        public fun refresh(): Boolean {
            var changed = false
            providers.forEach { provider -> changed = provider.provideRefresh() || changed }
            return changed
        }

        private fun getProvider(zoneId: String): ZoneRulesProvider =
            zones[zoneId] ?: if (zones.isEmpty()) {
                throw ZoneRulesException("No time-zone data files registered")
            } else {
                throw ZoneRulesException("Unknown time-zone ID: $zoneId")
            }
    }
}
