package io.heapy.krogu.time.format

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalField
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter
import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.TemporalAccessor as JavaTemporalAccessor
import java.time.temporal.TemporalField as JavaTemporalField
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterPlainAccessorJavaConformanceTest {
    private val inputs = listOf(
        Input("empty"),
        Input("time", "HOUR_OF_DAY" to 11, "MINUTE_OF_HOUR" to 5),
        Input(
            "time with second",
            "HOUR_OF_DAY" to 11,
            "MINUTE_OF_HOUR" to 5,
            "SECOND_OF_MINUTE" to 30,
        ),
        Input(
            "time with fraction",
            "HOUR_OF_DAY" to 11,
            "MINUTE_OF_HOUR" to 5,
            "SECOND_OF_MINUTE" to 30,
            "NANO_OF_SECOND" to 120_000_000,
        ),
        Input(
            "time with fraction but no second",
            "HOUR_OF_DAY" to 11,
            "MINUTE_OF_HOUR" to 5,
            "NANO_OF_SECOND" to 120_000_000,
        ),
        Input("date", "YEAR" to 2024, "MONTH_OF_YEAR" to 2, "DAY_OF_MONTH" to 29),
        Input(
            "date-time",
            "YEAR" to 2024,
            "MONTH_OF_YEAR" to 2,
            "DAY_OF_MONTH" to 29,
            "HOUR_OF_DAY" to 11,
            "MINUTE_OF_HOUR" to 5,
        ),
        Input(
            "offset time",
            "HOUR_OF_DAY" to 11,
            "MINUTE_OF_HOUR" to 5,
            "OFFSET_SECONDS" to 3_600,
        ),
        Input("offset only", "OFFSET_SECONDS" to 3_600),
        Input("instant", "INSTANT_SECONDS" to 1_709_208_330, "NANO_OF_SECOND" to 0),
        Input("instant without nano", "INSTANT_SECONDS" to 1_709_208_330),
        Input("nano only", "NANO_OF_SECOND" to 123_456_789),
    )

    private val formatters = listOf(
        Formatter(
            "ISO_LOCAL_TIME",
            JavaDateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ISO_LOCAL_TIME,
        ),
        Formatter(
            "ISO_LOCAL_DATE",
            JavaDateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE,
        ),
        Formatter(
            "ISO_LOCAL_DATE_TIME",
            JavaDateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        ),
        Formatter(
            "ISO_OFFSET_TIME",
            JavaDateTimeFormatter.ISO_OFFSET_TIME,
            DateTimeFormatter.ISO_OFFSET_TIME,
        ),
        Formatter("ISO_INSTANT", JavaDateTimeFormatter.ISO_INSTANT, DateTimeFormatter.ISO_INSTANT),
    )

    @Test
    fun plainTemporalAccessorFormattingMatchesJavaTime() {
        val mismatches = inputs.flatMap { input ->
            formatters.mapNotNull { formatter ->
                val expected = outcome {
                    formatter.java.format(JavaMapAccessor(input.fields))
                }
                val actual = outcome {
                    formatter.krogu.format(KroguMapAccessor(input.fields))
                }
                if (expected == actual) {
                    null
                } else {
                    "${formatter.name}, ${input.name}: Java=$expected, Kotlin=$actual"
                }
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun outcome(operation: () -> String): Outcome = try {
        Outcome.Success(operation())
    } catch (exception: RuntimeException) {
        Outcome.Failure(exception.javaClass.simpleName)
    }

    private data class Input(
        val name: String,
        val fields: Map<String, Long>,
    ) {
        constructor(name: String, vararg fields: Pair<String, Int>) : this(
            name,
            fields.associate { (field, value) -> field to value.toLong() },
        )
    }

    private data class Formatter(
        val name: String,
        val java: JavaDateTimeFormatter,
        val krogu: DateTimeFormatter,
    )

    private sealed interface Outcome {
        data class Success(val text: String) : Outcome

        data class Failure(val exceptionType: String) : Outcome
    }

    private class JavaMapAccessor(fieldValues: Map<String, Long>) : JavaTemporalAccessor {
        private val fields: Map<JavaTemporalField, Long> = fieldValues.mapKeys { (field) ->
            JavaChronoField.valueOf(field)
        }

        override fun isSupported(field: JavaTemporalField): Boolean = fields.containsKey(field)

        override fun getLong(field: JavaTemporalField): Long =
            fields[field] ?: throw java.time.DateTimeException("Field missing: $field")
    }

    private class KroguMapAccessor(fieldValues: Map<String, Long>) : TemporalAccessor {
        private val fields: Map<TemporalField, Long> = fieldValues.mapKeys { (field) ->
            ChronoField.valueOf(field)
        }

        override fun isSupported(field: TemporalField?): Boolean = fields.containsKey(field)

        override fun getLong(field: TemporalField): Long =
            fields[field] ?: throw DateTimeException("Field missing: $field")
    }
}
