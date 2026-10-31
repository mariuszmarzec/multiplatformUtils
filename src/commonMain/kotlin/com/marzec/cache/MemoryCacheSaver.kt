package com.marzec.cache

import kotlinx.coroutines.flow.Flow

class MemoryCacheSaver<T>(
    private val key: String,
    private val memoryCache: Cache
) : UpdatableCacheSaver<T> {

    override suspend fun get(): T? = memoryCache.get<T>(key)

    override suspend fun observeCached(): Flow<T?> = memoryCache.observe<T>(key)

    override suspend fun saveCache(data: T) = memoryCache.put(key, data)

    override suspend fun updateCache(update: (T?) -> T?) {
        memoryCache.update(key, update)
    }
}

@Suppress("FunctionName")
fun <ID, MODEL> MemoryListCacheSaver(
    key: String,
    memoryCache: Cache,
    isSameId: MODEL.(id: ID) -> Boolean,
    newItemInsert: List<MODEL>?.(item: MODEL) -> List<MODEL>? = atFirstPositionInserter()
) = ListCacheSaverImpl(
    cacheSaver = MemoryCacheSaver(
        key = key,
        memoryCache = memoryCache
    ),
    isSameId = isSameId,
    newItemInsert = newItemInsert
)
