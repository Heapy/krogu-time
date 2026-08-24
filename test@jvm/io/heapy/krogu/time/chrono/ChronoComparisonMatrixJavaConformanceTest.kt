package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Instant
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoComparisonMatrixJavaConformanceTest {
    @Test
    fun chronoLocalDateComparisonsMatchJavaTime() {
        val values = dates()
        val mismatches = values.flatMap { first ->
            values.mapNotNull { second ->
                val javaSnapshot = comparison(
                    compareTo = first.java.compareTo(second.java),
                    timeline = java.time.chrono.ChronoLocalDate.timeLineOrder()
                        .compare(first.java, second.java),
                    isBefore = first.java.isBefore(second.java),
                    isAfter = first.java.isAfter(second.java),
                    isEqual = first.java.isEqual(second.java),
                    equals = first.java == second.java,
                )
                val kotlinSnapshot = comparison(
                    compareTo = first.kotlin.compareTo(second.kotlin),
                    timeline = ChronoLocalDate.timeLineOrder()
                        .compare(first.kotlin, second.kotlin),
                    isBefore = first.kotlin.isBefore(second.kotlin),
                    isAfter = first.kotlin.isAfter(second.kotlin),
                    isEqual = first.kotlin.isEqual(second.kotlin),
                    equals = first.kotlin == second.kotlin,
                )
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${first.name} vs ${second.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun chronoLocalDateTimeComparisonsMatchJavaTime() {
        val values = dateTimes()
        val mismatches = values.flatMap { first ->
            values.mapNotNull { second ->
                val javaSnapshot = comparison(
                    compareTo = first.java.compareTo(second.java),
                    timeline = java.time.chrono.ChronoLocalDateTime.timeLineOrder()
                        .compare(first.java, second.java),
                    isBefore = first.java.isBefore(second.java),
                    isAfter = first.java.isAfter(second.java),
                    isEqual = first.java.isEqual(second.java),
                    equals = first.java == second.java,
                )
                val kotlinSnapshot = comparison(
                    compareTo = first.kotlin.compareTo(second.kotlin),
                    timeline = ChronoLocalDateTime.timeLineOrder()
                        .compare(first.kotlin, second.kotlin),
                    isBefore = first.kotlin.isBefore(second.kotlin),
                    isAfter = first.kotlin.isAfter(second.kotlin),
                    isEqual = first.kotlin.isEqual(second.kotlin),
                    equals = first.kotlin == second.kotlin,
                )
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${first.name} vs ${second.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun chronoZonedDateTimeComparisonsMatchJavaTime() {
        val values = zonedDateTimes()
        val mismatches = values.flatMap { first ->
            values.mapNotNull { second ->
                val javaSnapshot = comparison(
                    compareTo = first.java.compareTo(second.java),
                    timeline = java.time.chrono.ChronoZonedDateTime.timeLineOrder()
                        .compare(first.java, second.java),
                    isBefore = first.java.isBefore(second.java),
                    isAfter = first.java.isAfter(second.java),
                    isEqual = first.java.isEqual(second.java),
                    equals = first.java == second.java,
                )
                val kotlinSnapshot = comparison(
                    compareTo = first.kotlin.compareTo(second.kotlin),
                    timeline = ChronoZonedDateTime.timeLineOrder()
                        .compare(first.kotlin, second.kotlin),
                    isBefore = first.kotlin.isBefore(second.kotlin),
                    isAfter = first.kotlin.isAfter(second.kotlin),
                    isEqual = first.kotlin.isEqual(second.kotlin),
                    equals = first.kotlin == second.kotlin,
                )
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${first.name} vs ${second.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun dates(): List<DatePair> {
        val epochDays = listOf(
            LocalDate.of(2024, 3, 30).toEpochDay(),
            LocalDate.of(2024, 3, 31).toEpochDay(),
        )
        return chronologies().flatMap { chronology ->
            epochDays.mapIndexed { index, epochDay ->
                DatePair(
                    "${chronology.name}+$index",
                    chronology.java.dateEpochDay(epochDay),
                    chronology.kotlin.dateEpochDay(epochDay),
                )
            }
        }
    }

    private fun dateTimes(): List<DateTimePair> {
        val times = listOf(
            JavaKotlinTime(java.time.LocalTime.MIDNIGHT, LocalTime.MIDNIGHT),
            JavaKotlinTime(
                java.time.LocalTime.of(23, 59, 59, 999_999_999),
                LocalTime.of(23, 59, 59, 999_999_999),
            ),
        )
        val epochDay = LocalDate.of(2024, 3, 30).toEpochDay()
        return chronologies().flatMap { chronology ->
            times.mapIndexed { index, time ->
                DateTimePair(
                    "${chronology.name}@$index",
                    chronology.java.dateEpochDay(epochDay).atTime(time.java),
                    chronology.kotlin.dateEpochDay(epochDay).atTime(time.kotlin),
                )
            }
        }
    }

    private fun zonedDateTimes(): List<ZonedDateTimePair> {
        val instant = JavaKotlinInstant(
            java.time.Instant.parse("2024-03-31T01:00:00.123456789Z"),
            Instant.parse("2024-03-31T01:00:00.123456789Z"),
        )
        val zones = listOf(
            JavaKotlinZone(java.time.ZoneOffset.UTC, ZoneOffset.UTC),
            JavaKotlinZone(
                java.time.ZoneOffset.ofHoursMinutes(5, 45),
                ZoneOffset.ofHoursMinutes(5, 45),
            ),
            JavaKotlinZone(
                java.time.ZoneId.of("Europe/Paris"),
                ZoneId.of("Europe/Paris"),
            ),
        )
        return chronologies().flatMap { chronology ->
            zones.mapIndexed { index, zone ->
                ZonedDateTimePair(
                    "${chronology.name}@$index",
                    chronology.java.zonedDateTime(instant.java, zone.java),
                    chronology.kotlin.zonedDateTime(instant.kotlin, zone.kotlin),
                )
            }
        }
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
            "Hijrah",
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

    private fun comparison(
        compareTo: Int,
        timeline: Int,
        isBefore: Boolean,
        isAfter: Boolean,
        isEqual: Boolean,
        equals: Boolean,
    ): ComparisonSnapshot = ComparisonSnapshot(
        compareTo = compareTo.sign,
        timeline = timeline.sign,
        isBefore = isBefore,
        isAfter = isAfter,
        isEqual = isEqual,
        equals = equals,
    )

    private val Int.sign: Int
        get() = compareTo(0)

    private data class ChronologyPair(
        val name: String,
        val java: java.time.chrono.Chronology,
        val kotlin: Chronology,
    )

    private data class DatePair(
        val name: String,
        val java: java.time.chrono.ChronoLocalDate,
        val kotlin: ChronoLocalDate,
    )

    private data class DateTimePair(
        val name: String,
        val java: java.time.chrono.ChronoLocalDateTime<*>,
        val kotlin: ChronoLocalDateTime<*>,
    )

    private data class ZonedDateTimePair(
        val name: String,
        val java: java.time.chrono.ChronoZonedDateTime<*>,
        val kotlin: ChronoZonedDateTime<*>,
    )

    private data class JavaKotlinTime(
        val java: java.time.LocalTime,
        val kotlin: LocalTime,
    )

    private data class JavaKotlinInstant(
        val java: java.time.Instant,
        val kotlin: Instant,
    )

    private data class JavaKotlinZone(
        val java: java.time.ZoneId,
        val kotlin: ZoneId,
    )

    private data class ComparisonSnapshot(
        val compareTo: Int,
        val timeline: Int,
        val isBefore: Boolean,
        val isAfter: Boolean,
        val isEqual: Boolean,
        val equals: Boolean,
    )
}
