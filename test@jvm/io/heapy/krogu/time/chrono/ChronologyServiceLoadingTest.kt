package io.heapy.krogu.time.chrono

import io.heapy.krogu.time.Locale
import java.net.URL
import java.util.Collections
import java.util.Enumeration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChronologyServiceLoadingTest {
    @Test
    fun discoversApplicationChronologiesFromTheContextClassLoader() {
        withServiceProvider {
            assertIs<TestServiceChronology>(Chronology.of(TestServiceChronology.ID))
            assertIs<TestServiceChronology>(Chronology.of(TestServiceChronology.CALENDAR_TYPE))
            assertIs<TestServiceChronology>(
                Chronology.ofLocale(
                    Locale.forLanguageTag("en-u-ca-${TestServiceChronology.CALENDAR_TYPE}"),
                ),
            )
            assertTrue(
                Chronology.getAvailableChronologies().any { chronology ->
                    chronology.id == TestServiceChronology.ID
                },
            )
            assertEquals(
                TestServiceChronology.CALENDAR_TYPE,
                Chronology.of(TestServiceChronology.ID).calendarType,
            )
        }
    }

    private fun withServiceProvider(block: () -> Unit) {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        val serviceDefinition = requireNotNull(
            javaClass.classLoader.getResource(SERVICE_FIXTURE),
        )
        thread.contextClassLoader = object : ClassLoader(previous) {
            override fun getResources(name: String): Enumeration<URL> =
                if (name == SERVICE_RESOURCE) {
                    Collections.enumeration(listOf(serviceDefinition))
                } else {
                    super.getResources(name)
                }
        }
        try {
            block()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    private companion object {
        const val SERVICE_RESOURCE: String =
            "META-INF/services/io.heapy.krogu.time.chrono.Chronology"
        const val SERVICE_FIXTURE: String = "service-fixtures/krogu-time-chronology"
    }
}
