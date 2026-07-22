package io.heapy.grogu.time

import java.time.LocalDateTime as JavaLocalDateTime
import java.time.temporal.ChronoField as JavaChronoField
import io.heapy.grogu.time.temporal.ChronoField
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeJavaConformanceTest {
    @Test
    fun foundationBehaviorMatchesJavaTime() {
        val cases = listOf(
            LocalDateTime.MIN,
            LocalDateTime.of(-1, 1, 1, 0, 0),
            LocalDateTime.of(1970, 1, 1, 0, 0),
            LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789),
            LocalDateTime.MAX,
        )

        cases.forEach { dateTime ->
            val javaDateTime = JavaLocalDateTime.of(
                dateTime.year,
                dateTime.monthValue,
                dateTime.dayOfMonth,
                dateTime.hour,
                dateTime.minute,
                dateTime.second,
                dateTime.nano,
            )
            assertEquals(javaDateTime.year, dateTime.year)
            assertEquals(javaDateTime.monthValue, dateTime.monthValue)
            assertEquals(javaDateTime.month.value, dateTime.month.value)
            assertEquals(javaDateTime.dayOfMonth, dateTime.dayOfMonth)
            assertEquals(javaDateTime.dayOfYear, dateTime.dayOfYear)
            assertEquals(javaDateTime.dayOfWeek.value, dateTime.dayOfWeek.value)
            assertEquals(javaDateTime.hour, dateTime.hour)
            assertEquals(javaDateTime.minute, dateTime.minute)
            assertEquals(javaDateTime.second, dateTime.second)
            assertEquals(javaDateTime.nano, dateTime.nano)
            assertEquals(javaDateTime.toString(), dateTime.toString())
            assertEquals(javaDateTime.hashCode(), dateTime.hashCode())
        }

        cases.forEach { first ->
            val javaFirst = JavaLocalDateTime.of(
                first.year,
                first.monthValue,
                first.dayOfMonth,
                first.hour,
                first.minute,
                first.second,
                first.nano,
            )
            cases.forEach { second ->
                val javaSecond = JavaLocalDateTime.of(
                    second.year,
                    second.monthValue,
                    second.dayOfMonth,
                    second.hour,
                    second.minute,
                    second.second,
                    second.nano,
                )
                assertEquals(javaFirst.compareTo(javaSecond), first.compareTo(second))
                assertEquals(javaFirst.isAfter(javaSecond), first.isAfter(second))
                assertEquals(javaFirst.isBefore(javaSecond), first.isBefore(second))
                assertEquals(javaFirst.isEqual(javaSecond), first.isEqual(second))
            }
        }
    }

    @Test
    fun temporalFieldBehaviorMatchesJavaTime() {
        val javaDateTime = JavaLocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        val dateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)

        ChronoField.entries.forEach { field ->
            val javaField = JavaChronoField.valueOf(field.name)
            val context = field.toString()
            assertEquals(javaDateTime.isSupported(javaField), dateTime.isSupported(field), context)
            assertSameOutcome(
                javaOperation = { javaDateTime.range(javaField).toString() },
                kotlinOperation = { dateTime.range(field).toString() },
                context = context,
            )
            assertSameOutcome(
                javaOperation = { javaDateTime.get(javaField) },
                kotlinOperation = { dateTime.get(field) },
                context = context,
            )
            assertSameOutcome(
                javaOperation = { javaDateTime.getLong(javaField) },
                kotlinOperation = { dateTime.getLong(field) },
                context = context,
            )
        }
    }

    private fun assertSameOutcome(
        javaOperation: () -> Any?,
        kotlinOperation: () -> Any?,
        context: String,
    ) {
        val javaResult = runCatching(javaOperation)
        val kotlinResult = runCatching(kotlinOperation)
        assertEquals(javaResult.getOrNull(), kotlinResult.getOrNull(), context)
        assertEquals(
            javaResult.exceptionOrNull()?.javaClass?.simpleName,
            kotlinResult.exceptionOrNull()?.javaClass?.simpleName,
            context,
        )
    }
}
