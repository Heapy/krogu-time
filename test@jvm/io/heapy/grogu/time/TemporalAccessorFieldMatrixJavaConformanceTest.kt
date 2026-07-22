package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.ChronoLocalDate
import io.heapy.grogu.time.chrono.ChronoLocalDateTime
import io.heapy.grogu.time.chrono.ChronoZonedDateTime
import io.heapy.grogu.time.chrono.Chronology
import io.heapy.grogu.time.chrono.HijrahChronology
import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.HijrahEra
import io.heapy.grogu.time.chrono.IsoChronology
import io.heapy.grogu.time.chrono.IsoEra
import io.heapy.grogu.time.chrono.JapaneseChronology
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.chrono.JapaneseEra
import io.heapy.grogu.time.chrono.MinguoChronology
import io.heapy.grogu.time.chrono.MinguoDate
import io.heapy.grogu.time.chrono.MinguoEra
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.chrono.ThaiBuddhistEra
import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalQuery
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

    @Test
    fun everyStandardQueryMatchesJavaTimeAcrossAccessors() {
        val queries = listOf(
            QueryPair(
                "chronology",
                java.time.temporal.TemporalQueries.chronology(),
                TemporalQueries.chronology(),
            ),
            QueryPair("zoneId", java.time.temporal.TemporalQueries.zoneId(), TemporalQueries.zoneId()),
            QueryPair("zone", java.time.temporal.TemporalQueries.zone(), TemporalQueries.zone()),
            QueryPair("offset", java.time.temporal.TemporalQueries.offset(), TemporalQueries.offset()),
            QueryPair(
                "localDate",
                java.time.temporal.TemporalQueries.localDate(),
                TemporalQueries.localDate(),
            ),
            QueryPair(
                "localTime",
                java.time.temporal.TemporalQueries.localTime(),
                TemporalQueries.localTime(),
            ),
            QueryPair(
                "precision",
                java.time.temporal.TemporalQueries.precision(),
                TemporalQueries.precision(),
            ),
        )
        val mismatches = accessors().flatMap { accessor ->
            queries.mapNotNull { query ->
                val javaOutcome = outcome { accessor.java.query(query.java) }
                val kotlinOutcome = outcome { accessor.kotlin.query(query.kotlin) }
                if (javaOutcome == kotlinOutcome) {
                    null
                } else {
                    "${accessor.name} ${query.name}: Java=$javaOutcome, Kotlin=$kotlinOutcome"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun everyChronologyFactoryMatchesJavaTimeAcrossAccessors() {
        val mismatches = chronologies().flatMap { chronology ->
            accessors().mapNotNull { accessor ->
                val javaSnapshot = ChronologyFactorySnapshot(
                    date = outcome { chronology.java.date(accessor.java) },
                    localDateTime = outcome { chronology.java.localDateTime(accessor.java) },
                    zonedDateTime = outcome { chronology.java.zonedDateTime(accessor.java) },
                )
                val kotlinSnapshot = ChronologyFactorySnapshot(
                    date = outcome { chronology.kotlin.date(accessor.kotlin) },
                    localDateTime = outcome { chronology.kotlin.localDateTime(accessor.kotlin) },
                    zonedDateTime = outcome { chronology.kotlin.zonedDateTime(accessor.kotlin) },
                )
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${chronology.name} from ${accessor.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun everyChronologyEpochAndInstantZoneFactoryMatchesJavaTime() {
        val epochDays = listOf(
            java.time.LocalDate.MIN.toEpochDay(),
            -1L,
            0L,
            java.time.LocalDate.of(2024, 2, 29).toEpochDay(),
            java.time.LocalDate.MAX.toEpochDay(),
        )
        val instants = listOf(
            InstantPair("MIN", java.time.Instant.MIN, Instant.MIN),
            InstantPair("EPOCH", java.time.Instant.EPOCH, Instant.EPOCH),
            InstantPair(
                "Paris gap boundary",
                java.time.Instant.parse("2024-03-31T01:00:00Z"),
                Instant.parse("2024-03-31T01:00:00Z"),
            ),
            InstantPair("MAX", java.time.Instant.MAX, Instant.MAX),
        )
        val zones = listOf(
            ZonePair("UTC", java.time.ZoneOffset.UTC, ZoneOffset.UTC),
            ZonePair(
                "+05:45",
                java.time.ZoneOffset.ofHoursMinutes(5, 45),
                ZoneOffset.ofHoursMinutes(5, 45),
            ),
            ZonePair(
                "Europe/Paris",
                java.time.ZoneId.of("Europe/Paris"),
                ZoneId.of("Europe/Paris"),
            ),
        )
        val mismatches = chronologies().flatMap { chronology ->
            val epochMismatches = epochDays.mapNotNull { epochDay ->
                val javaOutcome = outcome { chronology.java.dateEpochDay(epochDay) }
                val kotlinOutcome = outcome { chronology.kotlin.dateEpochDay(epochDay) }
                if (javaOutcome == kotlinOutcome) {
                    null
                } else {
                    "${chronology.name} epochDay $epochDay: " +
                        "Java=$javaOutcome, Kotlin=$kotlinOutcome"
                }
            }
            val instantMismatches = instants.flatMap { instant ->
                zones.mapNotNull { zone ->
                    val javaOutcome = outcome {
                        chronology.java.zonedDateTime(instant.java, zone.java)
                    }
                    val kotlinOutcome = outcome {
                        chronology.kotlin.zonedDateTime(instant.kotlin, zone.kotlin)
                    }
                    if (javaOutcome == kotlinOutcome) {
                        null
                    } else {
                        "${chronology.name} ${instant.name} ${zone.name}: " +
                            "Java=$javaOutcome, Kotlin=$kotlinOutcome"
                    }
                }
            }
            epochMismatches + instantMismatches
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun everyTemporalAccessorConversionFactoryMatchesJavaTime() {
        val factories = listOf(
            AccessorFactoryPair("DayOfWeek", java.time.DayOfWeek::from, DayOfWeek::from),
            AccessorFactoryPair("Month", java.time.Month::from, Month::from),
            AccessorFactoryPair("Year", java.time.Year::from, Year::from),
            AccessorFactoryPair("MonthDay", java.time.MonthDay::from, MonthDay::from),
            AccessorFactoryPair("YearMonth", java.time.YearMonth::from, YearMonth::from),
            AccessorFactoryPair("LocalDate", java.time.LocalDate::from, LocalDate::from),
            AccessorFactoryPair("LocalTime", java.time.LocalTime::from, LocalTime::from),
            AccessorFactoryPair(
                "LocalDateTime",
                java.time.LocalDateTime::from,
                LocalDateTime::from,
            ),
            AccessorFactoryPair("Instant", java.time.Instant::from, Instant::from),
            AccessorFactoryPair("ZoneOffset", java.time.ZoneOffset::from, ZoneOffset::from),
            AccessorFactoryPair("ZoneId", java.time.ZoneId::from, ZoneId::from),
            AccessorFactoryPair("OffsetTime", java.time.OffsetTime::from, OffsetTime::from),
            AccessorFactoryPair(
                "OffsetDateTime",
                java.time.OffsetDateTime::from,
                OffsetDateTime::from,
            ),
            AccessorFactoryPair(
                "ZonedDateTime",
                java.time.ZonedDateTime::from,
                ZonedDateTime::from,
            ),
            AccessorFactoryPair(
                "Chronology",
                java.time.chrono.Chronology::from,
                Chronology::from,
            ),
            AccessorFactoryPair(
                "ChronoLocalDate",
                java.time.chrono.ChronoLocalDate::from,
                ChronoLocalDate::from,
            ),
            AccessorFactoryPair(
                "ChronoLocalDateTime",
                java.time.chrono.ChronoLocalDateTime<*>::from,
                { temporal -> ChronoLocalDateTime.from(temporal) },
            ),
            AccessorFactoryPair(
                "ChronoZonedDateTime",
                java.time.chrono.ChronoZonedDateTime<*>::from,
                { temporal -> ChronoZonedDateTime.from(temporal) },
            ),
            AccessorFactoryPair(
                "JapaneseDate",
                java.time.chrono.JapaneseDate::from,
                JapaneseDate::from,
            ),
            AccessorFactoryPair(
                "HijrahDate",
                java.time.chrono.HijrahDate::from,
                HijrahDate::from,
            ),
            AccessorFactoryPair(
                "MinguoDate",
                java.time.chrono.MinguoDate::from,
                MinguoDate::from,
            ),
            AccessorFactoryPair(
                "ThaiBuddhistDate",
                java.time.chrono.ThaiBuddhistDate::from,
                ThaiBuddhistDate::from,
            ),
        )
        val mismatches = factories.flatMap { factory ->
            accessors().mapNotNull { accessor ->
                val javaOutcome = outcome { factory.java(accessor.java) }
                val kotlinOutcome = outcome { factory.kotlin(accessor.kotlin) }
                if (javaOutcome == kotlinOutcome) {
                    null
                } else {
                    "${factory.name} from ${accessor.name}: " +
                        "Java=$javaOutcome, Kotlin=$kotlinOutcome"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun chronologies(): List<ChronologyPair> = listOf(
        ChronologyPair(
            "ISO",
            java.time.chrono.IsoChronology.INSTANCE,
            IsoChronology,
        ),
        ChronologyPair(
            "Japanese",
            java.time.chrono.JapaneseChronology.INSTANCE,
            JapaneseChronology,
        ),
        ChronologyPair(
            "Hijrah-umalqura",
            java.time.chrono.HijrahChronology.INSTANCE,
            HijrahChronology,
        ),
        ChronologyPair(
            "Minguo",
            java.time.chrono.MinguoChronology.INSTANCE,
            MinguoChronology,
        ),
        ChronologyPair(
            "ThaiBuddhist",
            java.time.chrono.ThaiBuddhistChronology.INSTANCE,
            ThaiBuddhistChronology,
        ),
    )

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

    private data class QueryPair(
        val name: String,
        val java: java.time.temporal.TemporalQuery<*>,
        val kotlin: TemporalQuery<*>,
    )

    private data class ChronologyPair(
        val name: String,
        val java: java.time.chrono.Chronology,
        val kotlin: Chronology,
    )

    private data class InstantPair(
        val name: String,
        val java: java.time.Instant,
        val kotlin: Instant,
    )

    private data class ZonePair(
        val name: String,
        val java: java.time.ZoneId,
        val kotlin: ZoneId,
    )

    private data class AccessorFactoryPair(
        val name: String,
        val java: (java.time.temporal.TemporalAccessor) -> Any?,
        val kotlin: (TemporalAccessor) -> Any?,
    )

    private data class ChronologyFactorySnapshot(
        val date: Outcome,
        val localDateTime: Outcome,
        val zonedDateTime: Outcome,
    )

    private data class Outcome(
        val value: String?,
        val exception: String?,
    )
}
