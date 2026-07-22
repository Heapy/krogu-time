package io.heapy.grogu.time

import android.os.Build

internal actual fun defaultFormatLocaleTag(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        java.util.Locale.getDefault(java.util.Locale.Category.FORMAT).toLanguageTag()
    } else {
        java.util.Locale.getDefault().toLanguageTag()
    }
