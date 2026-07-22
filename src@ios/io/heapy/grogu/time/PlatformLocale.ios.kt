package io.heapy.grogu.time

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

internal actual fun defaultFormatLocaleTag(): String =
    NSLocale.currentLocale.localeIdentifier
        .substringBefore('@')
        .replace('_', '-')
