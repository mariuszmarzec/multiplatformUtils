package com.marzec.extensions

fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit): T {
    if (condition()) {
        block()
    }
    return this
}

fun <T> T.letIf(condition: T.() -> Boolean, block: T.() -> T): T = let {
    if (condition()) {
        block()
    } else {
        this
    }
}