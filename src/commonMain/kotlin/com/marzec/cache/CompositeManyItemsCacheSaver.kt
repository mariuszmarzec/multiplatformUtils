package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class CompositeManyItemsCacheSaver<ID, MODEL>(
    private val savers: List<ManyItemsCacheSaver<ID, MODEL>>
) : ManyItemsCacheSaver<ID, MODEL> {

    override suspend fun get(): List<MODEL>? = savers.firstOrNull()?.get()

    override suspend fun observeCached(): Flow<List<MODEL>?> =
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

    override suspend fun removeItem(id: ID) {
        savers.forEach { it.removeItem(id) }
    }

    override suspend fun addItem(data: MODEL) {
        savers.forEach { it.addItem(data) }
    }

    override suspend fun updateItem(id: ID, data: MODEL) {
        savers.forEach { it.updateItem(id, data) }
    }

    override suspend fun observeCachedById(id: ID): Flow<MODEL?> =
        savers.mapIndexed { index, saver ->
            saver.observeCachedById(id).filterNotNull().let { flow ->
                if (index > 0) {
                    flow.distinctUntilChanged()
                        .onEach { newValue ->
                            savers.firstOrNull()?.updateItem(id, newValue)
                        }
                } else {
                    flow
                }
            }
        }.merge()
            .distinctUntilChanged()

    override suspend fun getById(id: ID): MODEL? = savers.firstOrNull()?.getById(id)

    override suspend fun saveCache(data: List<MODEL>) {
        savers.forEach {
            it.saveCache(data)
        }
    }
}
