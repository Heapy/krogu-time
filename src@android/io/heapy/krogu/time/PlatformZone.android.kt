package io.heapy.krogu.time

internal actual fun systemDefaultZoneId(): String = java.util.TimeZone.getDefault().id
