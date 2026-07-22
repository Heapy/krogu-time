package io.heapy.grogu.time

internal actual fun systemDefaultZoneId(): String = java.util.TimeZone.getDefault().id
