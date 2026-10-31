package com.marzec.cache

import com.marzec.extensions.withSuspendLock
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MemoryCache(cacheSize: Int = MAX_CACHE_SIZE) : Cache {

    private val lock = reentrantLock()

    private val cache: MutableMap<String, MutableStateFlow<Any?>> =
        object : LinkedHashMap<String, MutableStateFlow<Any?>>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MutableStateFlow<Any?>>?
            ): Boolean = size > cacheSize
        }

    override suspend fun put(key: String, value: Any?): Unit = lock.withSuspendLock {
        putInternal(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: String): T? = lock.withSuspendLock {
        return cache[key]?.value as? T
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> update(key: String, action: (T?) -> T?): Unit = lock.withSuspendLock {
        val oldValue = cache[key]?.value as? T?
        putInternal(key, action(oldValue))
    }

    override suspend fun remove(key: String): Unit = lock.withSuspendLock {
        cache.remove(key)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> observe(key: String): Flow<T?> = lock.withSuspendLock {
        val state = cache[key] ?: putInternal(
            key,
            null
        ).let { cache[key]!! }
        state.map {
            it as? T
        }
    }

    override suspend fun toMap(): Map<String, Any?> = lock.withSuspendLock {
        cache.mapValues { it.value.value }
    }

    private suspend fun putInternal(key: String, value: Any?) {
        if (cache.containsKey(key)) {
            cache[key]?.emit(value)
        } else {
            cache[key] = MutableStateFlow(value)
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = Int.MAX_VALUE
    }
}
