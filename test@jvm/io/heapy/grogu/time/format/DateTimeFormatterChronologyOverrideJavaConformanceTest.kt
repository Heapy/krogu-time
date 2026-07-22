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

    private data class ChronologyCase(
        val javaChronology: java.time.chrono.Chronology,
        val groguChronology: io.heapy.grogu.time.chrono.Chronology,
        val text: String,
    )
}
