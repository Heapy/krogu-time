package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.LocalDate
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class ChronoFormattingTest {
    @Test
    fun genericChronologyTypesFormatThroughDateTimeFormatter() {
        val date: ChronoLocalDate = ThaiBuddhistChronology.date(LocalDate.of(2024, 2, 29))
        val dateTime: ChronoLocalDateTime<*> = date.atTime(LocalTime.of(12, 30, 45))
        val zonedDateTime: ChronoZonedDateTime<*> = dateTime.atZone(ZoneId.of("Europe/Paris"))

        val dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        val zonedFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss VV")

        assertEquals(dateFormatter.format(date), date.format(dateFormatter))
        assertEquals(dateTimeFormatter.format(dateTime), dateTime.format(dateTimeFormatter))
        assertEquals(zonedFormatter.format(zonedDateTime), zonedDateTime.format(zonedFormatter))
    }
}
