package io.heapy.krogu.time

import io.heapy.krogu.time.format.TextStyle
import io.heapy.krogu.time.format.formatLocaleZoneText
import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalQueries
import io.heapy.krogu.time.zone.ZoneRules
import io.heapy.krogu.time.zone.ZoneRulesException
import io.heapy.krogu.time.zone.ZoneRulesProvider

/** An identifier for a time-zone. */
public abstract class ZoneId {
    /** The unique textual identifier of this zone. */
    public abstract val id: String

    /** The offset rules associated with this zone. */
    public abstract val rules: ZoneRules

    /** Returns a fixed offset when these rules never change. */
    public fun normalized(): ZoneId =
        if (rules.isFixedOffset) rules.getOffset(Instant.EPOCH) else this

    /** Returns this zone's localized display name, falling back to [id]. */
    public fun getDisplayName(style: TextStyle, locale: Locale): String =
        if (style == TextStyle.NARROW || hasOffsetBasedId()) {
            id
        } else {
            formatLocaleZoneText(
                languageTag = locale.toLanguageTag(),
                zoneId = id,
                epochSecond = null,
                style = style,
                generic = false,
            ) ?: id
        }

    private fun hasOffsetBasedId(): Boolean =
        this is ZoneOffset ||
            id.startsWith("UTC+") || id.startsWith("UTC-") ||
            id.startsWith("GMT+") || id.startsWith("GMT-") ||
            id.startsWith("UT+") || id.startsWith("UT-")

    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneId && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id

    public companion object {
        /** The legacy short-ID mappings defined by Java's `ZoneId`. */
        public val SHORT_IDS: Map<String, String> = mapOf(
            "ACT" to "Australia/Darwin",
            "AET" to "Australia/Sydney",
            "AGT" to "America/Argentina/Buenos_Aires",
            "ART" to "Africa/Cairo",
            "AST" to "America/Anchorage",
            "BET" to "America/Sao_Paulo",
            "BST" to "Asia/Dhaka",
            "CAT" to "Africa/Harare",
            "CNT" to "America/St_Johns",
            "CST" to "America/Chicago",
            "CTT" to "Asia/Shanghai",
            "EAT" to "Africa/Addis_Ababa",
            "ECT" to "Europe/Paris",
            "IET" to "America/Indiana/Indianapolis",
            "IST" to "Asia/Kolkata",
            "JST" to "Asia/Tokyo",
            "MIT" to "Pacific/Apia",
            "NET" to "Asia/Yerevan",
            "NST" to "Pacific/Auckland",
            "PLT" to "Asia/Karachi",
            "PNT" to "America/Phoenix",
            "PRT" to "America/Puerto_Rico",
            "PST" to "America/Los_Angeles",
            "SST" to "Pacific/Guadalcanal",
            "VST" to "Asia/Ho_Chi_Minh",
            "EST" to "-05:00",
            "MST" to "-07:00",
            "HST" to "-10:00",
        )

        /** Obtains the system default time-zone. */
        public fun systemDefault(): ZoneId = of(systemDefaultZoneId(), SHORT_IDS)

        /** Obtains a fixed-offset or registered region zone ID. */
        public fun of(zoneId: String): ZoneId {
            if (zoneId.length <= 1 || zoneId[0] == '+' || zoneId[0] == '-') {
                return ZoneOffset.of(zoneId)
            }
            return when {
                zoneId.startsWith("UTC") || zoneId.startsWith("GMT") ->
                    ofWithPrefix(zoneId, 3)
                zoneId.startsWith("UT") -> ofWithPrefix(zoneId, 2)
                else -> RegionZoneId.of(zoneId)
            }
        }

        /** Obtains a zone ID after applying [aliasMap]. */
        public fun of(zoneId: String, aliasMap: Map<String, String>): ZoneId =
            of(aliasMap[zoneId] ?: zoneId)

        /** Creates a zone ID by prefixing a fixed [offset]. */
        public fun ofOffset(prefix: String, offset: ZoneOffset): ZoneId {
            if (prefix != "" && prefix != "GMT" && prefix != "UTC" && prefix != "UT") {
                throw IllegalArgumentException("Prefix should be GMT, UTC or UT, is: $prefix")
            }
            if (prefix.isEmpty()) return offset
            val id = if (offset.totalSeconds == 0) prefix else prefix + offset.id
            return FixedZoneId(id, offset)
        }

        /** Obtains a zone ID from a temporal accessor. */
        public fun from(temporal: TemporalAccessor): ZoneId =
            temporal.query(TemporalQueries.zone()) ?: throw DateTimeException(
                "Unable to obtain ZoneId from TemporalAccessor: $temporal",
            )

        /** Returns every region ID registered with a zone-rules provider. */
        public fun getAvailableZoneIds(): Set<String> = ZoneRulesProvider.getAvailableZoneIds()

        private fun ofWithPrefix(zoneId: String, prefixLength: Int): ZoneId {
            if (zoneId.length == prefixLength) {
                return ofOffset(zoneId, ZoneOffset.UTC)
            }
            if (zoneId[prefixLength] != '+' && zoneId[prefixLength] != '-') {
                return RegionZoneId.of(zoneId)
            }
            val offset = ZoneOffset.of(zoneId.substring(prefixLength))
            return ofOffset(zoneId.substring(0, prefixLength), offset)
        }
    }
}

private class FixedZoneId(
    override val id: String,
    offset: ZoneOffset,
) : ZoneId() {
    override val rules: ZoneRules = ZoneRules.of(offset)
}

private class RegionZoneId private constructor(
    override val id: String,
    private val cachedRules: ZoneRules?,
) : ZoneId() {
    override val rules: ZoneRules
        get() = cachedRules ?: ZoneRulesProvider.getRules(id, false)
            ?: throw ZoneRulesException("Provider returned no rules for time-zone ID: $id")

    companion object {
        fun of(zoneId: String): RegionZoneId {
            validateName(zoneId)
            val rules = ZoneRulesProvider.getRules(zoneId, true)
            return RegionZoneId(zoneId, rules)
        }

        private fun validateName(zoneId: String) {
            if (zoneId.length < 2) throw invalidId(zoneId)
            zoneId.forEachIndexed { index, character ->
                val valid = character in 'a'..'z' ||
                    character in 'A'..'Z' ||
                    index != 0 && (
                        character == '/' ||
                            character in '0'..'9' ||
                            character == '~' ||
                            character == '.' ||
                            character == '_' ||
                            character == '+' ||
                            character == '-'
                    )
                if (!valid) throw invalidId(zoneId)
            }
        }

        private fun invalidId(zoneId: String): DateTimeException = DateTimeException(
            "Invalid ID for region-based ZoneId, invalid format: $zoneId",
        )
    }
}
