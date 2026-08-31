package io.heapy.krogu.time

import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneIdShortIdsImmutabilityJavaConformanceTest {
    // Java builds SHORT_IDS with Collections.unmodifiableMap, which also wraps
    // the entry set, its iterator and every entry. Kotlin's read-only Map is a
    // compile-time guarantee only, and mapOf returns a LinkedHashMap whose
    // views a Java caller can mutate, so any of these paths would empty the
    // shared constant for the rest of the process.
    @Test
    fun shortIdsRejectMutationLikeJavaTime() {
        val mismatches = mutations.mapNotNull { (name, mutate) ->
            val expected = outcome(java.time.ZoneId.SHORT_IDS, mutate)
            val actual = outcome(ZoneId.SHORT_IDS, mutate)
            if (expected == actual) null else "$name: Java=$expected, Kotlin=$actual"
        }

        assertEquals(emptyList(), mismatches)
    }

    // Rejecting mutation must not change what the map contains.
    @Test
    fun shortIdsContentsMatchJavaTime() {
        val expected = java.time.ZoneId.SHORT_IDS
        val keys = (expected.keys + ZoneId.SHORT_IDS.keys).sorted()
        val mismatches = keys.mapNotNull { key ->
            val javaValue = expected[key]
            val kroguValue = ZoneId.SHORT_IDS[key]
            if (javaValue == kroguValue) {
                null
            } else {
                "$key: Java=$javaValue, Kotlin=$kroguValue"
            }
        }

        assertEquals(emptyList(), mismatches)
        assertEquals(expected.size, ZoneId.SHORT_IDS.size)
    }

    // Kotlin cannot call clear() on a read-only Map, and casting to MutableMap
    // fails before the call is made. The question is what a Java caller sees,
    // so the calls go through the java.util methods the way Java would make
    // them. Any::class.java is Object.class, which is what those methods
    // declare.
    private val mutations: List<Pair<String, (Map<String, String>) -> Unit>> = listOf(
        "clear()" to { map ->
            mapClass.getMethod("clear").invoke(map)
        },
        "put()" to { map ->
            mapClass
                .getMethod("put", Any::class.java, Any::class.java)
                .invoke(map, "XYZ", "Antarctica/Troll")
        },
        "remove()" to { map ->
            mapClass.getMethod("remove", Any::class.java).invoke(map, "ECT")
        },
        "entrySet().clear()" to { map ->
            collectionClass.getMethod("clear").invoke(map.entries)
        },
        "entrySet().iterator().remove()" to { map ->
            removeThroughIterator(map.entries.iterator())
        },
        "entrySet().iterator().next().setValue()" to { map ->
            entryClass
                .getMethod("setValue", Any::class.java)
                .invoke(map.entries.iterator().next(), "Antarctica/Troll")
        },
        "keySet().clear()" to { map ->
            collectionClass.getMethod("clear").invoke(map.keys)
        },
        "keySet().iterator().remove()" to { map ->
            removeThroughIterator(map.keys.iterator())
        },
        "values().clear()" to { map ->
            collectionClass.getMethod("clear").invoke(map.values)
        },
        "values().iterator().remove()" to { map ->
            removeThroughIterator(map.values.iterator())
        },
    )

    private val mapClass: Class<*> get() = Class.forName("java.util.Map")

    private val entryClass: Class<*> get() = Class.forName("java.util.Map\$Entry")

    private val collectionClass: Class<*> get() = Class.forName("java.util.Collection")

    private val iteratorClass: Class<*> get() = Class.forName("java.util.Iterator")

    private fun removeThroughIterator(iterator: Iterator<*>) {
        iterator.next()
        iteratorClass.getMethod("remove").invoke(iterator)
    }

    private fun outcome(
        map: Map<String, String>,
        mutate: (Map<String, String>) -> Unit,
    ): String {
        val snapshot = map.toMap()
        return try {
            mutate(map)
            "no exception"
        } catch (exception: InvocationTargetException) {
            exception.cause?.let { it::class.simpleName } ?: "unknown"
        } finally {
            // A probe that really mutates one of these shared statics would
            // otherwise decide what the later probes are allowed to see.
            restore(map, snapshot)
        }
    }

    private fun restore(map: Map<String, String>, snapshot: Map<String, String>) {
        if (map == snapshot) return
        try {
            mapClass.getMethod("clear").invoke(map)
            mapClass.getMethod("putAll", mapClass).invoke(map, snapshot)
        } catch (exception: InvocationTargetException) {
            throw AssertionError("Mutated map could not be restored", exception.cause)
        }
    }
}
