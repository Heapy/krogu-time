package io.heapy.krogu.time.format

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatEnumsJavaConformanceTest {
    @Test
    fun enumConstantsAndTextStyleConversionsMatchJavaTime() {
        assertEquals(
            java.time.format.ResolverStyle.values().map(Enum<*>::name),
            ResolverStyle.entries.map(Enum<*>::name),
        )
        assertEquals(
            java.time.format.FormatStyle.values().map(Enum<*>::name),
            FormatStyle.entries.map(Enum<*>::name),
        )
        assertEquals(
            java.time.format.SignStyle.values().map(Enum<*>::name),
            SignStyle.entries.map(Enum<*>::name),
        )
        assertEquals(
            java.time.format.TextStyle.values().map(Enum<*>::name),
            TextStyle.entries.map(Enum<*>::name),
        )

        TextStyle.entries.forEach { style ->
            val javaStyle = java.time.format.TextStyle.valueOf(style.name)
            assertEquals(javaStyle.isStandalone, style.isStandalone, style.name)
            assertEquals(javaStyle.asStandalone().name, style.asStandalone().name, style.name)
            assertEquals(javaStyle.asNormal().name, style.asNormal().name, style.name)
        }
    }
}
