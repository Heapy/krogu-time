package io.heapy.krogu.time

internal actual fun systemDefaultZoneId(): String = java.time.ZoneId.systemDefault().id
