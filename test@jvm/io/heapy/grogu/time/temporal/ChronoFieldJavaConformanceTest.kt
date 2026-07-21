package io.heapy.grogu.time.temporal

import java.time.temporal.ChronoField as JavaChronoField
import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoFieldJavaConformanceTest {
    @Test
    fun metadataMatchesJavaTime() {
        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)

            assertEquals((javaField.baseUnit as JavaChronoUnit).name, field.baseUnit.name)
            assertEquals((javaField.rangeUnit as JavaChronoUnit).name, field.rangeUnit.name)
            assertEquals(javaField.range().toString(), field.range.toString())
            assertEquals(javaField.isDateBased, field.isDateBased)
            assertEquals(javaField.isTimeBased, field.isTimeBased)
            assertEquals(javaField.toString(), field.toString())
        }
    }
}
