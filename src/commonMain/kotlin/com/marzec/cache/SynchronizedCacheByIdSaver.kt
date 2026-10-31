package com.marzec.cache

import com.marzec.extensions.withSuspendLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex

class SynchronizedManyItemsCacheSaver<ID, MODEL>(private val saver: ManyItemsCacheSaver<ID, MODEL>) :
    ManyItemsCacheSaver<ID, MODEL> {

    private val lock = Mutex()

    override suspend fun getById(id: ID): MODEL? = saver.getById(id)

    override suspend fun observeCachedById(id: ID): Flow<MODEL?> = saver.observeCachedById(id)

    override suspend fun updateItem(id: ID, data: MODEL) = lock.withSuspendLock {
        saver.updateItem(id, data)
    }

    override suspend fun addItem(data: MODEL) {
        saver.addItem(data)
    }

    override suspend fun removeItem(id: ID) {
        saver.removeItem(id)
    }

    override suspend fun get(): List<MODEL>? = saver.get()

    override suspend fun observeCached(): Flow<List<MODEL>?> = saver.observeCached()

    override suspend fun saveCache(data: List<MODEL>) = lock.withSuspendLock {
        saver.saveCache(data)
    }
}

fun <ID, MODEL> ManyItemsCacheSaver<ID, MODEL>.synchronized(): ManyItemsCacheSaver<ID, MODEL> =
    SynchronizedManyItemsCacheSaver(this)
