package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MapCacheSaver<TARGET : Any, NESTED : Any>(
    private val cacheSaver: UpdatableCacheSaver<NESTED>,
    private val toNested: TARGET.() -> NESTED,
    private val toTarget: NESTED.() -> TARGET,
) : UpdatableCacheSaver<TARGET> {

    override suspend fun get(): TARGET? = cacheSaver.get()?.toTarget()

    override suspend fun observeCached(): Flow<TARGET?> =
        cacheSaver.observeCached().map { it?.toTarget() }

    override suspend fun saveCache(data: TARGET) = cacheSaver.saveCache(data.toNested())

    override suspend fun updateCache(update: (TARGET?) -> TARGET?) {
        cacheSaver.updateCache {
            update(it?.toTarget())?.toNested()
        }
    }
}

fun <T : Any, R : Any> UpdatableCacheSaver<T>.map(
    toNested: R.() -> T,
    toTarget: T.() -> R,
): UpdatableCacheSaver<R> =
    MapCacheSaver(
        cacheSaver = this,
        toNested = toNested,
        toTarget = toTarget
    )