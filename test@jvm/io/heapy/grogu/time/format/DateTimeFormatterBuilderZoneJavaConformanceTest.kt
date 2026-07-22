package io.heapy.grogu.time.format

import io.heapy.grogu.time.ZoneId
import io.heapy.grogu.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterBuilderZoneJavaConformanceTest {
    @Test
    fun everyCustomOffsetPatternMatchesJavaTime() {
        val patterns = listOf(
            "+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM",
            "+HHMMss", "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS", "+HHmmss", "+HH:mm:ss",
            "+H", "+Hmm", "+H:mm", "+HMM", "+H:MM",
            "+HMMss", "+H:MM:ss", "+HMMSS", "+H:MM:SS", "+Hmmss", "+H:mm:ss",
        )
        val offsets = listOf(
            0,
            3_600,
            3_720,
            3_723,
            -(9 * 3_600 + 30 * 60 + 15),
        )

        patterns.forEach { pattern ->
            val javaFormatter = java.time.format.DateTimeFormatterBuilder()
                .appendOffset(pattern, "ZERO")
                .toFormatter()
            val groguFormatter = DateTimeFormatterBuilder()
                .appendOffset(pattern, "ZERO")
                .toFormatter()

            offsets.forEach { totalSeconds ->
                val javaOffset = java.time.ZoneOffset.ofTotalSeconds(totalSeconds)
                val groguOffset = ZoneOffset.ofTotalSeconds(totalSeconds)
                val javaText = javaFormatter.format(javaOffset)
                val groguText = groguFormatter.format(groguOffset)

                assertEquals(javaText, groguText, "$pattern at $totalSeconds seconds")
                assertEquals(
                    java.time.ZoneOffset.from(javaFormatter.parse(javaText)).totalSeconds,
                    ZoneOffset.from(groguFormatter.parse(groguText)).totalSeconds,
                    "$pattern parsing $javaText",
                )
            }
        }
    }

    @Test
    fun offsetIdAndZoneIdMethodsMatchJavaTime() {
        val javaOffset = java.time.ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val groguOffset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val javaOffsetId = java.time.format.DateTimeFormatterBuilder().appendOffsetId().toFormatter()
        val groguOffsetId = DateTimeFormatterBuilder().appendOffsetId().toFormatter()
        assertEquals(javaOffsetId.format(javaOffset), groguOffsetId.format(groguOffset))

        val inputs = listOf(
            "Europe/Paris",
            "+02:30",
            "UTC",
            "UTC+01:30",
            "GMT-03:00",
            "UT",
            "Z",
        )
        val javaFormatters = listOf(
            java.time.format.DateTimeFormatterBuilder().appendZoneId().toFormatter(),
            java.time.format.DateTimeFormatterBuilder().appendZoneRegionId().toFormatter(),
            java.time.format.DateTimeFormatterBuilder().appendZoneOrOffsetId().toFormatter(),
        )
        val groguFormatters = listOf(
            DateTimeFormatterBuilder().appendZoneId().toFormatter(),
            DateTimeFormatterBuilder().appendZoneRegionId().toFormatter(),
            DateTimeFormatterBuilder().appendZoneOrOffsetId().toFormatter(),
        )

        javaFormatters.zip(groguFormatters).forEach { (javaFormatter, groguFormatter) ->
            inputs.forEach { input ->
                assertEquals(
                    java.time.ZoneId.from(javaFormatter.parse(input)).id,
                    ZoneId.from(groguFormatter.parse(input)).id,
                    input,
                )
            }
        }
    }
}
