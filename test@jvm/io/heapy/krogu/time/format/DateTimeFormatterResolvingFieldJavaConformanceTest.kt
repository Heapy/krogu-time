package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDateTime
import io.heapy.krogu.time.LocalTime
import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZonedDateTime
import io.heapy.krogu.time.chrono.Chronology
import io.heapy.krogu.time.chrono.MinguoChronology
import io.heapy.krogu.time.chrono.MinguoDate
import io.heapy.krogu.time.chrono.ThaiBuddhistDate
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.Temporal
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.temporal.TemporalUnit
import io.heapy.krogu.time.temporal.ValueRange
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.LocalTime as JavaLocalTime
import java.time.ZoneId as JavaZoneId
import java.time.ZonedDateTime as JavaZonedDateTime
import java.time.chrono.Chronology as JavaChronology
import java.time.chrono.MinguoChronology as JavaMinguoChronology
import java.time.chrono.MinguoDate as JavaMinguoDate
import java.time.chrono.ThaiBuddhistDate as JavaThaiBuddhistDate
import java.time.format.DateTimeFormatterBuilder as JavaDateTimeFormatterBuilder
import java.time.format.ResolverStyle as JavaResolverStyle
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.Temporal as JavaTemporal
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalField as JavaTemporalField
import java.time.temporal.TemporalQueries as JavaTemporalQueries
import java.time.temporal.TemporalUnit as JavaTemporalUnit
import java.time.temporal.ValueRange as JavaValueRange
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterResolvingFieldJavaConformanceTest {
    private val javaParis = JavaZoneId.of("Europe/Paris")
    private val javaLondon = JavaZoneId.of("Europe/London")
    private val paris = ZoneId.of("Europe/Paris")
    private val london = ZoneId.of("Europe/London")

    private val inputs = listOf(
        Input(
            "LocalTime",
            java = { javaResolvedValue(JavaLocalTime.of(12, 30, 40)) },
            krogu = { kroguResolvedValue(LocalTime.of(12, 30, 40)) },
        ),
        Input(
            "ChronoLocalDateTime without override, matching chronology",
            java = { javaResolvedValue(JavaLocalDateTime.of(2010, 6, 30, 12, 30)) },
            krogu = { kroguResolvedValue(LocalDateTime.of(2010, 6, 30, 12, 30)) },
        ),
        Input(
            "ChronoLocalDateTime with override, matching chronology",
            java = {
                javaResolvedValue(
                    JavaMinguoDate.of(100, 6, 30).atTime(JavaLocalTime.NOON),
                    chronology = JavaMinguoChronology.INSTANCE,
                )
            },
            krogu = {
                kroguResolvedValue(
                    MinguoDate.of(100, 6, 30).atTime(LocalTime.NOON),
                    chronology = MinguoChronology,
                )
            },
        ),
        Input(
            "ChronoLocalDateTime without override, wrong chronology",
            java = {
                javaResolvedValue(
                    JavaThaiBuddhistDate.of(2569, 8, 24).atTime(JavaLocalTime.NOON),
                )
            },
            krogu = {
                kroguResolvedValue(
                    ThaiBuddhistDate.of(2569, 8, 24).atTime(LocalTime.NOON),
                )
            },
        ),
        Input(
            "ChronoLocalDateTime with override, wrong chronology",
            java = {
                javaResolvedValue(
                    JavaThaiBuddhistDate.of(2569, 8, 24).atTime(JavaLocalTime.NOON),
                    chronology = JavaMinguoChronology.INSTANCE,
                )
            },
            krogu = {
                kroguResolvedValue(
                    ThaiBuddhistDate.of(2569, 8, 24).atTime(LocalTime.NOON),
                    chronology = MinguoChronology,
                )
            },
        ),
        Input(
            "ChronoZonedDateTime without override, matching chronology",
            java = {
                javaResolvedValue(
                    JavaZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, javaParis),
                )
            },
            krogu = {
                kroguResolvedValue(
                    ZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, paris),
                )
            },
        ),
        Input(
            "ChronoZonedDateTime with override, matching chronology",
            java = {
                javaResolvedValue(
                    JavaMinguoDate.of(100, 6, 30).atTime(JavaLocalTime.NOON)
                        .atZone(javaParis),
                    chronology = JavaMinguoChronology.INSTANCE,
                )
            },
            krogu = {
                kroguResolvedValue(
                    MinguoDate.of(100, 6, 30).atTime(LocalTime.NOON).atZone(paris),
                    chronology = MinguoChronology,
                )
            },
        ),
        Input(
            "ChronoZonedDateTime without override, wrong chronology",
            java = {
                javaResolvedValue(
                    JavaThaiBuddhistDate.of(2569, 8, 24).atTime(JavaLocalTime.NOON)
                        .atZone(javaParis),
                )
            },
            krogu = {
                kroguResolvedValue(
                    ThaiBuddhistDate.of(2569, 8, 24).atTime(LocalTime.NOON).atZone(paris),
                )
            },
        ),
        Input(
            "ChronoZonedDateTime with override, wrong chronology",
            java = {
                javaResolvedValue(
                    JavaThaiBuddhistDate.of(2569, 8, 24).atTime(JavaLocalTime.NOON)
                        .atZone(javaParis),
                    chronology = JavaMinguoChronology.INSTANCE,
                )
            },
            krogu = {
                kroguResolvedValue(
                    ThaiBuddhistDate.of(2569, 8, 24).atTime(LocalTime.NOON).atZone(paris),
                    chronology = MinguoChronology,
                )
            },
        ),
        Input(
            "ChronoZonedDateTime with matching zone override",
            java = {
                javaResolvedValue(
                    JavaZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, javaParis),
                    zone = javaParis,
                    queryZonedDateTime = true,
                )
            },
            krogu = {
                kroguResolvedValue(
                    ZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, paris),
                    zone = paris,
                    queryZonedDateTime = true,
                )
            },
        ),
        Input(
            "ChronoZonedDateTime with wrong zone override",
            java = {
                javaResolvedValue(
                    JavaZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, javaParis),
                    zone = javaLondon,
                )
            },
            krogu = {
                kroguResolvedValue(
                    ZonedDateTime.of(2010, 6, 30, 12, 30, 0, 0, paris),
                    zone = london,
                )
            },
        ),
    ) + ResolverStyle.entries.flatMap { style ->
        listOf(-1L, 0L, 1L, 2L).map { value ->
            Input(
                "AMPM_OF_DAY $style $value",
                java = { javaAmPm(style, value) },
                krogu = { kroguAmPm(style, value) },
            )
        }
    }

    @Test
    fun temporalFieldResolutionMatchesJavaTime() {
        val mismatches = inputs.mapNotNull { input ->
            val expected = outcome(input.java)
            val actual = outcome(input.krogu)
            if (expected == actual) {
                null
            } else {
                "${input.name}: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun javaResolvedValue(
        resolvedValue: JavaTemporalAccessor,
        chronology: JavaChronology? = null,
        zone: JavaZoneId? = null,
        queryZonedDateTime: Boolean = false,
    ): String {
        var formatter = JavaDateTimeFormatterBuilder()
            .appendValue(JavaResolvingField(resolvedValue))
            .toFormatter()
        chronology?.let { formatter = formatter.withChronology(it) }
        zone?.let { formatter = formatter.withZone(it) }
        val parsed = formatter.parse("1234567890")
        return if (queryZonedDateTime) {
            JavaZonedDateTime.from(parsed).toString()
        } else {
            javaSnapshot(parsed)
        }
    }

    private fun kroguResolvedValue(
        resolvedValue: TemporalAccessor,
        chronology: Chronology? = null,
        zone: ZoneId? = null,
        queryZonedDateTime: Boolean = false,
    ): String {
        var formatter = DateTimeFormatterBuilder()
            .appendValue(KroguResolvingField(resolvedValue))
            .toFormatter()
        chronology?.let { formatter = formatter.withChronology(it) }
        zone?.let { formatter = formatter.withZone(it) }
        val parsed = formatter.parse("1234567890")
        return if (queryZonedDateTime) {
            ZonedDateTime.from(parsed).toString()
        } else {
            kroguSnapshot(parsed)
        }
    }

    private fun javaAmPm(style: ResolverStyle, value: Long): String {
        val parsed = JavaDateTimeFormatterBuilder().appendValue(JavaChronoField.AMPM_OF_DAY)
            .toFormatter()
            .withResolverStyle(JavaResolverStyle.valueOf(style.name))
            .parse(value.toString())
        return javaSnapshot(parsed) + ", amPm=" +
            parsed.getLong(JavaChronoField.AMPM_OF_DAY)
    }

    private fun kroguAmPm(style: ResolverStyle, value: Long): String {
        val parsed = DateTimeFormatterBuilder().appendValue(ChronoField.AMPM_OF_DAY)
            .toFormatter()
            .withResolverStyle(style)
            .parse(value.toString())
        return kroguSnapshot(parsed) + ", amPm=" + parsed.getLong(ChronoField.AMPM_OF_DAY)
    }

    private fun javaSnapshot(temporal: JavaTemporalAccessor): String =
        "date=${temporal.query(JavaTemporalQueries.localDate())}, " +
            "time=${temporal.query(JavaTemporalQueries.localTime())}, " +
            "chronology=${temporal.query(JavaTemporalQueries.chronology())?.id}, " +
            "zone=${temporal.query(JavaTemporalQueries.zoneId())}"

    private fun kroguSnapshot(temporal: TemporalAccessor): String =
        "date=${temporal.query(TemporalQueries.localDate())}, " +
            "time=${temporal.query(TemporalQueries.localTime())}, " +
            "chronology=${temporal.query(TemporalQueries.chronology())?.id}, " +
            "zone=${temporal.query(TemporalQueries.zoneId())}"

    private fun outcome(operation: () -> String): Outcome = try {
        Outcome.Success(operation())
    } catch (exception: RuntimeException) {
        Outcome.Failure(exception.javaClass.simpleName)
    }

    private data class Input(
        val name: String,
        val java: () -> String,
        val krogu: () -> String,
    )

    private sealed interface Outcome {
        data class Success(val value: String) : Outcome

        data class Failure(val exceptionType: String) : Outcome
    }

    private class JavaResolvingField(
        private val resolvedValue: JavaTemporalAccessor,
    ) : JavaTemporalField {
        override fun getBaseUnit(): JavaTemporalUnit = error("Unused")

        override fun getRangeUnit(): JavaTemporalUnit = error("Unused")

        override fun range(): JavaValueRange = error("Unused")

        override fun isDateBased(): Boolean = error("Unused")

        override fun isTimeBased(): Boolean = error("Unused")

        override fun isSupportedBy(temporal: JavaTemporalAccessor): Boolean = error("Unused")

        override fun rangeRefinedBy(temporal: JavaTemporalAccessor): JavaValueRange =
            error("Unused")

        override fun getFrom(temporal: JavaTemporalAccessor): Long = error("Unused")

        override fun <R : JavaTemporal> adjustInto(temporal: R, newValue: Long): R =
            error("Unused")

        override fun resolve(
            fieldValues: MutableMap<JavaTemporalField, Long>,
            partialTemporal: JavaTemporalAccessor,
            resolverStyle: JavaResolverStyle,
        ): JavaTemporalAccessor {
            fieldValues.remove(this)
            return resolvedValue
        }
    }

    private class KroguResolvingField(
        private val resolvedValue: TemporalAccessor,
    ) : TemporalField {
        override val baseUnit: TemporalUnit get() = error("Unused")
        override val rangeUnit: TemporalUnit get() = error("Unused")
        override val range: ValueRange get() = error("Unused")
        override val isDateBased: Boolean get() = error("Unused")
        override val isTimeBased: Boolean get() = error("Unused")

        override fun isSupportedBy(temporal: TemporalAccessor): Boolean = error("Unused")

        override fun rangeRefinedBy(temporal: TemporalAccessor): ValueRange = error("Unused")

        override fun getFrom(temporal: TemporalAccessor): Long = error("Unused")

        override fun <R : Temporal> adjustInto(temporal: R, newValue: Long): R =
            error("Unused")

        override fun resolve(
            fieldValues: MutableMap<TemporalField, Long>,
            partialTemporal: TemporalAccessor,
            resolverStyle: ResolverStyle,
        ): TemporalAccessor {
            fieldValues.remove(this)
            return resolvedValue
        }
    }
}
