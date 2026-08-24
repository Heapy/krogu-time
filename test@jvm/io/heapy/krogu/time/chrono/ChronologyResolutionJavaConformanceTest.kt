package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalField
import java.time.chrono.Chronology as JavaChronology
import java.time.format.ResolverStyle as JavaResolverStyle
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.TemporalField as JavaTemporalField
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronologyResolutionJavaConformanceTest {
    @Test
    fun standardDateFieldResolutionMatchesJavaTime() {
        Chronology.getAvailableChronologies().forEach { chronology ->
            val javaChronology = JavaChronology.of(chronology.id)
            val latestEra = chronology.range(ChronoField.ERA).maximum
            val scenarios = scenarios(latestEra)

            ResolverStyle.entries.forEach { resolverStyle ->
                val javaResolverStyle = JavaResolverStyle.valueOf(resolverStyle.name)
                scenarios.forEach { (name, values) ->
                    assertEquals(
                        javaOutcome(javaChronology, values, javaResolverStyle),
                        kroguOutcome(chronology, values, resolverStyle),
                        "${chronology.id} $resolverStyle $name",
                    )
                }
            }
        }
    }

    private fun scenarios(latestEra: Long): List<Scenario> = listOf(
        Scenario("empty", emptyMap()),
        Scenario("insufficient", mapOf("YEAR" to 2024, "MONTH_OF_YEAR" to 2)),
        Scenario("epochDay", mapOf("EPOCH_DAY" to 0, "YEAR" to 2024)),
        Scenario("prolepticMonth", mapOf("PROLEPTIC_MONTH" to 2024L * 12 + 1, "DAY_OF_MONTH" to 29)),
        Scenario(
            "yearOfEraWithEra",
            mapOf(
                "ERA" to latestEra,
                "YEAR_OF_ERA" to 2,
                "MONTH_OF_YEAR" to 2,
                "DAY_OF_MONTH" to 28,
            ),
        ),
        Scenario(
            "yearOfEraWithoutEra",
            mapOf("YEAR_OF_ERA" to 2, "MONTH_OF_YEAR" to 2, "DAY_OF_MONTH" to 28),
        ),
        Scenario("calendarDate", mapOf("YEAR" to 2024, "MONTH_OF_YEAR" to 2, "DAY_OF_MONTH" to 29)),
        Scenario("invalidDay", mapOf("YEAR" to 2023, "MONTH_OF_YEAR" to 2, "DAY_OF_MONTH" to 31)),
        Scenario("overflowingCalendarDate", mapOf("YEAR" to 2023, "MONTH_OF_YEAR" to 15, "DAY_OF_MONTH" to -3)),
        Scenario("ordinalDate", mapOf("YEAR" to 2024, "DAY_OF_YEAR" to 60)),
        Scenario("overflowingOrdinalDate", mapOf("YEAR" to 2023, "DAY_OF_YEAR" to 370)),
        Scenario(
            "alignedDayInMonth",
            mapOf(
                "YEAR" to 2024,
                "MONTH_OF_YEAR" to 2,
                "ALIGNED_WEEK_OF_MONTH" to 2,
                "ALIGNED_DAY_OF_WEEK_IN_MONTH" to 3,
            ),
        ),
        Scenario(
            "dayOfWeekInMonth",
            mapOf(
                "YEAR" to 2024,
                "MONTH_OF_YEAR" to 2,
                "ALIGNED_WEEK_OF_MONTH" to 2,
                "DAY_OF_WEEK" to 4,
            ),
        ),
        Scenario(
            "alignedDayInYear",
            mapOf(
                "YEAR" to 2024,
                "ALIGNED_WEEK_OF_YEAR" to 9,
                "ALIGNED_DAY_OF_WEEK_IN_YEAR" to 4,
            ),
        ),
        Scenario(
            "dayOfWeekInYear",
            mapOf("YEAR" to 2024, "ALIGNED_WEEK_OF_YEAR" to 9, "DAY_OF_WEEK" to 4),
        ),
        Scenario(
            "prolepticMonthConflict",
            mapOf("PROLEPTIC_MONTH" to 2024L * 12 + 1, "YEAR" to 2023, "DAY_OF_MONTH" to 29),
        ),
        Scenario("invalidEra", mapOf("ERA" to latestEra + 1)),
        Scenario(
            "firstEraYearCalendarDate",
            mapOf(
                "ERA" to latestEra,
                "YEAR_OF_ERA" to 1,
                "MONTH_OF_YEAR" to 1,
                "DAY_OF_MONTH" to 1,
            ),
        ),
        Scenario(
            "firstEraYearOrdinalDate",
            mapOf("ERA" to latestEra, "YEAR_OF_ERA" to 1, "DAY_OF_YEAR" to 1),
        ),
    )

    private fun javaOutcome(
        chronology: JavaChronology,
        values: Map<String, Long>,
        resolverStyle: JavaResolverStyle,
    ): Outcome {
        val fields = values.mapKeysTo(mutableMapOf<JavaTemporalField, Long>()) { (name) ->
            JavaChronoField.valueOf(name)
        }
        return runCatching { chronology.resolveDate(fields, resolverStyle) }.fold(
            onSuccess = { date ->
                Outcome(
                    date = date?.toString(),
                    epochDay = date?.toEpochDay(),
                    fields = fields.mapKeys { (field) -> field.toString() }.toSortedMap(),
                )
            },
            onFailure = { Outcome(error = it.javaClass.simpleName) },
        )
    }

    private fun kroguOutcome(
        chronology: Chronology,
        values: Map<String, Long>,
        resolverStyle: ResolverStyle,
    ): Outcome {
        val fields = values.mapKeysTo(mutableMapOf<TemporalField, Long>()) { (name) ->
            ChronoField.valueOf(name)
        }
        return runCatching { chronology.resolveDate(fields, resolverStyle) }.fold(
            onSuccess = { date ->
                Outcome(
                    date = date?.toString(),
                    epochDay = date?.toEpochDay(),
                    fields = fields.mapKeys { (field) -> field.toString() }.toSortedMap(),
                )
            },
            onFailure = { Outcome(error = it.javaClass.simpleName) },
        )
    }

    private data class Scenario(
        val name: String,
        val fields: Map<String, Long>,
    )

    private data class Outcome(
        val date: String? = null,
        val epochDay: Long? = null,
        val fields: Map<String, Long> = emptyMap(),
        val error: String? = null,
    )
}
