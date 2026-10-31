package com.marzec.cache

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FileCacheImpl(
    private val fileName: String,
    private val json: Json,
    private val memoryCache: MemoryCache
) : FileCache {

    private val lock = reentrantLock()
    private var initialized: AtomicBoolean = AtomicBoolean(false)

    override suspend fun <T: Any> put(key: String, value: T?, serializer: KSerializer<T>) {
        initIfNeeded()
        memoryCache.put(key, value?.let { json.encodeToString(serializer, value) })
        updateFile()
    }

    override suspend fun <T: Any> get(key: String, serializer: KSerializer<T>): T? {
        initIfNeeded()
        return memoryCache.get<String>(key)?.let {
                json.decodeFromString(serializer, it)
        }
    }


    override suspend fun <T: Any> observe(key: String, serializer: KSerializer<T>): Flow<T?> {
        initIfNeeded()
        return memoryCache.observe<String>(key).map {
            it?.let { json.decodeFromString(serializer, it) }
        }
    }

    override suspend fun <T : Any> update(
        key: String,
        update: (T?) -> T?,
        serializer: KSerializer<T>
    ) {
        initIfNeeded()

        memoryCache.update<String>(key) { jsonValue ->
            jsonValue?.let {
                json.decodeFromString(serializer, it)
            }?.let {
                update(it)
            }?.let { json.encodeToString(serializer, it) }
        }

        updateFile()
    }

    private suspend fun initIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            try {
                lock.lock()
                createFileIfDoesNotExist()
                initMemoryCache()
            } finally {

                lock.unlock()
            }
        }
    }

    private fun createFileIfDoesNotExist() {
        val file = File(fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    private suspend fun initMemoryCache() {
        val file = File(fileName)
        val content = file.readText().ifEmpty { "{}" }
        json.decodeFromString<Map<String, String?>>(content).forEach {
            memoryCache.put(it.key, it.value)
        }
    }

    private suspend fun updateFile() {
        try {
            lock.lock()
            val file = File(fileName)
            val newContent = json.encodeToString(memoryCache.toMap() as Map<String, String?>)
            file.writeText(newContent)
        } finally {
            lock.unlock()
        }
    }
}
