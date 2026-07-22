package io.heapy.grogu.time

import io.heapy.grogu.time.temporal.ChronoField
import io.heapy.grogu.time.temporal.ChronoUnit
import io.heapy.grogu.time.temporal.Temporal
import io.heapy.grogu.time.temporal.TemporalAccessor
import io.heapy.grogu.time.temporal.TemporalField
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.temporal.TemporalUnit
import io.heapy.grogu.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ZoneOffsetTest {
    @Test
    fun createsOffsetsFromSignedComponentsAndTotalSeconds() {
        assertEquals(0, ZoneOffset.UTC.totalSeconds)
        assertEquals(-64_800, ZoneOffset.MIN.totalSeconds)
        assertEquals(64_800, ZoneOffset.MAX.totalSeconds)
        assertEquals(3_600, ZoneOffset.ofHours(1).totalSeconds)
        assertEquals(-5_400, ZoneOffset.ofHoursMinutes(-1, -30).totalSeconds)
        assertEquals(5_415, ZoneOffset.ofHoursMinutesSeconds(1, 30, 15).totalSeconds)
        assertEquals(-5_415, ZoneOffset.ofHoursMinutesSeconds(-1, -30, -15).totalSeconds)
        assertSame(ZoneOffset.UTC, ZoneOffset.ofTotalSeconds(0))
        assertSame(ZoneOffset.ofHours(1), ZoneOffset.ofTotalSeconds(3_600))

        assertFailsWith<DateTimeException> { ZoneOffset.ofHours(19) }
        assertFailsWith<DateTimeException> { ZoneOffset.ofHoursMinutes(1, -1) }
        assertFailsWith<DateTimeException> { ZoneOffset.ofHoursMinutes(-1, 1) }
        assertFailsWith<DateTimeException> { ZoneOffset.ofHoursMinutesSeconds(0, 1, -1) }
        assertFailsWith<DateTimeException> { ZoneOffset.ofHoursMinutes(18, 1) }
        assertFailsWith<DateTimeException> { ZoneOffset.ofTotalSeconds(64_801) }
    }

    @Test
    fun parsesAndFormatsSupportedOffsetIds() {
        val cases = mapOf(
            "Z" to "Z",
            "+1" to "+01:00",
            "-01" to "-01:00",
            "+0130" to "+01:30",
            "-01:30" to "-01:30",
            "+013015" to "+01:30:15",
            "-01:30:15" to "-01:30:15",
            "+18:00" to "+18:00",
        )
        cases.forEach { (input, expected) -> assertEquals(expected, ZoneOffset.of(input).id) }

        listOf("", "z", "UTC", "+", "+1:00", "+010", "+01:3", "+19:00", "+18:00:01")
            .forEach { input -> assertFailsWith<DateTimeException>(input) { ZoneOffset.of(input) } }
    }

    @Test
    fun exposesTheOffsetFieldAndTemporalQuery() {
        val offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)
        ChronoField.entries.forEach { field ->
            assertEquals(field === ChronoField.OFFSET_SECONDS, offset.isSupported(field), field.toString())
        }
        assertEquals(3_723L, offset.getLong(ChronoField.OFFSET_SECONDS))
        assertEquals(3_723, offset.get(ChronoField.OFFSET_SECONDS))
        assertFailsWith<UnsupportedTemporalTypeException> { offset.getLong(ChronoField.HOUR_OF_DAY) }
        assertSame(offset, offset.query(TemporalQueries.offset()))
        assertNull(Instant.EPOCH.query(TemporalQueries.offset()))
        assertSame(offset, ZoneOffset.from(offset))
        assertEquals(offset, ZoneOffset.from(OffsetAccessor(3_723)))
        assertFailsWith<DateTimeException> { ZoneOffset.from(OffsetAccessor(null)) }
    }

    @Test
    fun adjustsOrdersAndHashesOffsets() {
        val offset = ZoneOffset.ofHoursMinutes(5, 30)
        assertEquals(
            OffsetRecordingTemporal(operations = listOf(ChronoField.OFFSET_SECONDS to 19_800L)),
            offset.adjustInto(OffsetRecordingTemporal()),
        )
        assertTrue(ZoneOffset.ofHours(2) < ZoneOffset.ofHours(1))
        assertEquals(offset, ZoneOffset.ofTotalSeconds(19_800))
        assertEquals(offset.hashCode(), ZoneOffset.ofTotalSeconds(19_800).hashCode())
        assertEquals(offset.id, offset.toString())
    }

    private data class OffsetAccessor(
        val totalSeconds: Long?,
    ) : TemporalAccessor {
        override fun isSupported(field: TemporalField): Boolean =
            field === ChronoField.OFFSET_SECONDS && totalSeconds != null

        override fun getLong(field: TemporalField): Long =
            if (field === ChronoField.OFFSET_SECONDS && totalSeconds != null) totalSeconds else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")
    }

    private data class OffsetRecordingTemporal(
        val operations: List<Pair<TemporalField, Long>> = emptyList(),
    ) : Temporal {
        override fun isSupported(field: TemporalField): Boolean = field === ChronoField.OFFSET_SECONDS

        override fun isSupported(unit: TemporalUnit): Boolean = false

        override fun getLong(field: TemporalField): Long =
            throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun with(field: TemporalField, newValue: Long): Temporal =
            if (isSupported(field)) copy(operations = operations + (field to newValue)) else
                throw UnsupportedTemporalTypeException("Unsupported field: $field")

        override fun plus(amountToAdd: Long, unit: TemporalUnit): Temporal =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")

        override fun until(endExclusive: Temporal, unit: TemporalUnit): Long =
            throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
    }
}
