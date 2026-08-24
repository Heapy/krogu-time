package io.heapy.krogu.time.zone

import io.heapy.krogu.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneRulesListImmutabilityJavaConformanceTest {
    private val zoneIds = listOf("Europe/Paris", "America/New_York", "UTC", "+01:00")

    // Java hands back unmodifiable lists here. Kotlin's read-only List is a
    // compile-time guarantee only, and map and listOf return an ArrayList a
    // Java caller can clear, so the rules would leak their own state.
    @Test
    fun transitionListsRejectMutationLikeJavaTime() {
        val mismatches = zoneIds.flatMap { zoneId ->
            val javaRules = java.time.ZoneId.of(zoneId).rules
            val kroguRules = ZoneId.of(zoneId).rules

            listOf(
                "getTransitions" to Pair(
                    clearOutcome(javaRules.transitions),
                    clearOutcome(kroguRules.getTransitions()),
                ),
                "getTransitionRules" to Pair(
                    clearOutcome(javaRules.transitionRules),
                    clearOutcome(kroguRules.getTransitionRules()),
                ),
            ).mapNotNull { (name, outcomes) ->
                val (expected, actual) = outcomes
                if (expected == actual) null else "$zoneId $name: Java=$expected, Kotlin=$actual"
            }
        }

        assertEquals(emptyList(), mismatches)
    }

    // Rejecting mutation must not change what the lists contain.
    @Test
    fun transitionListContentsMatchJavaTime() {
        val mismatches = zoneIds.mapNotNull { zoneId ->
            val javaRules = java.time.ZoneId.of(zoneId).rules
            val kroguRules = ZoneId.of(zoneId).rules

            val expected = listOf<Any>(
                javaRules.transitions.size,
                javaRules.transitionRules.size,
                javaRules.transitions.map { it.toString() },
            )
            val actual = listOf<Any>(
                kroguRules.getTransitions().size,
                kroguRules.getTransitionRules().size,
                kroguRules.getTransitions().map { it.toString() },
            )
            if (expected == actual) null else "$zoneId: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // Kotlin cannot call clear() on a read-only List, and casting to
    // MutableList fails before the call is made. The question is what a Java
    // caller sees, so the call goes through the java.util.List method the way
    // Java would make it.
    private fun clearOutcome(list: List<*>): String = try {
        Class.forName("java.util.List").getMethod("clear").invoke(list)
        "no exception"
    } catch (exception: java.lang.reflect.InvocationTargetException) {
        exception.cause?.let { it::class.simpleName } ?: "unknown"
    }
}
