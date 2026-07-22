package io.heapy.grogu.time

import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.zone.ZoneRules
import java.time.Instant as JavaInstant
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId as JavaZoneId
import java.time.ZoneOffset as JavaZoneOffset
import java.time.zone.ZoneRules as JavaZoneRules
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoneIdJavaConformanceTest {
    @Test
    fun fixedOffsetZoneIdsMatchJavaTime() {
        assertEquals(JavaZoneId.SHORT_IDS, ZoneId.SHORT_IDS)
        assertEquals(
            JavaZoneId.of("EST", JavaZoneId.SHORT_IDS).toString(),
            ZoneId.of("EST", ZoneId.SHORT_IDS).toString(),
        )
        val ids = listOf(
            "",
            "A",
            "Z",
            "+02",
            "+02:30:15",
            "UTC",
            "UTC+00:00",
            "UTC+01",
            "GMT",
            "GMT-03:30",
            "UT",
            "UT+18:00",
            "UT+18:00:01",
        )
        ids.forEach { id ->
            assertSameOutcome(
                javaOperation = {
                    val zone = JavaZoneId.of(id)
                    listOf(
                        zone.id,
                        zone.normalized().id,
                        zone.rules.isFixedOffset.toString(),
                        zone.hashCode().toString(),
                        zone.toString(),
                    )
                },
                kotlinOperation = {
                    val zone = ZoneId.of(id)
                    listOf(
                        zone.id,
                        zone.normalized().id,
                        zone.rules.isFixedOffset.toString(),
                        zone.hashCode().toString(),
                        zone.toString(),
                    )
                },
                context = id,
            )
        }

        listOf("", "UTC", "GMT", "UT", "utc", "X").forEach { prefix ->
            assertSameOutcome(
                javaOperation = {
                    JavaZoneId.ofOffset(prefix, JavaZoneOffset.ofHoursMinutes(2, 30)).toString()
                },
                kotlinOperation = {
                    ZoneId.ofOffset(prefix, ZoneOffset.ofHoursMinutes(2, 30)).toString()
                },
                context = prefix,
            )
        }
    }

    @Test
    fun fixedZoneRulesMatchJavaTime() {
        val instant = Instant.ofEpochSecond(1_709_210_096, 123_456_789)
        val localDateTime = LocalDateTime.of(2024, 2, 29, 13, 14, 15, 123_456_789)
        listOf(ZoneOffset.MIN, ZoneOffset.UTC, ZoneOffset.ofHoursMinutes(2, 30), ZoneOffset.MAX)
            .forEach { offset ->
                val javaRules = JavaZoneRules.of(JavaZoneOffset.ofTotalSeconds(offset.totalSeconds))
                val rules = ZoneRules.of(offset)
                val javaInstant = JavaInstant.ofEpochSecond(instant.epochSecond, instant.nano.toLong())
                val javaLocalDateTime = JavaLocalDateTime.of(
                    localDateTime.year,
                    localDateTime.monthValue,
                    localDateTime.dayOfMonth,
                    localDateTime.hour,
                    localDateTime.minute,
                    localDateTime.second,
                    localDateTime.nano,
                )
                assertEquals(javaRules.isFixedOffset, rules.isFixedOffset)
                assertEquals(javaRules.getOffset(javaInstant).toString(), rules.getOffset(instant).toString())
                assertEquals(
                    javaRules.getValidOffsets(javaLocalDateTime).map(JavaZoneOffset::toString),
                    rules.getValidOffsets(localDateTime).map(ZoneOffset::toString),
                )
                assertEquals(
                    javaRules.getDaylightSavings(javaInstant).toString(),
                    rules.getDaylightSavings(instant).toString(),
                )
                assertEquals(
                    javaRules.getTransition(javaLocalDateTime)?.toString(),
                    rules.getTransition(localDateTime)?.toString(),
                )
                assertEquals(
                    javaRules.nextTransition(javaInstant)?.toString(),
                    rules.nextTransition(instant)?.toString(),
                )
                assertEquals(
                    javaRules.previousTransition(javaInstant)?.toString(),
                    rules.previousTransition(instant)?.toString(),
                )
                assertEquals(javaRules.transitions.map(Any::toString), rules.getTransitions().map(Any::toString))
                assertEquals(
                    javaRules.transitionRules.map(Any::toString),
                    rules.getTransitionRules().map(Any::toString),
                )
                assertEquals(javaRules.hashCode(), rules.hashCode())
                assertEquals(javaRules.toString(), rules.toString())
            }
    }

    @Test
    fun regionIdValidationMatchesJavaTimeBeforeProviderLookup() {
        listOf("1/Bad", "/Bad", "_Bad", "A/B", "Unknown_Test/Zone").forEach { id ->
            assertSameOutcome(
                javaOperation = { JavaZoneId.of(id) },
                kotlinOperation = { ZoneId.of(id) },
                context = id,
            )
        }
    }

    @Test
    fun localizedDisplayNamesMatchJavaTime() {
        val zones = listOf("Europe/Paris", "America/New_York", "Asia/Tokyo", "+02:30", "GMT+02:30")
        val locales = listOf("en-US", "en-GB", "fr-FR", "de-DE", "ja-JP", "ar-SA")

        zones.forEach { zoneId ->
            locales.forEach { tag ->
                TextStyle.entries.forEach { style ->
                    assertEquals(
                        JavaZoneId.of(zoneId).getDisplayName(
                            java.time.format.TextStyle.valueOf(style.name),
                            java.util.Locale.forLanguageTag(tag),
                        ),
                        ZoneId.of(zoneId).getDisplayName(
                            style,
                            Locale.forLanguageTag(tag),
                        ),
                        "$zoneId $tag $style",
                    )
                }
            }
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
