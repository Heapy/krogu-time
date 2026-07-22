package io.heapy.grogu.time.zone

import io.heapy.grogu.time.DayOfWeek
import io.heapy.grogu.time.LocalDateTime
import io.heapy.grogu.time.LocalTime
import io.heapy.grogu.time.Month
import io.heapy.grogu.time.ZoneOffset

/** Provides the IANA time-zone database bundled in OpenJDK's compact binary format. */
internal class TzdbZoneRulesProvider : ZoneRulesProvider() {
    private val database: TzdbDatabase = try {
        readDatabase(BinaryReader(decodeBase64(BUNDLED_TZDB_BASE64)))
    } catch (exception: RuntimeException) {
        throw ZoneRulesException("Unable to load bundled TZDB time-zone rules", exception)
    }

    override fun provideZoneIds(): Set<String> = database.regionIds

    override fun provideRules(zoneId: String, forCaching: Boolean): ZoneRules =
        database.rulesByRegion[zoneId]
            ?: throw ZoneRulesException("Unknown time-zone ID: $zoneId")

    override fun provideVersions(zoneId: String): Map<String, ZoneRules> =
        mapOf(database.versionId to provideRules(zoneId, false))

    override fun toString(): String = "TZDB[${database.versionId}]"

    private fun readDatabase(reader: BinaryReader): TzdbDatabase {
        require(reader.readUnsignedByte() == FILE_FORMAT_VERSION) { "File format not recognised" }
        require(reader.readUtf() == GROUP_ID) { "File format not recognised" }

        val versionCount = reader.readUnsignedShort()
        require(versionCount > 0) { "TZDB contains no versions" }
        var versionId = ""
        repeat(versionCount) {
            versionId = reader.readUtf()
        }

        val regionIds = List(reader.readUnsignedShort()) { reader.readUtf() }
        val serializedRules = List(reader.readUnsignedShort()) {
            reader.readBytes(reader.readUnsignedShort())
        }
        val rules = serializedRules.map(::readZoneRules)

        var rulesByRegion: Map<String, ZoneRules> = emptyMap()
        repeat(versionCount) {
            rulesByRegion = buildMap {
                repeat(reader.readUnsignedShort()) {
                    val regionIndex = reader.readUnsignedShort()
                    val ruleIndex = reader.readUnsignedShort()
                    require(regionIndex in regionIds.indices) { "Invalid TZDB region index" }
                    require(ruleIndex in rules.indices) { "Invalid TZDB rule index" }
                    put(regionIds[regionIndex], rules[ruleIndex])
                }
            }
        }
        // OpenJDK appends alias metadata after the latest version mapping. The
        // region table already contains those aliases, matching its provider.
        require(rulesByRegion.keys.containsAll(regionIds)) { "TZDB version omits region mappings" }
        return TzdbDatabase(versionId, regionIds.toSet(), rulesByRegion)
    }

    private fun readZoneRules(data: ByteArray): ZoneRules {
        val reader = BinaryReader(data)
        require(reader.readUnsignedByte() == ZONE_RULES_TYPE) { "Unknown serialized TZDB rule type" }

        val standardTransitionCount = reader.readInt()
        require(standardTransitionCount in 0..MAX_TRANSITIONS) { "Too many standard transitions" }
        val standardTransitions = List(standardTransitionCount) { reader.readEpochSecond() }
        val standardOffsets = List(standardTransitionCount + 1) { reader.readOffset() }

        val savingsTransitionCount = reader.readInt()
        require(savingsTransitionCount in 0..MAX_TRANSITIONS) { "Too many savings transitions" }
        val savingsTransitions = List(savingsTransitionCount) { reader.readEpochSecond() }
        val wallOffsets = List(savingsTransitionCount + 1) { reader.readOffset() }

        val lastRuleCount = reader.readUnsignedByte()
        require(lastRuleCount <= MAX_LAST_RULES) { "Too many transition rules" }
        val lastRules = List(lastRuleCount) { reader.readTransitionRule() }
        require(reader.remaining == 0) { "Trailing bytes in serialized TZDB rule" }

        return ZoneRules.of(
            baseStandardOffset = standardOffsets.first(),
            baseWallOffset = wallOffsets.first(),
            standardOffsetTransitionList = transitions(standardTransitions, standardOffsets),
            transitionList = transitions(savingsTransitions, wallOffsets),
            lastRules = lastRules,
        )
    }

    private fun transitions(
        epochSeconds: List<Long>,
        offsets: List<ZoneOffset>,
    ): List<ZoneOffsetTransition> = epochSeconds.indices.map { index ->
        ZoneOffsetTransition(
            LocalDateTime.ofEpochSecond(epochSeconds[index], 0, offsets[index]),
            offsets[index],
            offsets[index + 1],
        )
    }

    private data class TzdbDatabase(
        val versionId: String,
        val regionIds: Set<String>,
        val rulesByRegion: Map<String, ZoneRules>,
    )

    private companion object {
        const val FILE_FORMAT_VERSION: Int = 1
        const val ZONE_RULES_TYPE: Int = 1
        const val MAX_TRANSITIONS: Int = 1_024
        const val MAX_LAST_RULES: Int = 16
        const val GROUP_ID: String = "TZDB"
    }
}

private class BinaryReader(
    private val data: ByteArray,
) {
    private var position: Int = 0

    val remaining: Int
        get() = data.size - position

    fun readUnsignedByte(): Int {
        requireRemaining(1)
        return data[position++].toInt() and 0xff
    }

    fun readByte(): Int {
        requireRemaining(1)
        return data[position++].toInt()
    }

    fun readUnsignedShort(): Int =
        readUnsignedByte() shl 8 or readUnsignedByte()

    fun readInt(): Int =
        readUnsignedByte() shl 24 or
            (readUnsignedByte() shl 16) or
            (readUnsignedByte() shl 8) or
            readUnsignedByte()

    fun readLong(): Long {
        var value = 0L
        repeat(Long.SIZE_BYTES) {
            value = value shl 8 or readUnsignedByte().toLong()
        }
        return value
    }

    fun readUtf(): String = readBytes(readUnsignedShort()).decodeToString()

    fun readBytes(size: Int): ByteArray {
        require(size >= 0) { "Negative binary data length" }
        requireRemaining(size)
        val result = data.copyOfRange(position, position + size)
        position += size
        return result
    }

    fun readEpochSecond(): Long {
        val highByte = readUnsignedByte()
        if (highByte == 255) return readLong()
        val stored = highByte shl 16 or (readUnsignedByte() shl 8) or readUnsignedByte()
        return stored * 900L - 4_575_744_000L
    }

    fun readOffset(): ZoneOffset {
        val compressed = readByte()
        return ZoneOffset.ofTotalSeconds(
            if (compressed == 127) readInt() else compressed * 900,
        )
    }

    fun readTransitionRule(): ZoneOffsetTransitionRule {
        val packed = readInt()
        val month = Month.of(packed ushr 28)
        val dayOfMonthIndicator = (packed and (63 shl 22) ushr 22) - 32
        val dayOfWeekValue = packed and (7 shl 19) ushr 19
        val dayOfWeek = if (dayOfWeekValue == 0) null else DayOfWeek.of(dayOfWeekValue)
        val timeByte = packed and (31 shl 14) ushr 14
        val timeDefinition = ZoneOffsetTransitionRule.TimeDefinition.entries[
            packed and (3 shl 12) ushr 12
        ]
        val standardOffsetByte = packed and (255 shl 4) ushr 4
        val beforeByte = packed and (3 shl 2) ushr 2
        val afterByte = packed and 3
        val time = if (timeByte == 31) {
            LocalTime.ofSecondOfDay(readInt().toLong())
        } else {
            LocalTime.of(timeByte % 24, 0)
        }
        val standardOffset = ZoneOffset.ofTotalSeconds(
            if (standardOffsetByte == 255) readInt() else (standardOffsetByte - 128) * 900,
        )
        val offsetBefore = ZoneOffset.ofTotalSeconds(
            if (beforeByte == 3) readInt() else standardOffset.totalSeconds + beforeByte * 1_800,
        )
        val offsetAfter = ZoneOffset.ofTotalSeconds(
            if (afterByte == 3) readInt() else standardOffset.totalSeconds + afterByte * 1_800,
        )
        return ZoneOffsetTransitionRule.of(
            month,
            dayOfMonthIndicator,
            dayOfWeek,
            time,
            timeByte == 24,
            timeDefinition,
            standardOffset,
            offsetBefore,
            offsetAfter,
        )
    }

    private fun requireRemaining(size: Int) {
        require(size <= remaining) { "Unexpected end of binary TZDB data" }
    }
}

private fun decodeBase64(chunks: Array<String>): ByteArray {
    val encoded = chunks.joinToString(separator = "").filterNot(Char::isWhitespace)
    val padding = encoded.takeLastWhile { it == '=' }.length
    val result = ByteArray(encoded.length / 4 * 3 - padding)
    var accumulator = 0
    var bitCount = 0
    var resultIndex = 0
    for (character in encoded) {
        if (character == '=') break
        val value = when (character) {
            in 'A'..'Z' -> character - 'A'
            in 'a'..'z' -> character - 'a' + 26
            in '0'..'9' -> character - '0' + 52
            '+' -> 62
            '/' -> 63
            else -> error("Invalid Base64 character")
        }
        accumulator = accumulator shl 6 or value
        bitCount += 6
        if (bitCount >= 8) {
            bitCount -= 8
            if (resultIndex < result.size) {
                result[resultIndex++] = (accumulator shr bitCount).toByte()
            }
        }
    }
    require(resultIndex == result.size) { "Invalid Base64 data length" }
    return result
}
