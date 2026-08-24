package io.heapy.krogu.time

import java.time.LocalDate as JavaLocalDate
import java.time.Instant as JavaInstant
import java.time.LocalTime as JavaLocalTime
import java.time.Period as JavaPeriod
import java.time.ZoneId as JavaZoneId
import java.time.ZoneOffset as JavaZoneOffset
import java.time.format.DateTimeParseException as JavaDateTimeParseException
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateJavaConformanceTest {
    @Test
    fun valueAndEpochBehaviorMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -1_000_000L,
            -719_528L,
            -1L,
            0L,
            1L,
            11_016L,
            19_782L,
            1_000_000L,
            365_241_780_471L,
        )

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)
            assertEquals(javaDate.year, date.year)
            assertEquals(javaDate.monthValue, date.monthValue)
            assertEquals(javaDate.dayOfMonth, date.dayOfMonth)
            assertEquals(javaDate.dayOfYear, date.dayOfYear)
            assertEquals(javaDate.dayOfWeek.value, date.dayOfWeek.value)
            assertEquals(javaDate.isLeapYear, date.isLeapYear)
            assertEquals(javaDate.lengthOfMonth(), date.lengthOfMonth())
            assertEquals(javaDate.lengthOfYear(), date.lengthOfYear())
            assertEquals(javaDate.toEpochDay(), date.toEpochDay())
            assertEquals(javaDate.toString(), date.toString())
        }
    }

    @Test
    fun defaultIsoParsingMatchesJavaTime() {
        val inputs = listOf(
            "",
            "0000-01-01",
            "2024-02-29",
            "-0001-01-01",
            "+10000-01-01",
            "-999999999-01-01",
            "+999999999-12-31",
            "2024-2-29",
            "2024-02-9",
            "2024/02/29",
            "2024-02/29",
            "2023-02-29",
            "2024-13-01",
            "2024-02-30",
            "999-01-01",
            "+2024-01-01",
            "10000-01-01",
            "+00000-01-01",
            "-0000-01-01",
            "+0000-01-01",
            "00000-01-01",
            "+1000000000-01-01",
            "-1000000000-01-01",
            "+5294967295-01-01",
            "-3294967297-01-01",
            "+12345678901-01-01",
            "-12345678901-01-01",
            "202A-01-01",
            "2024--01-01",
            "2024-001-01",
            "2024-0101",
            "2024-01-001",
            "2024-01-01-",
            "2024-02-29Z",
            " 2024-02-29",
            "2024-02-29 ",
            "２０２４-０２-２９",
        )

        inputs.forEach { input ->
            val javaResult = runCatching { JavaLocalDate.parse(input).toString() }
            val kotlinResult = runCatching { LocalDate.parse(input).toString() }
            assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), input)
            assertEquals(
                javaResult.exceptionOrNull()?.javaClass?.simpleName,
                kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
                input,
            )
            val javaErrorIndex = (javaResult.exceptionOrNull() as? JavaDateTimeParseException)
                ?.errorIndex
            val kotlinErrorIndex = (kotlinResult.exceptionOrNull()
                as? io.heapy.krogu.time.format.DateTimeParseException)?.errorIndex
            assertEquals(javaErrorIndex, kotlinErrorIndex, input)
        }
    }

    @Test
    fun temporalFieldBehaviorMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -719_893L,
            -1L,
            0L,
            11_016L,
            19_782L,
            365_241_780_471L,
        )

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val context = "date=$date field=$field"
                assertEquals(javaDate.isSupported(javaField), date.isSupported(field), context)
                assertSameOutcome(
                    javaOperation = { javaDate.range(javaField).toString() },
                    kotlinOperation = { date.range(field).toString() },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaDate.get(javaField) },
                    kotlinOperation = { date.get(field) },
                    context = context,
                )
                assertSameOutcome(
                    javaOperation = { javaDate.getLong(javaField) },
                    kotlinOperation = { date.getLong(field) },
                    context = context,
                )
            }
            assertEquals(javaDate.era.value, date.era.value)
        }
    }

    @Test
    fun fieldReplacementAndArithmeticMatchJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -719_893L,
            -1L,
            0L,
            11_016L,
            19_782L,
            365_241_780_471L,
        )
        val amounts = listOf(Long.MIN_VALUE, -1_000L, -1L, 0L, 1L, 1_000L, Long.MAX_VALUE)

        epochDays.forEach { epochDay ->
            val javaDate = JavaLocalDate.ofEpochDay(epochDay)
            val date = LocalDate.ofEpochDay(epochDay)

            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                val current = runCatching { date.getLong(field) }.getOrDefault(0)
                listOf(Long.MIN_VALUE, -1L, 0L, 1L, 28L, 31L, current, Long.MAX_VALUE)
                    .distinct()
                    .forEach { newValue ->
                        assertSameOutcome(
                            javaOperation = { javaDate.with(javaField, newValue).toString() },
                            kotlinOperation = { date.with(field, newValue).toString() },
                            context = "date=$date field=$field newValue=$newValue",
                        )
                    }
            }

            ChronoUnit.entries.forEach { unit ->
                val javaUnit = JavaChronoUnit.valueOf(unit.name)
                assertEquals(javaDate.isSupported(javaUnit), date.isSupported(unit), "date=$date unit=$unit")
                amounts.forEach { amount ->
                    assertSameOutcome(
                        javaOperation = { javaDate.plus(amount, javaUnit).toString() },
                        kotlinOperation = { date.plus(amount, unit).toString() },
                        context = "date=$date unit=$unit amount=$amount",
                    )
                    assertSameOutcome(
                        javaOperation = { javaDate.minus(amount, javaUnit).toString() },
                        kotlinOperation = { date.minus(amount, unit).toString() },
                        context = "date=$date unit=$unit amount=-($amount)",
                    )
                }
            }
        }

        epochDays.forEach { startEpochDay ->
            val javaStart = JavaLocalDate.ofEpochDay(startEpochDay)
            val start = LocalDate.ofEpochDay(startEpochDay)
            epochDays.forEach { endEpochDay ->
                val javaEnd = JavaLocalDate.ofEpochDay(endEpochDay)
                val end = LocalDate.ofEpochDay(endEpochDay)
                ChronoUnit.entries.forEach { unit ->
                    val javaUnit = JavaChronoUnit.valueOf(unit.name)
                    assertSameOutcome(
                        javaOperation = { javaStart.until(javaEnd, javaUnit) },
                        kotlinOperation = { start.until(end, unit) },
                        context = "start=$start end=$end unit=$unit",
                    )
                }
            }
        }
    }

    @Test
    fun timelineComparisonMatchesJavaTime() {
        val epochDays = listOf(
            -365_243_219_162L,
            -1L,
            0L,
            1L,
            19_782L,
            365_241_780_471L,
        )

        epochDays.forEach { firstEpochDay ->
            val javaFirst = JavaLocalDate.ofEpochDay(firstEpochDay)
            val first = LocalDate.ofEpochDay(firstEpochDay)
            epochDays.forEach { secondEpochDay ->
                val javaSecond = JavaLocalDate.ofEpochDay(secondEpochDay)
                val second = LocalDate.ofEpochDay(secondEpochDay)
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
                assertEquals(javaFirst.isEqual(javaSecond), first.isEqual(second))
            }
        }
    }

    @Test
    fun instantAndEpochSecondConversionsMatchJavaTime() {
        val instants = listOf(
            Instant.MIN,
            Instant.ofEpochSecond(-1),
            Instant.EPOCH,
            Instant.parse("2024-02-29T23:30:00Z"),
            Instant.MAX,
        )
        val zones = listOf("Z", "+05:45", "Europe/Paris", "America/New_York")

        instants.forEach { instant ->
            zones.forEach { zoneId ->
                assertSameOutcome(
                    javaOperation = {
                        JavaLocalDate.ofInstant(
                            JavaInstant.ofEpochSecond(instant.epochSecond, instant.nano.toLong()),
                            JavaZoneId.of(zoneId),
                        ).toString()
                    },
                    kotlinOperation = {
                        LocalDate.ofInstant(instant, ZoneId.of(zoneId)).toString()
                    },
                    context = "instant=$instant zone=$zoneId",
                )
            }
        }

        val dates = listOf(LocalDate.MIN, LocalDate.of(-1, 12, 31), LocalDate.EPOCH, LocalDate.MAX)
        val times = listOf(LocalTime.MIN, LocalTime.NOON, LocalTime.MAX)
        val offsets = listOf(ZoneOffset.MIN, ZoneOffset.UTC, ZoneOffset.ofHoursMinutes(5, 45), ZoneOffset.MAX)
        dates.forEach { date ->
            times.forEach { time ->
                offsets.forEach { offset ->
                    assertEquals(
                        JavaLocalDate.of(date.year, date.monthValue, date.dayOfMonth).toEpochSecond(
                            JavaLocalTime.of(time.hour, time.minute, time.second, time.nano),
                            JavaZoneOffset.ofTotalSeconds(offset.totalSeconds),
                        ),
                        date.toEpochSecond(time, offset),
                        "date=$date time=$time offset=$offset",
                    )
                }
            }
        }
    }

    @Test
    fun dateSequencesMatchJavaTime() {
        val cases = listOf(
            Triple("2015-01-31", "2015-05-01", Period.ofMonths(1)),
            Triple("2015-05-31", "2015-01-01", Period.ofMonths(-1)),
            Triple("2024-01-31", "2024-07-01", Period.of(0, 1, 2)),
            Triple("2024-07-31", "2024-01-01", Period.of(0, -1, -2)),
            Triple("2024-01-01", "2024-01-03", Period.of(1, -12, 1)),
            Triple("+999999998-12-31", "+999999999-12-31", Period.ofYears(1)),
            Triple("-999999998-01-01", "-999999999-01-01", Period.ofYears(-1)),
        )

        cases.forEach { (startText, endText, step) ->
            val javaStep = JavaPeriod.of(step.years, step.months, step.days)
            assertSameOutcome(
                javaOperation = {
                    JavaLocalDate.parse(startText)
                        .datesUntil(JavaLocalDate.parse(endText), javaStep)
                        .map(JavaLocalDate::toString)
                        .toList()
                },
                kotlinOperation = {
                    LocalDate.parse(startText)
                        .datesUntil(LocalDate.parse(endText), step)
                        .map(LocalDate::toString)
                        .toList()
                },
                context = "$startText until $endText by $step",
            )
        }

        val invalidCases = listOf(
            Triple("2024-01-01", "2024-01-02", Period.ZERO),
            Triple("2024-01-01", "2024-01-02", Period.of(0, 1, -1)),
            Triple("2024-01-01", "2023-12-31", Period.ofDays(1)),
            Triple("2024-01-01", "2024-01-02", Period.ofDays(-1)),
        )
        invalidCases.forEach { (startText, endText, step) ->
            assertSameOutcome(
                javaOperation = {
                    JavaLocalDate.parse(startText).datesUntil(
                        JavaLocalDate.parse(endText),
                        JavaPeriod.of(step.years, step.months, step.days),
                    )
                },
                kotlinOperation = {
                    LocalDate.parse(startText).datesUntil(LocalDate.parse(endText), step)
                },
                context = "$startText until $endText by $step",
            )
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)

        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), context)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }
}
