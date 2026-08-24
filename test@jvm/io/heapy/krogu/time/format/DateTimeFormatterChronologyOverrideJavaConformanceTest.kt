package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.chrono.MinguoChronology
import io.heapy.krogu.time.chrono.ThaiBuddhistChronology
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterChronologyOverrideJavaConformanceTest {
    @Test
    fun chronologyOverrideFormattingAndParsingMatchesJavaTime() {
        val cases = listOf(
            ChronologyCase(
                java.time.chrono.ThaiBuddhistChronology.INSTANCE,
                ThaiBuddhistChronology,
                "2567-03-01",
            ),
            ChronologyCase(
                java.time.chrono.MinguoChronology.INSTANCE,
                MinguoChronology,
                "0113-03-01",
            ),
        )

        cases.forEach { case ->
            val javaFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(case.javaChronology)
            val kroguFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(case.kroguChronology)

            assertEquals(
                javaFormatter.format(java.time.LocalDate.of(2024, 3, 1)),
                kroguFormatter.format(LocalDate.of(2024, 3, 1)),
                case.text,
            )
            assertEquals(
                java.time.LocalDate.from(javaFormatter.parse(case.text)).toString(),
                LocalDate.from(kroguFormatter.parse(case.text)).toString(),
                case.text,
            )
        }
    }

    @Test
    fun nonIsoResolutionStylesMatchJavaTime() {
        ResolverStyle.entries.forEach { style ->
            val javaFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(java.time.chrono.HijrahChronology.INSTANCE)
                .withResolverStyle(java.time.format.ResolverStyle.valueOf(style.name))
            val kroguFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(io.heapy.krogu.time.chrono.HijrahChronology)
                .withResolverStyle(style)

            val javaResult = runCatching {
                java.time.LocalDate.from(javaFormatter.parse("1445-08-30")).toString()
            }
            val kroguResult = runCatching {
                LocalDate.from(kroguFormatter.parse("1445-08-30")).toString()
            }
            assertEquals(javaResult.isSuccess, kroguResult.isSuccess, style.name)
            if (javaResult.isSuccess) {
                assertEquals(javaResult.getOrThrow(), kroguResult.getOrThrow(), style.name)
            }
        }
    }

    private data class ChronologyCase(
        val javaChronology: java.time.chrono.Chronology,
        val kroguChronology: io.heapy.krogu.time.chrono.Chronology,
        val text: String,
    )
}
