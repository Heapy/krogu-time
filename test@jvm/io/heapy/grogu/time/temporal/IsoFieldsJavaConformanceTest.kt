package io.heapy.grogu.time.temporal

import io.heapy.grogu.time.LocalDate
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.IsoFields as JavaIsoFields
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoFieldsJavaConformanceTest {
    @Test
    fun quarterAndWeekFieldsMatchJavaTime() {
        val fields = listOf(
            JavaIsoFields.DAY_OF_QUARTER to IsoFields.DAY_OF_QUARTER,
            JavaIsoFields.QUARTER_OF_YEAR to IsoFields.QUARTER_OF_YEAR,
            JavaIsoFields.WEEK_OF_WEEK_BASED_YEAR to IsoFields.WEEK_OF_WEEK_BASED_YEAR,
            JavaIsoFields.WEEK_BASED_YEAR to IsoFields.WEEK_BASED_YEAR,
        )
        val dates = listOf(
            JavaLocalDate.of(2019, 12, 29) to LocalDate.of(2019, 12, 29),
            JavaLocalDate.of(2019, 12, 30) to LocalDate.of(2019, 12, 30),
            JavaLocalDate.of(2020, 2, 29) to LocalDate.of(2020, 2, 29),
            JavaLocalDate.of(2020, 12, 31) to LocalDate.of(2020, 12, 31),
            JavaLocalDate.of(2021, 1, 1) to LocalDate.of(2021, 1, 1),
            JavaLocalDate.of(2021, 1, 4) to LocalDate.of(2021, 1, 4),
            JavaLocalDate.of(2024, 12, 31) to LocalDate.of(2024, 12, 31),
        )

        fields.forEach { (javaField, field) ->
            assertEquals(javaField.toString(), field.toString())
            assertEquals(javaField.range().toString(), field.range.toString())
            dates.forEach { (javaDate, date) ->
                assertEquals(javaDate.getLong(javaField), date.getLong(field))
                assertEquals(
                    javaDate.range(javaField).toString(),
                    date.range(field).toString(),
                )
            }
        }
    }

    @Test
    fun adjustmentsAndUnitsMatchJavaTime() {
        val javaStart = JavaLocalDate.of(2020, 12, 31)
        val start = LocalDate.of(2020, 12, 31)
        val fieldsAndValues = listOf(
            Triple(JavaIsoFields.DAY_OF_QUARTER, IsoFields.DAY_OF_QUARTER, 60L),
            Triple(JavaIsoFields.QUARTER_OF_YEAR, IsoFields.QUARTER_OF_YEAR, 2L),
            Triple(JavaIsoFields.WEEK_OF_WEEK_BASED_YEAR, IsoFields.WEEK_OF_WEEK_BASED_YEAR, 20L),
            Triple(JavaIsoFields.WEEK_BASED_YEAR, IsoFields.WEEK_BASED_YEAR, 2021L),
        )
        fieldsAndValues.forEach { (javaField, field, value) ->
            assertEquals(javaStart.with(javaField, value).toString(), start.with(field, value).toString())
        }

        val units = listOf(
            JavaIsoFields.WEEK_BASED_YEARS to IsoFields.WEEK_BASED_YEARS,
            JavaIsoFields.QUARTER_YEARS to IsoFields.QUARTER_YEARS,
        )
        units.forEach { (javaUnit, unit) ->
            assertEquals(javaUnit.toString(), unit.toString())
            assertEquals(javaUnit.duration.toString(), unit.duration.toString())
            listOf(-5L, -1L, 0L, 1L, 5L).forEach { amount ->
                assertEquals(
                    javaStart.plus(amount, javaUnit).toString(),
                    start.plus(amount, unit).toString(),
                )
            }
        }
    }
}
