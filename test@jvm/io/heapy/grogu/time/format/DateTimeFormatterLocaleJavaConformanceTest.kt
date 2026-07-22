package io.heapy.grogu.time.format

import io.heapy.grogu.time.Locale
import java.util.Locale as JavaLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeFormatterLocaleJavaConformanceTest {
    @Test
    fun explicitLocaleAndOverridesMatchJava() {
        val java = java.time.format.DateTimeFormatter.ofPattern("uuuu", JavaLocale.US)
        val grogu = DateTimeFormatter.ofPattern("uuuu", Locale.US)

        assertEquals(java.locale.toLanguageTag(), grogu.locale.toLanguageTag())
        assertEquals(
            java.withLocale(JavaLocale.UK).locale.toLanguageTag(),
            grogu.withLocale(Locale.UK).locale.toLanguageTag(),
        )
        assertSame(grogu, grogu.withLocale(Locale.US))
    }

    @Test
    fun builderLocaleMatchesJava() {
        val java = java.time.format.DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .toFormatter(JavaLocale.CANADA_FRENCH)
        val grogu = DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .toFormatter(Locale.forLanguageTag("fr-CA"))

        assertEquals(java.locale.toLanguageTag(), grogu.locale.toLanguageTag())
    }
}
