package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoLocalDateJavaConformanceTest {
    @Test
    fun isoImplementationAndDefaultBehaviorMatchJavaTime() {
        val javaDate: java.time.chrono.ChronoLocalDate = java.time.LocalDate.of(2024, 2, 29)
        val date: ChronoLocalDate = LocalDate.of(2024, 2, 29)

        assertEquals(javaDate.chronology.toString(), date.chronology.toString())
        assertEquals(javaDate.era.toString(), date.era.toString())
        assertEquals(javaDate.isLeapYear, date.isLeapYear)
        assertEquals(javaDate.lengthOfMonth(), date.lengthOfMonth())
        assertEquals(javaDate.lengthOfYear(), date.lengthOfYear())
        assertEquals(javaDate.toEpochDay(), date.toEpochDay())
        assertEquals(
            javaDate.plus(2, JavaChronoUnit.MONTHS).toString(),
            date.plus(2, ChronoUnit.MONTHS).toString(),
        )
        assertEquals(
            javaDate.with(JavaChronoField.DAY_OF_MONTH, 1).toString(),
            date.with(ChronoField.DAY_OF_MONTH, 1).toString(),
        )

        val javaOther: java.time.chrono.ChronoLocalDate = java.time.LocalDate.of(2024, 3, 1)
        val other: ChronoLocalDate = LocalDate.of(2024, 3, 1)
        assertEquals(javaDate.compareTo(javaOther), date.compareTo(other))
        assertEquals(javaDate.isBefore(javaOther), date.isBefore(other))
        assertEquals(javaDate.isAfter(javaOther), date.isAfter(other))
        assertEquals(javaDate.isEqual(javaOther), date.isEqual(other))
        assertEquals(
            java.time.chrono.ChronoLocalDate.timeLineOrder().compare(javaDate, javaOther),
            ChronoLocalDate.timeLineOrder().compare(date, other),
        )
    }
}
