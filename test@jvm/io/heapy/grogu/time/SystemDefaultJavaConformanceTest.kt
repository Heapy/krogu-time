package io.heapy.grogu.time

import java.time.ZoneId as JavaZoneId
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemDefaultJavaConformanceTest {
    @Test
    fun systemDefaultZoneTracksJavaTime() {
        val original = TimeZone.getDefault()
        try {
            listOf("America/New_York", "Asia/Kathmandu", "GMT+03:00").forEach { zoneId ->
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
                val expected = JavaZoneId.systemDefault().id
                assertEquals(expected, ZoneId.systemDefault().id)
                assertEquals(expected, Clock.systemDefaultZone().zone.id)
                assertEquals(expected, ZonedDateTime.now().zone.id)
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
