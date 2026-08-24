package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.TemporalQueries
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeFormatterZoneIdParseJavaConformanceTest {
    private val texts = listOf(
        // bare offsets: minutes are required, so a lone hour is a failure
        "+01", "-01", "+01:30", "-08:00", "Z", "+1:30", "+01:60", "+01:30:45",
        // prefixed offsets: the prefix stands alone when the offset is malformed
        "UTC", "UT", "GMT",
        "UTC-01", "UTC-0", "UTC+", "UTC-1:30", "UTC-01:3", "UTC-01:60",
        "UTC-01:WW", "UTC-01:30", "UTC-01:30:4", "UTC-01:30:45", "UTC-01:30:WW",
        "GMT+01:30", "UT+01:30", "GMT-03:WW", "UT-02:WW",
        // '0' and 'Z' never start an offset, and GMT0 is an id of its own
        "GMT0", "GMT0X", "UTC0", "UT0", "UTCZ", "GMTX", "GMT+0", "GMT-0",
        // region ids must keep resolving, including the lookalikes
        "UCT", "Universal", "Zulu", "Greenwich", "US/Eastern", "Europe/Paris",
        "Etc/GMT+5",
        // trailing text is left unparsed rather than swallowed
        "UTC-01:30XXX", "Europe/ParisXXX",
    )

    // java.time parses a zone id structurally: prefix, then an offset with
    // required two-digit hours and minutes. ZoneId.of is more permissive, so
    // a longest-match over it consumes text that Java leaves alone.
    @Test
    fun zoneIdParsingMatchesJavaTime() {
        assertEquals(emptyList(), mismatches(caseSensitive = true))
    }

    @Test
    fun caseInsensitiveZoneIdParsingMatchesJavaTime() {
        assertEquals(emptyList(), mismatches(caseSensitive = false))
    }

    private fun mismatches(caseSensitive: Boolean): List<String> {
        val inputs = if (caseSensitive) texts else texts.map { it.lowercase() }
        return inputs.mapNotNull { text ->
            val javaBuilder = java.time.format.DateTimeFormatterBuilder()
            if (!caseSensitive) javaBuilder.parseCaseInsensitive()
            val javaPosition = java.text.ParsePosition(0)
            val javaParsed = javaBuilder.appendZoneId().toFormatter()
                .parseUnresolved(text, javaPosition)

            val kroguBuilder = DateTimeFormatterBuilder()
            if (!caseSensitive) kroguBuilder.parseCaseInsensitive()
            val kroguPosition = ParsePosition(0)
            val kroguParsed = kroguBuilder.appendZoneId().toFormatter()
                .parseUnresolved(text, kroguPosition)

            val expected = listOf(
                javaPosition.index,
                javaPosition.errorIndex,
                javaParsed?.query(java.time.temporal.TemporalQueries.zoneId())?.toString(),
            )
            val actual = listOf(
                kroguPosition.index,
                kroguPosition.errorIndex,
                kroguParsed?.query(TemporalQueries.zoneId())?.toString(),
            )
            if (expected == actual) null else "$text: Java=$expected, Kotlin=$actual"
        }
    }
}
