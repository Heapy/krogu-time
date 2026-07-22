package io.heapy.grogu.time

import kotlin.test.Test
import kotlin.test.assertEquals

class InstantFactoryTest {
    @Test
    fun createsLocalAndOffsetValuesAcrossAZoneTransition() {
        val paris = ZoneId.of("Europe/Paris")
        val beforeTransition = Instant.parse("2024-03-31T00:30:00Z")
        val afterTransition = Instant.parse("2024-03-31T01:30:00Z")

        assertEquals(
            LocalTime.of(1, 30),
            LocalTime.ofInstant(beforeTransition, paris),
        )
        assertEquals(
            LocalDateTime.of(2024, 3, 31, 1, 30),
            LocalDateTime.ofInstant(beforeTransition, paris),
        )
        assertEquals(
            OffsetTime.of(LocalTime.of(1, 30), ZoneOffset.ofHours(1)),
            OffsetTime.ofInstant(beforeTransition, paris),
        )
        assertEquals(
            OffsetDateTime.of(
                LocalDateTime.of(2024, 3, 31, 1, 30),
                ZoneOffset.ofHours(1),
            ),
            OffsetDateTime.ofInstant(beforeTransition, paris),
        )

        assertEquals(
            LocalTime.of(3, 30),
            LocalTime.ofInstant(afterTransition, paris),
        )
        assertEquals(
            LocalDateTime.of(2024, 3, 31, 3, 30),
            LocalDateTime.ofInstant(afterTransition, paris),
        )
        assertEquals(
            OffsetTime.of(LocalTime.of(3, 30), ZoneOffset.ofHours(2)),
            OffsetTime.ofInstant(afterTransition, paris),
        )
        assertEquals(
            OffsetDateTime.of(
                LocalDateTime.of(2024, 3, 31, 3, 30),
                ZoneOffset.ofHours(2),
            ),
            OffsetDateTime.ofInstant(afterTransition, paris),
        )
    }

    @Test
    fun floorsNegativeEpochSecondsIntoThePreviousDay() {
        val instant = Instant.ofEpochSecond(-1, 123)
        val expectedTime = LocalTime.of(23, 59, 59, 123)

        assertEquals(expectedTime, LocalTime.ofInstant(instant, ZoneOffset.UTC))
        assertEquals(
            LocalDateTime.of(LocalDate.of(1969, 12, 31), expectedTime),
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
        )
        assertEquals(
            OffsetTime.of(expectedTime, ZoneOffset.UTC),
            OffsetTime.ofInstant(instant, ZoneOffset.UTC),
        )
    }
}
