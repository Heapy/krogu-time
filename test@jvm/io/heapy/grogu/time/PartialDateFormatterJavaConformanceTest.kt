package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals

class PartialDateFormatterJavaConformanceTest {
    @Test
    fun formatterOverloadsMatchJavaTime() {
        val cases = listOf(
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("'year='uuuu"),
                groguFormatter = io.heapy.grogu.time.format.DateTimeFormatter.ofPattern("'year='uuuu"),
                javaValue = java.time.Year.of(-42),
                groguValue = Year.of(-42),
                text = "year=-0042",
                parseJava = { text, formatter -> java.time.Year.parse(text, formatter).toString() },
                parseGrogu = { text, formatter -> Year.parse(text, formatter).toString() },
            ),
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/uuuu"),
                groguFormatter = io.heapy.grogu.time.format.DateTimeFormatter.ofPattern("MM/uuuu"),
                javaValue = java.time.YearMonth.of(2024, 3),
                groguValue = YearMonth.of(2024, 3),
                text = "03/2024",
                parseJava = { text, formatter -> java.time.YearMonth.parse(text, formatter).toString() },
                parseGrogu = { text, formatter -> YearMonth.parse(text, formatter).toString() },
            ),
            FormatterCase(
                javaFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM"),
                groguFormatter = io.heapy.grogu.time.format.DateTimeFormatter.ofPattern("dd.MM"),
                javaValue = java.time.MonthDay.of(2, 29),
                groguValue = MonthDay.of(2, 29),
                text = "29.02",
                parseJava = { text, formatter -> java.time.MonthDay.parse(text, formatter).toString() },
                parseGrogu = { text, formatter -> MonthDay.parse(text, formatter).toString() },
            ),
        )

        cases.forEach { case ->
            assertEquals(case.javaFormatter.format(case.javaValue), case.groguFormatter.format(case.groguValue))
            assertEquals(
                case.parseJava(case.text, case.javaFormatter),
                case.parseGrogu(case.text, case.groguFormatter),
            )
        }
    }

    @Test
    fun formatterFailuresMatchJavaTime() {
        val javaDate = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val groguDate = io.heapy.grogu.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd")
        assertEquals(
            runCatching { java.time.Year.of(2024).format(javaDate) }.isSuccess,
            runCatching { Year.of(2024).format(groguDate) }.isSuccess,
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
                    io.heapy.grogu.time.format.DateTimeFormatter.ofPattern("MM"),
                )
            }.isSuccess,
        )
    }

    private data class FormatterCase(
        val javaFormatter: java.time.format.DateTimeFormatter,
        val groguFormatter: io.heapy.grogu.time.format.DateTimeFormatter,
        val javaValue: java.time.temporal.TemporalAccessor,
        val groguValue: io.heapy.grogu.time.temporal.TemporalAccessor,
        val text: String,
        val parseJava: (String, java.time.format.DateTimeFormatter) -> String,
        val parseGrogu: (String, io.heapy.grogu.time.format.DateTimeFormatter) -> String,
    )
}
