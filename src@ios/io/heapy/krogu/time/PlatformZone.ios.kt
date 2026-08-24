package io.heapy.krogu.time

import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

internal actual fun systemDefaultZoneId(): String = NSTimeZone.localTimeZone.name
