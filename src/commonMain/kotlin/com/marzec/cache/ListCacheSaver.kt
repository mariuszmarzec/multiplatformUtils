package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ListCacheSaver<ID, MODEL> : ManyItemsCacheSaver<ID, MODEL>, WithListByIdUpdate<ID, MODEL>

class ListCacheSaverImpl<ID, MODEL>(
    private val cacheSaver: UpdatableCacheSaver<List<MODEL>>,
    override val isSameId: MODEL.(id: ID) -> Boolean,
    override val newItemInsert: List<MODEL>?.(item: MODEL) -> List<MODEL>? = atFirstPositionInserter()
) : ListCacheSaver<ID, MODEL>,
    UpdatableCacheSaver<List<MODEL>> by cacheSaver {

    override suspend fun getById(id: ID): MODEL? =
        cacheSaver.get()?.firstOrNull { it.isSameId(id) }

    override suspend fun observeCachedById(id: ID): Flow<MODEL?> =
        cacheSaver.observeCached().map { list ->
            list?.firstOrNull { it.isSameId(id) }
        }
}

@Suppress("FunctionName")
fun <ID, MODEL> ListCacheSaver(
    cacheSaver: UpdatableCacheSaver<List<MODEL>>,
    isSameId: MODEL.(id: ID) -> Boolean
) = ListCacheSaverImpl(
    cacheSaver = cacheSaver,
    isSameId = isSameId
)
