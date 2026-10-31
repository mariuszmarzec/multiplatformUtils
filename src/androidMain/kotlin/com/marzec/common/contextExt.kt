package com.marzec.common

import android.content.Context

fun Context.readFile(fileName: String): String {
    return assets.open(fileName).bufferedReader().use { it.readText() }
}
