package io.heapy.grogu.time

import io.heapy.grogu.time.format.TextStyle
import io.heapy.grogu.time.temporal.TemporalQueries
import io.heapy.grogu.time.zone.ZoneRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ZoneIdTest {
    @Test
    fun createsNormalizesAndValidatesFixedOffsetZoneIds() {
        assertSame(ZoneOffset.UTC, ZoneId.of("Z"))
        assertSame(ZoneOffset.UTC, ZoneId.of("+00:00"))
        assertEquals(ZoneOffset.ofHours(2), ZoneId.of("+02"))
        assertEquals(ZoneOffset.ofHours(-5), ZoneId.of("EST", ZoneId.SHORT_IDS))

        val utc = ZoneId.of("UTC")
        assertEquals("UTC", utc.id)
        assertEquals(ZoneOffset.UTC, utc.normalized())
        assertTrue(utc.rules.isFixedOffset)

        val prefixed = ZoneId.ofOffset("GMT", ZoneOffset.ofHoursMinutes(5, 30))
        assertEquals("GMT+05:30", prefixed.id)
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), prefixed.normalized())
        assertEquals(prefixed, ZoneId.of("GMT+05:30"))
        assertSame(ZoneOffset.ofHours(2), ZoneId.ofOffset("", ZoneOffset.ofHours(2)))

        assertFailsWith<IllegalArgumentException> {
            ZoneId.ofOffset("utc", ZoneOffset.UTC)
        }
        assertEquals("Europe/Paris", ZoneId.of("Europe/Paris").id)
    }

    @Test
    fun fixedRulesAlwaysReturnTheirSingleOffset() {
        val offset = ZoneOffset.ofHoursMinutes(2, 30)
        val rules = ZoneRules.of(offset)
        val instant = Instant.ofEpochSecond(1_709_210_096, 123_456_789)
        val localDateTime = LocalDateTime.of(2024, 2, 29, 13, 14)

        assertTrue(rules.isFixedOffset)
        assertEquals(offset, rules.getOffset(instant))
        assertEquals(offset, rules.getOffset(localDateTime))
        assertEquals(listOf(offset), rules.getValidOffsets(localDateTime))
        assertNull(rules.getTransition(localDateTime))
        assertEquals(offset, rules.getStandardOffset(instant))
        assertEquals(Duration.ZERO, rules.getDaylightSavings(instant))
        assertFalse(rules.isDaylightSavings(instant))
        assertTrue(rules.isValidOffset(localDateTime, offset))
        assertFalse(rules.isValidOffset(localDateTime, ZoneOffset.UTC))
        assertNull(rules.nextTransition(instant))
        assertNull(rules.previousTransition(instant))
        assertEquals(emptyList(), rules.getTransitions())
        assertEquals(emptyList(), rules.getTransitionRules())
        assertEquals("ZoneRules[currentStandardOffset=+02:30]", rules.toString())
        assertEquals(ZoneRules.of(offset), rules)
        assertEquals(1, rules.hashCode())
    }

    @Test
    fun zoneQueriesDistinguishExplicitZoneIdsFromOffsetFallbacks() {
        val value = OffsetDateTime.of(2024, 2, 29, 13, 14, 0, 0, ZoneOffset.ofHours(2))
        assertNull(value.query(TemporalQueries.zoneId()))
        assertEquals(value.offset, value.query(TemporalQueries.zone()))
        assertEquals(value.offset, ZoneId.from(value))
        assertNull(value.offset.query(TemporalQueries.zoneId()))
        assertEquals(value.offset, value.offset.query(TemporalQueries.zone()))
    }

    @Test
    fun obtainsLocalizedDisplayNamesWithIdFallbacks() {
        val paris = ZoneId.of("Europe/Paris")
        val full = paris.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val short = paris.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

        assertTrue(full.isNotEmpty())
        assertTrue(short.isNotEmpty())
        assertEquals("Europe/Paris", paris.getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
        assertNotEquals(
            "Europe/Paris",
            paris.getDisplayName(TextStyle.NARROW_STANDALONE, Locale.ENGLISH),
        )
        assertEquals(
            "+02:30",
            ZoneOffset.ofHoursMinutes(2, 30).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
        )
        assertEquals(
            "GMT+02:30",
            ZoneId.of("GMT+02:30").getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
        )
    }

    @Test
    fun displayNamesUseTheRequestedLocale() {
        val paris = ZoneId.of("Europe/Paris")

        assertNotEquals(
            paris.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            paris.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("fr-FR")),
        )
    }
}
