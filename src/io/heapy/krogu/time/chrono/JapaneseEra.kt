package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.DateTimeException
import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.temporal.ChronoField
import io.heapy.krogu.time.temporal.TemporalField
import io.heapy.krogu.time.temporal.ValueRange
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/** An era in the Japanese Imperial calendar system. */
public class JapaneseEra private constructor(
    override val value: Int,
    private val eraName: String,
    internal val since: LocalDate,
) : Era {
    override fun range(field: TemporalField): ValueRange =
        if (field === ChronoField.ERA) ERA_RANGE else super.range(field)

    override fun toString(): String = eraName

    public companion object {
        /** The Meiji era, with numeric value `-1`. */
        @JvmField
        public val MEIJI: JapaneseEra = JapaneseEra(-1, "Meiji", LocalDate.of(1868, 1, 1))

        /** The Taisho era, with numeric value `0`. */
        @JvmField
        public val TAISHO: JapaneseEra = JapaneseEra(0, "Taisho", LocalDate.of(1912, 7, 30))

        /** The Showa era, with numeric value `1`. */
        @JvmField
        public val SHOWA: JapaneseEra = JapaneseEra(1, "Showa", LocalDate.of(1926, 12, 25))

        /** The Heisei era, with numeric value `2`. */
        @JvmField
        public val HEISEI: JapaneseEra = JapaneseEra(2, "Heisei", LocalDate.of(1989, 1, 8))

        /** The Reiwa era, with numeric value `3`. */
        @JvmField
        public val REIWA: JapaneseEra = JapaneseEra(3, "Reiwa", LocalDate.of(2019, 5, 1))

        private val KNOWN_ERAS: Array<JapaneseEra> =
            arrayOf(MEIJI, TAISHO, SHOWA, HEISEI, REIWA)
        private val ERA_RANGE: ValueRange = ValueRange.of(MEIJI.value.toLong(), REIWA.value.toLong())
        private val MIN_SUPPORTED_DATE: LocalDate = LocalDate.of(1873, 1, 1)

        /** Obtains the Japanese era for [japaneseEra]. */
        @JvmStatic
        public fun of(japaneseEra: Int): JapaneseEra = KNOWN_ERAS.getOrNull(japaneseEra + 1)
            ?: throw DateTimeException("Invalid era: $japaneseEra")

        /** Obtains the Japanese era with the exact title-case [japaneseEra] name. */
        @JvmStatic
        public fun valueOf(japaneseEra: String): JapaneseEra =
            KNOWN_ERAS.firstOrNull { it.eraName == japaneseEra }
                ?: throw IllegalArgumentException("japaneseEra is invalid")

        /** Returns a defensive copy of the known Japanese eras. */
        @JvmStatic
        public fun values(): Array<JapaneseEra> = KNOWN_ERAS.copyOf()

        internal fun from(date: LocalDate): JapaneseEra {
            if (date < MIN_SUPPORTED_DATE) {
                throw DateTimeException("JapaneseDate before Meiji 6 are not supported")
            }
            return KNOWN_ERAS.last { date >= it.since }
        }

        internal fun next(era: JapaneseEra): JapaneseEra? =
            KNOWN_ERAS.getOrNull(era.value + 2)
    }
}
