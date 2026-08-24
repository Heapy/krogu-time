package io.heapy.krogu.time.format

import io.heapy.krogu.time.Locale
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeFormatterLocaleJavaConformanceTest {
    @Test
    fun explicitLocaleAndOverridesMatchJava() {
        val java = java.time.format.DateTimeFormatter.ofPattern("uuuu", JavaLocale.US)
        val krogu = DateTimeFormatter.ofPattern("uuuu", Locale.US)

        assertEquals(java.locale.toLanguageTag(), krogu.locale.toLanguageTag())
        assertEquals(
            java.withLocale(JavaLocale.UK).locale.toLanguageTag(),
            krogu.withLocale(Locale.UK).locale.toLanguageTag(),
        )
        assertSame(krogu, krogu.withLocale(Locale.US))
    }

    @Test
    fun builderLocaleMatchesJava() {
        val java = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .toFormatter(JavaLocale.CANADA_FRENCH)
        val krogu = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .toFormatter(Locale.forLanguageTag("fr-CA"))

        assertEquals(java.locale.toLanguageTag(), krogu.locale.toLanguageTag())
    }
}
