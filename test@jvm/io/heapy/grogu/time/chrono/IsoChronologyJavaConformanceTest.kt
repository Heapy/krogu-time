package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Instant
import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.temporal.ChronoField
import java.time.chrono.IsoChronology as JavaIsoChronology
import java.time.temporal.ChronoField as JavaChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoChronologyJavaConformanceTest {
    @Test
    fun metadataCalendarOperationsAndFactoriesMatchJavaTime() {
        val javaIso = JavaIsoChronology.INSTANCE
        val iso = IsoChronology

        assertEquals(javaIso.id, iso.id)
        assertEquals(javaIso.calendarType, iso.calendarType)
        assertEquals(javaIso.toString(), iso.toString())
        assertEquals(javaIso.isIsoBased, iso.isIsoBased)
        listOf(-400L, -1L, 0L, 1900L, 2000L, 2024L).forEach { year ->
            assertEquals(javaIso.isLeapYear(year), iso.isLeapYear(year), year.toString())
        }
        IsoEra.entries.forEach { era ->
            val javaEra = java.time.chrono.IsoEra.valueOf(era.name)
            assertEquals(javaIso.prolepticYear(javaEra, 2024), iso.prolepticYear(era, 2024))
        }
        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            assertEquals(javaIso.range(javaField).toString(), iso.range(field).toString(), field.name)
        }

        assertEquals(javaIso.date(2024, 2, 29).toString(), iso.date(2024, 2, 29).toString())
        assertEquals(javaIso.dateYearDay(2024, 60).toString(), iso.dateYearDay(2024, 60).toString())
        assertEquals(javaIso.dateEpochDay(-1).toString(), iso.dateEpochDay(-1).toString())
        assertEquals(
            javaIso.localDateTime(java.time.LocalDateTime.parse("2024-02-29T12:30")).toString(),
            iso.localDateTime(LocalDateTime.parse("2024-02-29T12:30")).toString(),
        )
        assertEquals(
            javaIso.zonedDateTime(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC).toString(),
            iso.zonedDateTime(Instant.EPOCH, ZoneOffset.UTC).toString(),
        )
        assertEquals(javaIso.period(1, 2, 3).toString(), iso.period(1, 2, 3).toString())
        assertEquals(
            javaIso.epochSecond(2024, 2, 29, 12, 30, 0, java.time.ZoneOffset.ofHours(2)),
            iso.epochSecond(2024, 2, 29, 12, 30, 0, ZoneOffset.ofHours(2)),
        )
        assertEquals(
            javaIso.date(java.time.LocalDate.of(2024, 2, 29)).toString(),
            iso.date(LocalDate.of(2024, 2, 29)).toString(),
        )
    }
}
