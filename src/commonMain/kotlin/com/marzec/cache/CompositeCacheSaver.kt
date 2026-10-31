package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class CompositeUpdatableCacheSaver<T : Any>(
    private val savers: List<UpdatableCacheSaver<T>>
) : UpdatableCacheSaver<T> {

    // TODO: Revisit this composite-cache hydration workaround; ownership and reconciliation
    // between the in-memory and persistent savers should have an explicit long-term design.
    override suspend fun get(): T? {
        val primary = savers.firstOrNull() ?: return null
        primary.get()?.let { return it }

        // The primary in-memory saver may be empty after process restart while a secondary
        // persistent saver still has the value. Hydrate the primary before returning a miss.
        for (saver in savers.drop(1)) {
            val cached = saver.get() ?: continue
            primary.saveCache(cached)
            return cached
        }

        return null
    }
    override suspend fun observeCached(): Flow<T?> =
        savers.mapIndexed { index, saver ->
            saver.observeCached().filterNotNull().let { flow ->
                if (index > 0) {
                    flow.distinctUntilChanged()
                        .onEach { newValue ->
                            savers.firstOrNull()?.saveCache(newValue)
                        }
                } else {
                    flow
                }
            }
        }.merge()
            .distinctUntilChanged()

    override suspend fun saveCache(data: T) {
        savers.forEach {
            it.saveCache(data)
        }
    }

    override suspend fun updateCache(update: (T?) -> T?) {
        // A read-modify-write must hydrate persisted data before updating an empty primary cache.
        get()
        savers.forEach {
            it.updateCache(update)
        }
    }
}
