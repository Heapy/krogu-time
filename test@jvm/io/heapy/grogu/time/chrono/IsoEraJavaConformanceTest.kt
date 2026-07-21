package io.heapy.grogu.time.chrono

import java.time.chrono.IsoEra as JavaIsoEra
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoEraJavaConformanceTest {
    @Test
    fun coreBehaviorMatchesJavaTime() {
        IsoEra.entries.forEach { era ->
            val javaEra = JavaIsoEra.valueOf(era.name)

            assertEquals(javaEra.value, era.value)
            assertEquals(javaEra.toString(), era.toString())
            ChronoField.entries.forEach { field ->
                val javaField = JavaChronoField.valueOf(field.name)
                assertEquals(javaEra.isSupported(javaField), era.isSupported(field))
                assertSameOutcome(
                    javaOperation = { javaEra.range(javaField).toString() },
                    kotlinOperation = { era.range(field).toString() },
                )
                assertSameOutcome(
                    javaOperation = { javaEra.getLong(javaField) },
                    kotlinOperation = { era.getLong(field) },
                )
            }
        }

        (-2..3).forEach { value ->
            assertSameOutcome(
                javaOperation = { JavaIsoEra.of(value).name },
                kotlinOperation = { IsoEra.of(value).name },
            )
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)

        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull())
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
        )
    }
}
