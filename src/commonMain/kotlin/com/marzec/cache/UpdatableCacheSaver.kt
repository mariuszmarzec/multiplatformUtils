package com.marzec.cache

interface UpdatableCacheSaver<T> : CacheSaver<T> {
    suspend fun updateCache(update: (T?) -> T?)
}