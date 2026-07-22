package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.ZoneOffset
import java.time.chrono.Chronology as JavaChronology
import java.time.chrono.Era as JavaEra
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronologyEpochSecondJavaConformanceTest {
    @Test
    fun bothEpochSecondOverloadsMatchJavaTimeForEveryBuiltInChronology() {
        val cases = listOf(
            Fields(
                java.time.chrono.IsoChronology.INSTANCE,
                IsoChronology,
                java.time.chrono.IsoEra.CE,
                IsoEra.CE,
                2_024,
                2_024,
                2,
                29,
            ),
            Fields(
                java.time.chrono.JapaneseChronology.INSTANCE,
                JapaneseChronology,
                java.time.chrono.JapaneseEra.REIWA,
                JapaneseEra.REIWA,
                2_024,
                6,
                2,
                29,
            ),
            Fields(
                java.time.chrono.MinguoChronology.INSTANCE,
                MinguoChronology,
                java.time.chrono.MinguoEra.ROC,
                MinguoEra.ROC,
                113,
                113,
                2,
                29,
            ),
            Fields(
                java.time.chrono.ThaiBuddhistChronology.INSTANCE,
                ThaiBuddhistChronology,
                java.time.chrono.ThaiBuddhistEra.BE,
                ThaiBuddhistEra.BE,
                2_567,
                2_567,
                2,
                29,
            ),
            Fields(
                java.time.chrono.HijrahChronology.INSTANCE,
                HijrahChronology,
                java.time.chrono.HijrahEra.AH,
                HijrahEra.AH,
                1_445,
                1_445,
                8,
                19,
            ),
        )
        val javaOffset = java.time.ZoneOffset.ofHoursMinutes(-3, -30)
        val offset = ZoneOffset.ofHoursMinutes(-3, -30)

        cases.forEach { fields ->
            assertEquals(
                fields.javaChronology.epochSecond(
                    fields.prolepticYear,
                    fields.month,
                    fields.day,
                    23,
                    59,
                    58,
                    javaOffset,
                ),
                fields.chronology.epochSecond(
                    fields.prolepticYear,
                    fields.month,
                    fields.day,
                    23,
                    59,
                    58,
                    offset,
                ),
                fields.chronology.id,
            )
            assertEquals(
                fields.javaChronology.epochSecond(
                    fields.javaEra,
                    fields.yearOfEra,
                    fields.month,
                    fields.day,
                    23,
                    59,
                    58,
                    javaOffset,
                ),
                fields.chronology.epochSecond(
                    fields.era,
                    fields.yearOfEra,
                    fields.month,
                    fields.day,
                    23,
                    59,
                    58,
                    offset,
                ),
                "${fields.chronology.id} era",
            )
        }
    }

    private data class Fields(
        val javaChronology: JavaChronology,
        val chronology: Chronology,
        val javaEra: JavaEra,
        val era: Era,
        val prolepticYear: Int,
        val yearOfEra: Int,
        val month: Int,
        val day: Int,
    )
}
