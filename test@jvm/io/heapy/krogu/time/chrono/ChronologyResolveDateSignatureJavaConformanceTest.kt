package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.format.ResolverStyle
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalField
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronologyResolveDateSignatureJavaConformanceTest {
    private val chronologies = listOf<Pair<Chronology, java.time.chrono.Chronology>>(
        IsoChronology to java.time.chrono.IsoChronology.INSTANCE,
        HijrahChronology to java.time.chrono.HijrahChronology.INSTANCE,
        JapaneseChronology to java.time.chrono.JapaneseChronology.INSTANCE,
        MinguoChronology to java.time.chrono.MinguoChronology.INSTANCE,
        ThaiBuddhistChronology to java.time.chrono.ThaiBuddhistChronology.INSTANCE,
    )

    // Java narrows the return type of resolveDate in every calendar system, so
    // a Java caller never casts. The port inherited ChronoLocalDate in four of
    // the five, which forced a cast the JDK does not need.
    @Test
    fun resolveDateReturnTypesMatchJavaTime() {
        val mismatches = chronologies.mapNotNull { (krogu, reference) ->
            val expected = declaredResolveDateReturn(reference.javaClass)
            val actual = declaredResolveDateReturn(krogu.javaClass)
            if (expected == actual) null else "${krogu.id}: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // Narrowing a return type must not change what comes back.
    @Test
    fun resolveDateResultsMatchJavaTime() {
        val mismatches = chronologies.flatMap { (krogu, reference) ->
            ResolverStyle.entries.map { style ->
                val javaStyle = java.time.format.ResolverStyle.valueOf(style.name)
                val expected = outcome {
                    reference.resolveDate(javaFields(reference), javaStyle)
                }
                val actual = outcome { krogu.resolveDate(kroguFields(krogu), style) }
                Triple(krogu.id, style, expected to actual)
            }.mapNotNull { (id, style, outcomes) ->
                val (expected, actual) = outcomes
                if (expected == actual) null else "$id $style: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    // getMethod can hand back the bridge method that still returns the wide
    // type, so the declared, non-bridge method is the one to read.
    private fun declaredResolveDateReturn(type: Class<*>): String =
        type.declaredMethods
            .filter { it.name == "resolveDate" && !it.isBridge && !it.isSynthetic }
            .map { it.returnType.simpleName }
            .sorted()
            .joinToString()
            .ifEmpty { "inherited" }

    private fun kroguFields(chronology: Chronology): MutableMap<TemporalField, Long> {
        val date = chronology.dateNow()
        return mutableMapOf(
            ChronoField.YEAR to date.getLong(ChronoField.YEAR),
            ChronoField.MONTH_OF_YEAR to date.getLong(ChronoField.MONTH_OF_YEAR),
            ChronoField.DAY_OF_MONTH to date.getLong(ChronoField.DAY_OF_MONTH),
        )
    }

    private fun javaFields(
        chronology: java.time.chrono.Chronology,
    ): MutableMap<java.time.temporal.TemporalField, Long> {
        val date = chronology.dateNow()
        return mutableMapOf(
            java.time.temporal.ChronoField.YEAR to
                date.getLong(java.time.temporal.ChronoField.YEAR),
            java.time.temporal.ChronoField.MONTH_OF_YEAR to
                date.getLong(java.time.temporal.ChronoField.MONTH_OF_YEAR),
            java.time.temporal.ChronoField.DAY_OF_MONTH to
                date.getLong(java.time.temporal.ChronoField.DAY_OF_MONTH),
        )
    }

    private fun outcome(block: () -> Any?): String = try {
        block().toString()
    } catch (exception: Throwable) {
        exception::class.simpleName ?: "unknown"
    }
}
