package io.heapy.krogu.time.format

import io.heapy.krogu.time.LocalDate
import io.heapy.krogu.time.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DateTimeFormatterLocaleTest {
    @Test
    fun patternFormatterStoresTheRequestedLocale() {
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US)

        assertEquals(Locale.US, formatter.locale)
        assertEquals("2024-02-29", formatter.format(LocalDate.of(2024, 2, 29)))
    }

    @Test
    fun defaultPatternAndBuilderFormattersCaptureTheDefaultFormatLocale() {
        assertEquals(Locale.getDefault(), DateTimeFormatter.ofPattern("uuuu").locale)
        assertEquals(Locale.getDefault(), DateTimeFormatterBuilder().toFormatter().locale)
    }

    @Test
    fun builderCanCreateAFormatterForAnExplicitLocale() {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("uuuu")
            .toFormatter(Locale.UK)

        assertEquals(Locale.UK, formatter.locale)
    }

    @Test
    fun localeOverrideIsImmutableAndIdempotent() {
        val original = DateTimeFormatter.ofPattern("uuuu", Locale.US)
        val localized = original.withLocale(Locale.UK)

        assertNotSame(original, localized)
        assertEquals(Locale.US, original.locale)
        assertEquals(Locale.UK, localized.locale)
        assertSame(localized, localized.withLocale(Locale.UK))
        assertEquals(Locale.UK, localized.withResolverStyle(ResolverStyle.LENIENT).locale)
    }
}
