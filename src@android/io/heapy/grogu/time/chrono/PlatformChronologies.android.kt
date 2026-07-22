package io.heapy.grogu.time.chrono

import java.util.ServiceLoader

internal actual fun loadChronologies(): List<Chronology> =
    ServiceLoader.load(Chronology::class.java).toList()
