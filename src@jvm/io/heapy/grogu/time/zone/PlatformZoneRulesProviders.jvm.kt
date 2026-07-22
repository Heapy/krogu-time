package io.heapy.grogu.time.zone

import java.util.ServiceLoader

internal actual fun loadZoneRulesProviders(): List<ZoneRulesProvider> =
    ServiceLoader.load(ZoneRulesProvider::class.java).toList()
