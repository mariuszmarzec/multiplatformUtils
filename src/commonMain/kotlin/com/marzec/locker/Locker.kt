package com.marzec.locker

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class Locker {

    private val locker = reentrantLock()

    fun lock() = locker.lock()

    fun unlock() = locker.unlock()

    fun tryLock(): Boolean = locker.tryLock()

    fun <T> withLock(block: () -> T) = locker.withLock(block)
}