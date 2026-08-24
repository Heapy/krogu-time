package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FormatEnumsTest {
    @Test
    fun exposesResolverFormatAndSignStylesInJavaOrder() {
        assertEquals(
            listOf("STRICT", "SMART", "LENIENT"),
            ResolverStyle.entries.map(Enum<*>::name),
        )
        assertEquals(
            listOf("FULL", "LONG", "MEDIUM", "SHORT"),
            FormatStyle.entries.map(Enum<*>::name),
        )
        assertEquals(
            listOf("NORMAL", "ALWAYS", "NEVER", "NOT_NEGATIVE", "EXCEEDS_PAD"),
            SignStyle.entries.map(Enum<*>::name),
        )
    }

    @Test
    fun textStylesConvertBetweenNormalAndStandaloneForms() {
        assertEquals(
            listOf(
                "FULL",
                "FULL_STANDALONE",
                "SHORT",
                "SHORT_STANDALONE",
                "NARROW",
                "NARROW_STANDALONE",
            ),
            TextStyle.entries.map(Enum<*>::name),
        )

        assertFalse(TextStyle.FULL.isStandalone)
        assertTrue(TextStyle.FULL_STANDALONE.isStandalone)
        assertSame(TextStyle.FULL_STANDALONE, TextStyle.FULL.asStandalone())
        assertSame(TextStyle.FULL, TextStyle.FULL_STANDALONE.asNormal())
        assertSame(TextStyle.SHORT_STANDALONE, TextStyle.SHORT.asStandalone())
        assertSame(TextStyle.SHORT, TextStyle.SHORT_STANDALONE.asNormal())
        assertSame(TextStyle.NARROW_STANDALONE, TextStyle.NARROW.asStandalone())
        assertSame(TextStyle.NARROW, TextStyle.NARROW_STANDALONE.asNormal())
        assertSame(TextStyle.FULL, TextStyle.FULL.asNormal())
        assertSame(TextStyle.NARROW_STANDALONE, TextStyle.NARROW_STANDALONE.asStandalone())
    }
}
