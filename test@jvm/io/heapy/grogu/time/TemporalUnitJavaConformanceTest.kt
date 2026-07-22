package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import java.time.Duration as JavaDuration
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.LocalTime as JavaLocalTime
import java.time.ZoneOffset as JavaZoneOffset
import java.time.ZonedDateTime as JavaZonedDateTime
import java.time.temporal.Temporal as JavaTemporal
import java.time.temporal.TemporalUnit as JavaTemporalUnit
import java.time.temporal.UnsupportedTemporalTypeException as JavaUnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalUnitJavaConformanceTest {
    @Test
    fun typeSpecificSupportChecksMatchJavaTimeWithoutCallingTheUnit() {
        val javaDateTime = JavaLocalDateTime.of(2024, 6, 1, 12, 30)
        val dateTime = LocalDateTime.of(2024, 6, 1, 12, 30)
        val temporals = listOf(
            JavaLocalTime.NOON to LocalTime.NOON,
            JavaLocalDate.EPOCH to LocalDate.EPOCH,
            javaDateTime to dateTime,
            JavaZonedDateTime.of(javaDateTime, JavaZoneOffset.UTC) to
                ZonedDateTime.of(dateTime, ZoneOffset.UTC),
        )
        val units = listOf(
            RejectingJavaUnit(isDateBased = true, isTimeBased = false) to
                RejectingKotlinUnit(isDateBased = true, isTimeBased = false),
            RejectingJavaUnit(isDateBased = false, isTimeBased = true) to
                RejectingKotlinUnit(isDateBased = false, isTimeBased = true),
            RejectingJavaUnit(isDateBased = false, isTimeBased = false) to
                RejectingKotlinUnit(isDateBased = false, isTimeBased = false),
        )

        temporals.forEach { (javaTemporal, temporal) ->
            units.forEach { (javaUnit, unit) ->
                assertEquals(
                    javaUnit.isSupportedBy(javaTemporal),
                    unit.isSupportedBy(temporal),
                    "$temporal date=${unit.isDateBased} time=${unit.isTimeBased}",
                )
            }
        }
    }

    private class RejectingJavaUnit(
        private val isDateBased: Boolean,
        private val isTimeBased: Boolean,
    ) : JavaTemporalUnit {
        override fun getDuration(): JavaDuration = JavaDuration.ZERO

        override fun isDurationEstimated(): Boolean = false

        override fun isDateBased(): Boolean = isDateBased

        override fun isTimeBased(): Boolean = isTimeBased

        override fun <R : JavaTemporal> addTo(temporal: R, amount: Long): R =
            throw JavaUnsupportedTemporalTypeException("Rejected")

        override fun between(
            temporal1Inclusive: JavaTemporal,
            temporal2Exclusive: JavaTemporal,
        ): Long = throw JavaUnsupportedTemporalTypeException("Rejected")
    }

    private class RejectingKotlinUnit(
        override val isDateBased: Boolean,
        override val isTimeBased: Boolean,
    ) : TemporalUnit {
        override val duration: Duration = Duration.ZERO
        override val isDurationEstimated: Boolean = false

        override fun <R : Temporal> addTo(temporal: R, amount: Long): R =
            throw UnsupportedTemporalTypeException("Rejected")

        override fun between(
            temporal1Inclusive: Temporal,
            temporal2Exclusive: Temporal,
        ): Long = throw UnsupportedTemporalTypeException("Rejected")
    }
}
