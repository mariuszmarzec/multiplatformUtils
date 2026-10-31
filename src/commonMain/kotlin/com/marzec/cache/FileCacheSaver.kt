package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

class FileCacheSaver<T : Any>(
    private val key: String,
    private val fileCache: FileCache,
    private val serializer: KSerializer<T>
) : UpdatableCacheSaver<T> {

    override suspend fun get(): T? = fileCache.get(key, serializer)

    override suspend fun observeCached(): Flow<T?> = fileCache.observe(key, serializer)

    override suspend fun saveCache(data: T) = fileCache.put(key, data, serializer)

    override suspend fun updateCache(update: (T?) -> T?) {
        fileCache.update(key, update, serializer)
    }
}