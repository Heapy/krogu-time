package io.heapy.grogu.time

internal actual fun defaultFormatLocaleTag(): String =
    java.util.Locale.getDefault(java.util.Locale.Category.FORMAT).toLanguageTag()
