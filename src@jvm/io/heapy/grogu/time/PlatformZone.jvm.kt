package io.heapy.grogu.time

internal actual fun systemDefaultZoneId(): String = java.time.ZoneId.systemDefault().id
