package io.heapy.grogu.time.format

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.OffsetDateTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import io.heapy.grogu.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DateTimeFormatterPatternTest {
    @Test
    fun createsSmartPatternFormattersWithDefaultConfiguration() {
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")

        assertEquals(ResolverStyle.SMART, formatter.resolverStyle)
        assertEquals(DecimalStyle.STANDARD, formatter.decimalStyle)
        assertNull(formatter.chronology)
        assertNull(formatter.zone)
    }

    @Test
    fun formatsAndParsesNumericDateTimePatterns() {
        val formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss.SSS")
        val dateTime = LocalDateTime.of(2024, 3, 1, 5, 6, 7, 8_000_000)

        assertEquals("2024/03/01 05:06:07.008", formatter.format(dateTime))
        assertEquals(dateTime, formatter.parse("2024/03/01 05:06:07.008", LocalDateTime::from))
    }

    @Test
    fun supportsQuotedLiteralsAndEscapedApostrophes() {
        val formatter = DateTimeFormatter.ofPattern(
            "uuuu-MM-dd 'at' HH:mm 'o''clock'",
        )
        val dateTime = LocalDateTime.of(2024, 3, 1, 5, 6)

        assertEquals("2024-03-01 at 05:06 o'clock", formatter.format(dateTime))
        assertEquals(
            dateTime,
            formatter.parse("2024-03-01 at 05:06 o'clock", LocalDateTime::from),
        )
    }

    @Test
    fun parsesAdjacentFieldsReducedYearsAndSmartDates() {
        val compact = DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
        val dateTime = LocalDateTime.of(2024, 3, 1, 5, 6, 7)
        assertEquals("20240301050607", compact.format(dateTime))
        assertEquals(dateTime, compact.parse("20240301050607", LocalDateTime::from))

        assertEquals(
            LocalDate.of(2024, 3, 1),
            DateTimeFormatter.ofPattern("uu-MM-dd")
                .parse("24-03-01", LocalDate::from),
        )
        assertEquals(
            LocalDate.of(2023, 2, 28),
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                .parse("2023-02-30", LocalDate::from),
        )
    }

    @Test
    fun supportsDateOnlyAndTimeOnlyPatterns() {
        val date = LocalDate.of(2024, 3, 1)
        val time = LocalTime.of(5, 6, 7, 800_000_000)

        assertEquals(
            date,
            DateTimeFormatter.ofPattern("u-M-d").parse("2024-3-1", LocalDate::from),
        )
        assertEquals(
            time,
            DateTimeFormatter.ofPattern("H:m:s.S").parse("5:6:7.8", LocalTime::from),
        )
    }

    @Test
    fun rejectsMalformedPatterns() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatter.ofPattern("uuuu-MM-dd '")
        }
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatter.ofPattern("uuuu-ddd")
        }
    }

    @Test
    fun formatsAndParsesOffsetPatterns() {
        val dateTime = OffsetDateTime.of(
            LocalDateTime.of(2024, 3, 1, 5, 6, 7),
            ZoneOffset.ofHoursMinutes(2, 30),
        )
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX")

        assertEquals("2024-03-01T05:06:07+02:30", formatter.format(dateTime))
        assertEquals(
            dateTime,
            formatter.parse("2024-03-01T05:06:07+02:30", OffsetDateTime::from),
        )
        assertEquals("Z", DateTimeFormatter.ofPattern("X").format(ZoneOffset.UTC))
        assertEquals("+00", DateTimeFormatter.ofPattern("x").format(ZoneOffset.UTC))
        assertEquals("+0000", DateTimeFormatter.ofPattern("Z").format(ZoneOffset.UTC))
    }

    @Test
    fun formatsAndParsesRegionZoneIds() {
        val zoned = ZonedDateTime.of(
            LocalDateTime.of(2024, 3, 1, 5, 6),
            ZoneId.of("Europe/Paris"),
        )
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm VV")

        assertEquals("2024-03-01 05:06 Europe/Paris", formatter.format(zoned))
        assertEquals(
            zoned,
            formatter.parse("2024-03-01 05:06 Europe/Paris", ZonedDateTime::from),
        )
        assertFailsWith<IllegalArgumentException> {
            DateTimeFormatter.ofPattern("V")
        }
    }
}
