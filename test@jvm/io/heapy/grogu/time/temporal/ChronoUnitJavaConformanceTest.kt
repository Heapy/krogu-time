package io.heapy.grogu.time.temporal

import java.time.temporal.ChronoUnit as JavaChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoUnitJavaConformanceTest {
    @Test
    fun metadataMatchesJavaTime() {
        ChronoUnit.entries.forEach { unit ->
            val javaUnit = JavaChronoUnit.valueOf(unit.name)

            assertEquals(javaUnit.duration.seconds, unit.duration.seconds)
            assertEquals(javaUnit.duration.nano, unit.duration.nano)
            assertEquals(javaUnit.isDurationEstimated, unit.isDurationEstimated)
            assertEquals(javaUnit.isDateBased, unit.isDateBased)
            assertEquals(javaUnit.isTimeBased, unit.isTimeBased)
            assertEquals(javaUnit.toString(), unit.toString())
        }
    }
}
