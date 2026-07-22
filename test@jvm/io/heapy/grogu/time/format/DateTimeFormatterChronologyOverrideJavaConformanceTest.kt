package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.chrono.MinguoChronology
import io.heapy.grogu.time.chrono.ThaiBuddhistChronology
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
            val groguFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(case.groguChronology)

            assertEquals(
                javaFormatter.format(java.time.LocalDate.of(2024, 3, 1)),
                groguFormatter.format(LocalDate.of(2024, 3, 1)),
                case.text,
            )
            assertEquals(
                java.time.LocalDate.from(javaFormatter.parse(case.text)).toString(),
                LocalDate.from(groguFormatter.parse(case.text)).toString(),
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
            val groguFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                .withChronology(io.heapy.grogu.time.chrono.HijrahChronology)
                .withResolverStyle(style)

            val javaResult = runCatching {
                java.time.LocalDate.from(javaFormatter.parse("1445-08-30")).toString()
            }
            val groguResult = runCatching {
                LocalDate.from(groguFormatter.parse("1445-08-30")).toString()
            }
            assertEquals(javaResult.isSuccess, groguResult.isSuccess, style.name)
            if (javaResult.isSuccess) {
                assertEquals(javaResult.getOrThrow(), groguResult.getOrThrow(), style.name)
            }
        }
    }

    private data class ChronologyCase(
        val javaChronology: java.time.chrono.Chronology,
        val groguChronology: io.heapy.grogu.time.chrono.Chronology,
        val text: String,
    )
}
