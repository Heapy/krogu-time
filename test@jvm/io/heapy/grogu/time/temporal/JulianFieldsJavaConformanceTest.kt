package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.LocalDate
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.JulianFields as JavaJulianFields
import kotlin.test.Test
import kotlin.test.assertEquals

class JulianFieldsJavaConformanceTest {
    @Test
    fun metadataRangesValuesAndAdjustmentsMatchJavaTime() {
        val fields = listOf(
            JavaJulianFields.JULIAN_DAY to JulianFields.JULIAN_DAY,
            JavaJulianFields.MODIFIED_JULIAN_DAY to JulianFields.MODIFIED_JULIAN_DAY,
            JavaJulianFields.RATA_DIE to JulianFields.RATA_DIE,
        )
        val dates = listOf(
            JavaLocalDate.MIN to LocalDate.MIN,
            JavaLocalDate.of(-4_713, 11, 24) to LocalDate.of(-4_713, 11, 24),
            JavaLocalDate.of(1, 1, 1) to LocalDate.of(1, 1, 1),
            JavaLocalDate.of(1970, 1, 1) to LocalDate.EPOCH,
            JavaLocalDate.of(2024, 2, 29) to LocalDate.of(2024, 2, 29),
            JavaLocalDate.MAX to LocalDate.MAX,
        )

        fields.forEach { (javaField, field) ->
            assertEquals(javaField.toString(), field.toString())
            assertEquals(javaField.range().toString(), field.range.toString())
            assertEquals(javaField.baseUnit.toString(), field.baseUnit.toString())
            assertEquals(javaField.rangeUnit.toString(), field.rangeUnit.toString())
            dates.forEach { (javaDate, date) ->
                assertEquals(javaDate.getLong(javaField), date.getLong(field))
                assertEquals(
                    javaDate.with(javaField, javaDate.getLong(javaField)).toString(),
                    date.with(field, date.getLong(field)).toString(),
                )
            }
        }
    }
}
