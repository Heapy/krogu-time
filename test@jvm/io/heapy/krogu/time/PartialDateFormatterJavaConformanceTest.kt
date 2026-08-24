package io.heapy.krogu.time

import kotlin.test.Test
import kotlin.test.assertEquals

class PartialDateFormatterJavaConformanceTest {
    @Test
    fun formatterOverloadsMatchJavaTime() {
        val cases = listOf(
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("'year='uuuu"),
                kroguFormatter = io.heapy.krogu.time.format.DateTimeFormatter.ofPattern("'year='uuuu"),
                javaValue = java.time.Year.of(-42),
                kroguValue = Year.of(-42),
                text = "year=-0042",
                parseJava = { text, formatter -> java.time.Year.parse(text, formatter).toString() },
                parseKrogu = { text, formatter -> Year.parse(text, formatter).toString() },
            ),
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/uuuu"),
                kroguFormatter = io.heapy.krogu.time.format.DateTimeFormatter.ofPattern("MM/uuuu"),
                javaValue = java.time.YearMonth.of(2024, 3),
                kroguValue = YearMonth.of(2024, 3),
                text = "03/2024",
                parseJava = { text, formatter -> java.time.YearMonth.parse(text, formatter).toString() },
                parseKrogu = { text, formatter -> YearMonth.parse(text, formatter).toString() },
            ),
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM"),
                kroguFormatter = io.heapy.krogu.time.format.DateTimeFormatter.ofPattern("dd.MM"),
                javaValue = java.time.MonthDay.of(2, 29),
                kroguValue = MonthDay.of(2, 29),
                text = "29.02",
                parseJava = { text, formatter -> java.time.MonthDay.parse(text, formatter).toString() },
                parseKrogu = { text, formatter -> MonthDay.parse(text, formatter).toString() },
            ),
        )

        cases.forEach { case ->
            assertEquals(case.javaFormatter.format(case.javaValue), case.kroguFormatter.format(case.kroguValue))
            assertEquals(
                case.parseJava(case.text, case.javaFormatter),
                case.parseKrogu(case.text, case.kroguFormatter),
            )
        }
    }

    @Test
    fun formatterFailuresMatchJavaTime() {
        val javaDate = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val kroguDate = io.heapy.krogu.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        assertEquals(
            runCatching { java.time.Year.of(2024).format(javaDate) }.isSuccess,
            runCatching { Year.of(2024).format(kroguDate) }.isSuccess,
        )

        assertEquals(
            runCatching {
                java.time.MonthDay.parse(
                    "02",
                    java.time.format.DateTimeFormatter.ofPattern("MM"),
                )
            }.isSuccess,
            runCatching {
                MonthDay.parse(
                    "02",
                    io.heapy.krogu.time.format.DateTimeFormatter.ofPattern("MM"),
                )
            }.isSuccess,
        )
    }

    private data class FormatterCase(
        val javaFormatter: java.time.format.DateTimeFormatter,
        val kroguFormatter: io.heapy.krogu.time.format.DateTimeFormatter,
        val javaValue: java.time.temporal.TemporalAccessor,
        val kroguValue: io.heapy.krogu.time.temporal.TemporalAccessor,
        val text: String,
        val parseJava: (String, java.time.format.DateTimeFormatter) -> String,
        val parseKrogu: (String, io.heapy.krogu.time.format.DateTimeFormatter) -> String,
    )
}
