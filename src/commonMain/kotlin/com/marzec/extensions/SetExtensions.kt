package com.marzec.extensions

fun <T> Set<T>.toggle(value: T): Set<T> = toMutableSet().let {
    if (value in this) {
        it.remove(value)
    } else {
        it.add(value)
    }
    it
}