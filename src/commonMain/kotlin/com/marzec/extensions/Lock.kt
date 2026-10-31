package com.marzec.extensions

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.coroutines.sync.Mutex

suspend inline fun <T> ReentrantLock.withSuspendLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}

suspend inline fun <T> Mutex.withSuspendLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}

