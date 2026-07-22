package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoFormattingJavaConformanceTest {
    @Test
    fun genericChronologyFormattingMatchesJavaTime() {
        val javaDateFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val groguDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val javaDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        val groguDateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        val javaZonedFormatter = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV")
        val groguZonedFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV")
        val isoDate = LocalDate.of(2024, 2, 29)
        val javaIsoDate = java.time.LocalDate.of(2024, 2, 29)
        val time = LocalTime.of(12, 30, 45)
        val javaTime = java.time.LocalTime.of(12, 30, 45)
        val zone = ZoneId.of("Europe/Paris")
        val javaZone = java.time.ZoneId.of("Europe/Paris")

        Chronology.getAvailableChronologies().forEach { groguChronology ->
            val javaChronology = java.time.chrono.Chronology.of(groguChronology.id)
            val groguDate: ChronoLocalDate = groguChronology.date(isoDate)
            val javaDate: java.time.chrono.ChronoLocalDate = javaChronology.date(javaIsoDate)
            val groguDateTime: ChronoLocalDateTime<*> = groguDate.atTime(time)
            val javaDateTime: java.time.chrono.ChronoLocalDateTime<*> = javaDate.atTime(javaTime)
            val groguZoned: ChronoZonedDateTime<*> = groguDateTime.atZone(zone)
            val javaZoned: java.time.chrono.ChronoZonedDateTime<*> = javaDateTime.atZone(javaZone)

            assertEquals(javaDate.format(javaDateFormatter), groguDate.format(groguDateFormatter), groguChronology.id)
            assertEquals(
                javaDateTime.format(javaDateTimeFormatter),
                groguDateTime.format(groguDateTimeFormatter),
                groguChronology.id,
            )
            assertEquals(
                javaZoned.format(javaZonedFormatter),
                groguZoned.format(groguZonedFormatter),
                groguChronology.id,
            )
        }
    }
}
