package io.heapy.grogu.time.chrono

import io.heapy.grogu.time.Locale
import io.heapy.grogu.time.format.TextStyle
import java.time.chrono.Era as JavaEra
import java.time.chrono.HijrahEra as JavaHijrahEra
import java.time.chrono.IsoEra as JavaIsoEra
import java.time.chrono.JapaneseEra as JavaJapaneseEra
import java.time.chrono.MinguoEra as JavaMinguoEra
import java.time.chrono.ThaiBuddhistEra as JavaThaiBuddhistEra
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class EraDisplayNameJavaConformanceTest {
    @Test
    fun localizedNamesMatchJavaTimeForEverySupportedEra() {
        val eraPairs = buildList<Pair<JavaEra, Era>> {
            addAll(JavaIsoEra.values().zip(IsoEra.entries))
            addAll(JavaJapaneseEra.values().zip(JapaneseEra.values()))
            add(JavaHijrahEra.AH to HijrahEra.AH)
            addAll(JavaMinguoEra.values().zip(MinguoEra.entries))
            addAll(JavaThaiBuddhistEra.values().zip(ThaiBuddhistEra.entries))
        }
        val languageTags = listOf("und", "en", "fr", "de", "ja", "ar")

        eraPairs.forEach { (javaEra, era) ->
            languageTags.forEach { languageTag ->
                val locale = Locale.forLanguageTag(languageTag)
                val javaLocale = JavaLocale.forLanguageTag(languageTag)
                TextStyle.entries.forEach { style ->
                    assertEquals(
                        javaEra.getDisplayName(JavaTextStyle.valueOf(style.name), javaLocale),
                        era.getDisplayName(style, locale),
                        message = "$era, $style, $languageTag",
                    )
                }
            }
        }
    }
}
