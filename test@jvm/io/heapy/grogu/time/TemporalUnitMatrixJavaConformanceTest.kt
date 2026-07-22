package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.chrono.MinguoDate
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalUnitMatrixJavaConformanceTest {
    @Test
    fun everyStandardUnitAndRepresentativeAmountMatchesJavaTime() {
        val amounts = listOf(Long.MIN_VALUE, -2L, -1L, 0L, 1L, 2L, Long.MAX_VALUE)
        val mismatches = temporals().flatMap { temporal ->
            ChronoUnit.entries.flatMap { kotlinUnit ->
                val javaUnit = java.time.temporal.ChronoUnit.valueOf(kotlinUnit.name)
                amounts.mapNotNull { amount ->
                    val javaSnapshot = snapshot(temporal.java, javaUnit, amount)
                    val kotlinSnapshot = snapshot(temporal.kotlin, kotlinUnit, amount)
                    if (javaSnapshot == kotlinSnapshot) {
                        null
                    } else {
                        "${temporal.name} $kotlinUnit $amount: " +
                            "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                    }
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun temporals(): List<TemporalPair> {
        val javaDate = java.time.LocalDate.of(2024, 3, 30)
        val kotlinDate = LocalDate.of(2024, 3, 30)
        val javaTime = java.time.LocalTime.of(23, 58, 59, 123_456_789)
        val kotlinTime = LocalTime.of(23, 58, 59, 123_456_789)
        val javaDateTime = java.time.LocalDateTime.of(javaDate, javaTime)
        val kotlinDateTime = LocalDateTime.of(kotlinDate, kotlinTime)
        val javaOffset = java.time.ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val kotlinOffset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val javaZone = java.time.ZoneId.of("Europe/Paris")
        val kotlinZone = ZoneId.of("Europe/Paris")
        val javaJapaneseDate = java.time.chrono.JapaneseDate.of(2019, 5, 1)
        val kotlinJapaneseDate = JapaneseDate.of(2019, 5, 1)
        val javaHijrahDate = java.time.chrono.HijrahDate.of(1445, 8, 19)
        val kotlinHijrahDate = HijrahDate.of(1445, 8, 19)
        val javaMinguoDate = java.time.chrono.MinguoDate.of(113, 3, 30)
        val kotlinMinguoDate = MinguoDate.of(113, 3, 30)
        val javaThaiDate = java.time.chrono.ThaiBuddhistDate.of(2567, 3, 30)
        val kotlinThaiDate = ThaiBuddhistDate.of(2567, 3, 30)
        return listOf(
            TemporalPair("Year", java.time.Year.of(2024), Year.of(2024)),
            TemporalPair("YearMonth", java.time.YearMonth.of(2024, 3), YearMonth.of(2024, 3)),
            TemporalPair("LocalDate", javaDate, kotlinDate),
            TemporalPair("LocalTime", javaTime, kotlinTime),
            TemporalPair("LocalDateTime", javaDateTime, kotlinDateTime),
            TemporalPair(
                "Instant",
                java.time.Instant.ofEpochSecond(1_709_250_139, 123_456_789),
                Instant.ofEpochSecond(1_709_250_139, 123_456_789),
            ),
            TemporalPair(
                "OffsetTime",
                java.time.OffsetTime.of(javaTime, javaOffset),
                OffsetTime.of(kotlinTime, kotlinOffset),
            ),
            TemporalPair(
                "OffsetDateTime",
                java.time.OffsetDateTime.of(javaDateTime, javaOffset),
                OffsetDateTime.of(kotlinDateTime, kotlinOffset),
            ),
            TemporalPair(
                "ZonedDateTime",
                java.time.ZonedDateTime.of(javaDateTime, javaZone),
                ZonedDateTime.of(kotlinDateTime, kotlinZone),
            ),
            TemporalPair("JapaneseDate", javaJapaneseDate, kotlinJapaneseDate),
            TemporalPair("HijrahDate", javaHijrahDate, kotlinHijrahDate),
            TemporalPair("MinguoDate", javaMinguoDate, kotlinMinguoDate),
            TemporalPair("ThaiBuddhistDate", javaThaiDate, kotlinThaiDate),
            TemporalPair(
                "JapaneseDateTime",
                javaJapaneseDate.atTime(javaTime),
                kotlinJapaneseDate.atTime(kotlinTime),
            ),
            TemporalPair(
                "HijrahDateTime",
                javaHijrahDate.atTime(javaTime),
                kotlinHijrahDate.atTime(kotlinTime),
            ),
            TemporalPair(
                "MinguoDateTime",
                javaMinguoDate.atTime(javaTime),
                kotlinMinguoDate.atTime(kotlinTime),
            ),
            TemporalPair(
                "ThaiBuddhistDateTime",
                javaThaiDate.atTime(javaTime),
                kotlinThaiDate.atTime(kotlinTime),
            ),
            TemporalPair(
                "JapaneseZonedDateTime",
                javaJapaneseDate.atTime(javaTime).atZone(javaZone),
                kotlinJapaneseDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            TemporalPair(
                "HijrahZonedDateTime",
                javaHijrahDate.atTime(javaTime).atZone(javaZone),
                kotlinHijrahDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            TemporalPair(
                "MinguoZonedDateTime",
                javaMinguoDate.atTime(javaTime).atZone(javaZone),
                kotlinMinguoDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            TemporalPair(
                "ThaiBuddhistZonedDateTime",
                javaThaiDate.atTime(javaTime).atZone(javaZone),
                kotlinThaiDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
        )
    }

    private fun snapshot(
        temporal: java.time.temporal.Temporal,
        unit: java.time.temporal.ChronoUnit,
        amount: Long,
    ): UnitSnapshot = UnitSnapshot(
        supported = outcome { temporal.isSupported(unit) },
        plus = outcome { temporal.plus(amount, unit) },
        minus = outcome { temporal.minus(amount, unit) },
        untilPlus = outcome { temporal.until(temporal.plus(amount, unit), unit) },
    )

    private fun snapshot(
        temporal: Temporal,
        unit: ChronoUnit,
        amount: Long,
    ): UnitSnapshot = UnitSnapshot(
        supported = outcome { temporal.isSupported(unit) },
        plus = outcome { temporal.plus(amount, unit) },
        minus = outcome { temporal.minus(amount, unit) },
        untilPlus = outcome { temporal.until(temporal.plus(amount, unit), unit) },
    )

    private fun outcome(block: () -> Any?): Outcome = runCatching(block).fold(
        onSuccess = { value -> Outcome(value.toString(), null) },
        onFailure = { exception -> Outcome(null, exception.javaClass.simpleName) },
    )

    private data class TemporalPair(
        val name: String,
        val java: java.time.temporal.Temporal,
        val kotlin: Temporal,
    )

    private data class UnitSnapshot(
        val supported: Outcome,
        val plus: Outcome,
        val minus: Outcome,
        val untilPlus: Outcome,
    )

    private data class Outcome(
        val value: String?,
        val exception: String?,
    )
}
