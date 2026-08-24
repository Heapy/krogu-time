package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoFormattingJavaConformanceTest {
    @Test
    fun genericChronologyFormattingMatchesJavaTime() {
        val javaDateFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val kroguDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val javaDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        val kroguDateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        val javaZonedFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV")
        val kroguZonedFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV")
        val isoDate = LocalDate.of(2024, 2, 29)
        val javaIsoDate = java.time.LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(12, 30, 45)
        val javaTime = java.time.LocalTime.of(12, 30, 45)
        val zone = ZoneId.of("Europe/Paris")
        val javaZone = java.time.ZoneId.of("Europe/Paris")

        Chronology.getAvailableChronologies().forEach { kroguChronology ->
            val javaChronology = java.time.chrono.Chronology.of(kroguChronology.id)
            val kroguDate: ChronoLocalDate = kroguChronology.date(isoDate)
            val javaDate: java.time.chrono.ChronoLocalDate = javaChronology.date(javaIsoDate)
            val kroguDateTime: ChronoLocalDateTime<*> = kroguDate.atTime(time)
            val javaDateTime: java.time.chrono.ChronoLocalDateTime<*> = javaDate.atTime(javaTime)
            val kroguZoned: ChronoZonedDateTime<*> = kroguDateTime.atZone(zone)
            val javaZoned: java.time.chrono.ChronoZonedDateTime<*> = javaDateTime.atZone(javaZone)

            assertEquals(javaDate.format(javaDateFormatter), kroguDate.format(kroguDateFormatter), kroguChronology.id)
            assertEquals(
                javaDateTime.format(javaDateTimeFormatter),
                kroguDateTime.format(kroguDateTimeFormatter),
                kroguChronology.id,
            )
            assertEquals(
                javaZoned.format(javaZonedFormatter),
                kroguZoned.format(kroguZonedFormatter),
                kroguChronology.id,
            )
        }
    }
}
