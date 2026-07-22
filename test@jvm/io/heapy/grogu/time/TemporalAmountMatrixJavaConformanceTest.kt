package io.heapy.grogu.time

import io.heapy.grogu.time.chrono.ChronoPeriod
import io.heapy.grogu.time.chrono.HijrahChronology
import io.heapy.grogu.time.chrono.HijrahDate
import io.heapy.grogu.time.chrono.JapaneseChronology
import io.heapy.grogu.time.chrono.JapaneseDate
import io.heapy.grogu.time.chrono.MinguoChronology
import io.heapy.grogu.time.chrono.MinguoDate
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistDate
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAmount
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalAmountMatrixJavaConformanceTest {
    @Test
    fun temporalAmountCombinationsMatchJavaTime() {
        val mismatches = temporals().flatMap { temporal ->
            amounts().mapNotNull { amount ->
                val javaSnapshot = snapshot(temporal.java, amount.java)
                val kotlinSnapshot = snapshot(temporal.kotlin, amount.kotlin)
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${temporal.name} with ${amount.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun temporalAmountConversionsMatchJavaTime() {
        val mismatches = amounts().mapNotNull { amount ->
            val javaSnapshot = ConversionSnapshot(
                duration = outcome { java.time.Duration.from(amount.java) },
                period = outcome { java.time.Period.from(amount.java) },
            )
            val kotlinSnapshot = ConversionSnapshot(
                duration = outcome { Duration.from(amount.kotlin) },
                period = outcome { Period.from(amount.kotlin) },
            )
            if (javaSnapshot == kotlinSnapshot) {
                null
            } else {
                "${amount.name}: Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun chronoPeriodArithmeticCombinationsMatchJavaTime() {
        val leftOperands = amounts().mapNotNull { amount ->
            val javaPeriod = amount.java as? java.time.chrono.ChronoPeriod
                ?: return@mapNotNull null
            val kotlinPeriod = amount.kotlin as? ChronoPeriod
                ?: return@mapNotNull null
            ChronoPeriodPair(amount.name, javaPeriod, kotlinPeriod)
        }
        val mismatches = leftOperands.flatMap { left ->
            amounts().mapNotNull { right ->
                val javaSnapshot = ChronoPeriodSnapshot(
                    plus = outcome { left.java.plus(right.java) },
                    minus = outcome { left.java.minus(right.java) },
                )
                val kotlinSnapshot = ChronoPeriodSnapshot(
                    plus = outcome { left.kotlin.plus(right.kotlin) },
                    minus = outcome { left.kotlin.minus(right.kotlin) },
                )
                if (javaSnapshot == kotlinSnapshot) {
                    null
                } else {
                    "${left.name} with ${right.name}: " +
                        "Java=$javaSnapshot, Kotlin=$kotlinSnapshot"
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

    private fun amounts(): List<AmountPair> = listOf(
        AmountPair("Duration.ZERO", java.time.Duration.ZERO, Duration.ZERO),
        AmountPair(
            "negative Duration",
            java.time.Duration.ofSeconds(-2, 500_000_000),
            Duration.ofSeconds(-2, 500_000_000),
        ),
        AmountPair(
            "positive Duration",
            java.time.Duration.ofSeconds(3_661, 123_456_789),
            Duration.ofSeconds(3_661, 123_456_789),
        ),
        AmountPair("Period.ZERO", java.time.Period.ZERO, Period.ZERO),
        AmountPair("year Period", java.time.Period.ofYears(1), Period.ofYears(1)),
        AmountPair("month Period", java.time.Period.ofMonths(1), Period.ofMonths(1)),
        AmountPair("day Period", java.time.Period.ofDays(1), Period.ofDays(1)),
        AmountPair("mixed Period", java.time.Period.of(1, 2, 3), Period.of(1, 2, 3)),
        AmountPair(
            "JapanesePeriod",
            java.time.chrono.JapaneseChronology.INSTANCE.period(1, 2, 3),
            JapaneseChronology.period(1, 2, 3),
        ),
        AmountPair(
            "HijrahPeriod",
            java.time.chrono.HijrahChronology.INSTANCE.period(1, 2, 3),
            HijrahChronology.period(1, 2, 3),
        ),
        AmountPair(
            "MinguoPeriod",
            java.time.chrono.MinguoChronology.INSTANCE.period(1, 2, 3),
            MinguoChronology.period(1, 2, 3),
        ),
        AmountPair(
            "ThaiBuddhistPeriod",
            java.time.chrono.ThaiBuddhistChronology.INSTANCE.period(1, 2, 3),
            ThaiBuddhistChronology.period(1, 2, 3),
        ),
    )

    private fun snapshot(
        temporal: java.time.temporal.Temporal,
        amount: java.time.temporal.TemporalAmount,
    ): AmountSnapshot = AmountSnapshot(
        plus = outcome { temporal.plus(amount) },
        minus = outcome { temporal.minus(amount) },
        addTo = outcome { amount.addTo(temporal) },
        subtractFrom = outcome { amount.subtractFrom(temporal) },
    )

    private fun snapshot(
        temporal: Temporal,
        amount: TemporalAmount,
    ): AmountSnapshot = AmountSnapshot(
        plus = outcome { temporal.plus(amount) },
        minus = outcome { temporal.minus(amount) },
        addTo = outcome { amount.addTo(temporal) },
        subtractFrom = outcome { amount.subtractFrom(temporal) },
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

    private data class AmountPair(
        val name: String,
        val java: java.time.temporal.TemporalAmount,
        val kotlin: TemporalAmount,
    )

    private data class ChronoPeriodPair(
        val name: String,
        val java: java.time.chrono.ChronoPeriod,
        val kotlin: ChronoPeriod,
    )

    private data class AmountSnapshot(
        val plus: Outcome,
        val minus: Outcome,
        val addTo: Outcome,
        val subtractFrom: Outcome,
    )

    private data class ConversionSnapshot(
        val duration: Outcome,
        val period: Outcome,
    )

    private data class ChronoPeriodSnapshot(
        val plus: Outcome,
        val minus: Outcome,
    )

    private data class Outcome(
        val value: String?,
        val exception: String?,
    )
}
