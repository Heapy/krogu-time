package io.heapy.krogu.time.format

import io.heapy.krogu.time.ZoneId
import io.heapy.krogu.time.ZoneOffset
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale as JavaLocale
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
            val kroguFormatter = DateTimeFormatterBuilder()
                .appendOffset(pattern, "ZERO")
                .toFormatter()

            offsets.forEach { totalSeconds ->
                val javaOffset = java.time.ZoneOffset.ofTotalSeconds(totalSeconds)
                val kroguOffset = ZoneOffset.ofTotalSeconds(totalSeconds)
                val javaText = javaFormatter.format(javaOffset)
                val kroguText = kroguFormatter.format(kroguOffset)

                assertEquals(javaText, kroguText, "$pattern at $totalSeconds seconds")
                assertEquals(
                    java.time.ZoneOffset.from(javaFormatter.parse(javaText)).totalSeconds,
                    ZoneOffset.from(kroguFormatter.parse(kroguText)).totalSeconds,
                    "$pattern parsing $javaText",
                )
            }
        }
    }

    @Test
    fun offsetIdAndZoneIdMethodsMatchJavaTime() {
        val javaOffset = java.time.ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val kroguOffset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 15)
        val javaOffsetId = java.time.format.DateTimeFormatterBuilder().appendOffsetId().toFormatter()
        val kroguOffsetId = DateTimeFormatterBuilder().appendOffsetId().toFormatter()
        assertEquals(javaOffsetId.format(javaOffset), kroguOffsetId.format(kroguOffset))

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
        val kroguFormatters = listOf(
            DateTimeFormatterBuilder().appendZoneId().toFormatter(),
            DateTimeFormatterBuilder().appendZoneRegionId().toFormatter(),
            DateTimeFormatterBuilder().appendZoneOrOffsetId().toFormatter(),
        )

        javaFormatters.zip(kroguFormatters).forEach { (javaFormatter, kroguFormatter) ->
            inputs.forEach { input ->
                assertEquals(
                    java.time.ZoneId.from(javaFormatter.parse(input)).id,
                    ZoneId.from(kroguFormatter.parse(input)).id,
                    input,
                )
            }
        }
    }

    @Test
    fun specificAndGenericZoneTextMatchJavaTime() {
        val javaValues = listOf(
            java.time.ZonedDateTime.of(2024, 1, 1, 5, 6, 0, 0, java.time.ZoneId.of("America/New_York")),
            java.time.ZonedDateTime.of(2024, 7, 1, 5, 6, 0, 0, java.time.ZoneId.of("America/New_York")),
            java.time.ZonedDateTime.of(2024, 7, 1, 5, 6, 0, 0, java.time.ZoneId.of("Europe/Paris")),
        )
        val kroguValues = listOf(
            io.heapy.krogu.time.ZonedDateTime.of(2024, 1, 1, 5, 6, 0, 0, ZoneId.of("America/New_York")),
            io.heapy.krogu.time.ZonedDateTime.of(2024, 7, 1, 5, 6, 0, 0, ZoneId.of("America/New_York")),
            io.heapy.krogu.time.ZonedDateTime.of(2024, 7, 1, 5, 6, 0, 0, ZoneId.of("Europe/Paris")),
        )

        listOf("en-US", "fr-FR", "de-DE").forEach { languageTag ->
            listOf(false, true).forEach { generic ->
                listOf(TextStyle.SHORT, TextStyle.FULL).forEach { style ->
                    val javaBuilder = java.time.format.DateTimeFormatterBuilder()
                    val builder = DateTimeFormatterBuilder()
                    if (generic) {
                        javaBuilder.appendGenericZoneText(JavaTextStyle.valueOf(style.name))
                        builder.appendGenericZoneText(style)
                    } else {
                        javaBuilder.appendZoneText(JavaTextStyle.valueOf(style.name))
                        builder.appendZoneText(style)
                    }
                    val javaFormatter = javaBuilder.toFormatter(JavaLocale.forLanguageTag(languageTag))
                    val formatter = builder.toFormatter(io.heapy.krogu.time.Locale.forLanguageTag(languageTag))

                    assertEquals(javaFormatter.toString(), formatter.toString())
                    javaValues.zip(kroguValues).forEach { (javaValue, value) ->
                        val javaText = javaFormatter.format(javaValue)
                        val text = formatter.format(value)
                        assertEquals(javaText, text, "$languageTag $generic $style $value")
                        assertEquals(
                            java.time.ZoneId.from(javaFormatter.parse(javaText)).id,
                            ZoneId.from(formatter.parse(text)).id,
                            "$languageTag $generic $style parsing $text",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun preferredZoneAmbiguityResolutionMatchesJavaTime() {
        val javaPreferred = java.time.ZoneId.of("Asia/Shanghai")
        val preferred = ZoneId.of("Asia/Shanghai")
        val javaTemporal = java.time.ZonedDateTime.of(
            2024,
            1,
            1,
            5,
            6,
            0,
            0,
            javaPreferred,
        )
        val temporal = io.heapy.krogu.time.ZonedDateTime.of(
            2024,
            1,
            1,
            5,
            6,
            0,
            0,
            preferred,
        )

        listOf(false, true).forEach { generic ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            val builder = DateTimeFormatterBuilder()
            if (generic) {
                javaBuilder.appendGenericZoneText(JavaTextStyle.SHORT, setOf(javaPreferred))
                builder.appendGenericZoneText(TextStyle.SHORT, setOf(preferred))
            } else {
                javaBuilder.appendZoneText(JavaTextStyle.SHORT, setOf(javaPreferred))
                builder.appendZoneText(TextStyle.SHORT, setOf(preferred))
            }
            val javaFormatter = javaBuilder.toFormatter(JavaLocale.US)
            val formatter = builder.toFormatter(io.heapy.krogu.time.Locale.US)
            val javaText = javaFormatter.format(javaTemporal)
            val text = formatter.format(temporal)

            assertEquals(javaText, text)
            assertEquals(
                java.time.ZoneId.from(javaFormatter.parse(javaText)).id,
                ZoneId.from(formatter.parse(text)).id,
                "generic=$generic text=$text",
            )
        }
    }
}
