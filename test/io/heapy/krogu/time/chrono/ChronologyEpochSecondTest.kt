package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChronologyEpochSecondTest {
    @Test
    fun convertsDateTimeFieldsFromEveryBuiltInChronology() {
        val offset = ZoneOffset.ofHours(2)
        val expected = LocalDateTime.of(2024, 2, 29, 12, 30, 45).toEpochSecond(offset)
        val cases = listOf(
            Fields(IsoChronology, IsoEra.CE, 2_024, 2_024, 2, 29),
            Fields(JapaneseChronology, JapaneseEra.REIWA, 2_024, 6, 2, 29),
            Fields(MinguoChronology, MinguoEra.ROC, 113, 113, 2, 29),
            Fields(ThaiBuddhistChronology, ThaiBuddhistEra.BE, 2_567, 2_567, 2, 29),
            Fields(HijrahChronology, HijrahEra.AH, 1_445, 1_445, 8, 19),
        )

        cases.forEach { fields ->
            assertEquals(
                expected,
                fields.chronology.epochSecond(
                    fields.prolepticYear,
                    fields.month,
                    fields.day,
                    12,
                    30,
                    45,
                    offset,
                ),
                fields.chronology.id,
            )
            assertEquals(
                expected,
                fields.chronology.epochSecond(
                    fields.era,
                    fields.yearOfEra,
                    fields.month,
                    fields.day,
                    12,
                    30,
                    45,
                    offset,
                ),
                "${fields.chronology.id} era",
            )
        }
    }

    @Test
    fun validatesTimeFieldsBeforeConvertingTheDate() {
        val chronology: Chronology = HijrahChronology
        assertFailsWith<DateTimeException> {
            chronology.epochSecond(1_445, 8, 19, 24, 0, 0, ZoneOffset.UTC)
        }
        assertFailsWith<DateTimeException> {
            chronology.epochSecond(1_445, 8, 19, 0, 60, 0, ZoneOffset.UTC)
        }
        assertFailsWith<DateTimeException> {
            chronology.epochSecond(1_445, 8, 19, 0, 0, 60, ZoneOffset.UTC)
        }
    }

    private data class Fields(
        val chronology: Chronology,
        val era: Era,
        val prolepticYear: Int,
        val yearOfEra: Int,
        val month: Int,
        val day: Int,
    )
}
