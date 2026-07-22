package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.HijrahEra
import io.heapy.grogu.time.chrono.IsoEra
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.chrono.JapaneseEra
import io.heapy.grogu.time.chrono.MinguoDate
import io.heapy.grogu.time.chrono.MinguoEra
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.chrono.ThaiBuddhistEra
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalAccessorFieldMatrixJavaConformanceTest {
    @Test
    fun everyStandardFieldMatchesJavaTimeAcrossAccessors() {
        val mismatches = accessors().flatMap { accessor ->
            ChronoField.entries.mapNotNull { kotlinField ->
                val javaField = java.time.temporal.ChronoField.valueOf(kotlinField.name)
                val javaSnapshot = snapshot(accessor.java, javaField)
                val kotlinSnapshot = snapshot(accessor.kotlin, kotlinField)
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${accessor.name} $kotlinField: Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun everyStandardFieldAdjustmentMatchesJavaTimeAtBoundaryValues() {
        val mismatches = accessors().flatMap { accessor ->
            val javaTemporal = accessor.java as? java.time.temporal.Temporal
                ?: return@flatMap emptyList()
            val kotlinTemporal = accessor.kotlin as? Temporal
                ?: return@flatMap emptyList()
            ChronoField.entries.flatMap { kotlinField ->
                val javaField = java.time.temporal.ChronoField.valueOf(kotlinField.name)
                val values = listOf(
                    Long.MIN_VALUE,
                    -1,
                    0,
                    1,
                    javaField.range().minimum,
                    javaField.range().maximum,
                    Long.MAX_VALUE,
                ).distinct()
                values.mapNotNull { value ->
                    val javaOutcome = outcome { javaTemporal.with(javaField, value) }
                    val kotlinOutcome = outcome { kotlinTemporal.with(kotlinField, value) }
                    if (javaOutcome == kotlinOutcome) {
                        null
                    } else {
                        "${accessor.name} $kotlinField $value: " +
                            "Java=$javaOutcome, Kotlin=$kotlinOutcome"
                    }
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun accessors(): List<AccessorPair> {
        val javaDate = java.time.LocalDate.of(2024, 2, 29)
        val kotlinDate = LocalDate.of(2024, 2, 29)
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
        val javaMinguoDate = java.time.chrono.MinguoDate.of(113, 2, 29)
        val kotlinMinguoDate = MinguoDate.of(113, 2, 29)
        val javaThaiDate = java.time.chrono.ThaiBuddhistDate.of(2567, 2, 29)
        val kotlinThaiDate = ThaiBuddhistDate.of(2567, 2, 29)
        return listOf(
            AccessorPair("DayOfWeek", java.time.DayOfWeek.THURSDAY, DayOfWeek.THURSDAY),
            AccessorPair("Month", java.time.Month.FEBRUARY, Month.FEBRUARY),
            AccessorPair("IsoEra", java.time.chrono.IsoEra.CE, IsoEra.CE),
            AccessorPair("JapaneseEra", java.time.chrono.JapaneseEra.REIWA, JapaneseEra.REIWA),
            AccessorPair("HijrahEra", java.time.chrono.HijrahEra.AH, HijrahEra.AH),
            AccessorPair("MinguoEra", java.time.chrono.MinguoEra.ROC, MinguoEra.ROC),
            AccessorPair("ThaiBuddhistEra", java.time.chrono.ThaiBuddhistEra.BE, ThaiBuddhistEra.BE),
            AccessorPair("Year", java.time.Year.of(2024), Year.of(2024)),
            AccessorPair("MonthDay", java.time.MonthDay.of(2, 29), MonthDay.of(2, 29)),
            AccessorPair("YearMonth", java.time.YearMonth.of(2024, 2), YearMonth.of(2024, 2)),
            AccessorPair("LocalDate", javaDate, kotlinDate),
            AccessorPair("LocalTime", javaTime, kotlinTime),
            AccessorPair("LocalDateTime", javaDateTime, kotlinDateTime),
            AccessorPair(
                "Instant",
                java.time.Instant.ofEpochSecond(1_709_250_139, 123_456_789),
                Instant.ofEpochSecond(1_709_250_139, 123_456_789),
            ),
            AccessorPair("ZoneOffset", javaOffset, kotlinOffset),
            AccessorPair(
                "OffsetTime",
                java.time.OffsetTime.of(javaTime, javaOffset),
                OffsetTime.of(kotlinTime, kotlinOffset),
            ),
            AccessorPair(
                "OffsetDateTime",
                java.time.OffsetDateTime.of(javaDateTime, javaOffset),
                OffsetDateTime.of(kotlinDateTime, kotlinOffset),
            ),
            AccessorPair(
                "ZonedDateTime",
                java.time.ZonedDateTime.of(javaDateTime, javaZone),
                ZonedDateTime.of(kotlinDateTime, kotlinZone),
            ),
            AccessorPair("JapaneseDate", javaJapaneseDate, kotlinJapaneseDate),
            AccessorPair("HijrahDate", javaHijrahDate, kotlinHijrahDate),
            AccessorPair("MinguoDate", javaMinguoDate, kotlinMinguoDate),
            AccessorPair("ThaiBuddhistDate", javaThaiDate, kotlinThaiDate),
            AccessorPair(
                "JapaneseDateTime",
                javaJapaneseDate.atTime(javaTime),
                kotlinJapaneseDate.atTime(kotlinTime),
            ),
            AccessorPair(
                "HijrahDateTime",
                javaHijrahDate.atTime(javaTime),
                kotlinHijrahDate.atTime(kotlinTime),
            ),
            AccessorPair(
                "MinguoDateTime",
                javaMinguoDate.atTime(javaTime),
                kotlinMinguoDate.atTime(kotlinTime),
            ),
            AccessorPair(
                "ThaiBuddhistDateTime",
                javaThaiDate.atTime(javaTime),
                kotlinThaiDate.atTime(kotlinTime),
            ),
            AccessorPair(
                "JapaneseZonedDateTime",
                javaJapaneseDate.atTime(javaTime).atZone(javaZone),
                kotlinJapaneseDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            AccessorPair(
                "HijrahZonedDateTime",
                javaHijrahDate.atTime(javaTime).atZone(javaZone),
                kotlinHijrahDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            AccessorPair(
                "MinguoZonedDateTime",
                javaMinguoDate.atTime(javaTime).atZone(javaZone),
                kotlinMinguoDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
            AccessorPair(
                "ThaiBuddhistZonedDateTime",
                javaThaiDate.atTime(javaTime).atZone(javaZone),
                kotlinThaiDate.atTime(kotlinTime).atZone(kotlinZone),
            ),
        )
    }

    private fun snapshot(
        accessor: java.time.temporal.TemporalAccessor,
        field: java.time.temporal.ChronoField,
    ): FieldSnapshot = FieldSnapshot(
        supported = outcome { accessor.isSupported(field) },
        range = outcome { accessor.range(field) },
        intValue = outcome { accessor.get(field) },
        longValue = outcome { accessor.getLong(field) },
    )

    private fun snapshot(
        accessor: TemporalAccessor,
        field: ChronoField,
    ): FieldSnapshot = FieldSnapshot(
        supported = outcome { accessor.isSupported(field) },
        range = outcome { accessor.range(field) },
        intValue = outcome { accessor.get(field) },
        longValue = outcome { accessor.getLong(field) },
    )

    private fun outcome(block: () -> Any?): Outcome = runCatching(block).fold(
        onSuccess = { value -> Outcome(value.toString(), null) },
        onFailure = { exception -> Outcome(null, exception.javaClass.simpleName) },
    )

    private data class AccessorPair(
        val name: String,
        val java: java.time.temporal.TemporalAccessor,
        val kotlin: TemporalAccessor,
    )

    private data class FieldSnapshot(
        val supported: Outcome,
        val range: Outcome,
        val intValue: Outcome,
        val longValue: Outcome,
    )

    private data class Outcome(
        val value: String?,
        val exception: String?,
    )
}
