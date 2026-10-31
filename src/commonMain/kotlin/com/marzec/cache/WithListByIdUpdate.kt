package com.marzec.cache

interface WithListByIdUpdate<ID, MODEL> : CacheByIdSaver<ID, MODEL> {

    val isSameId: MODEL.(id: ID) -> Boolean

    val newItemInsert: List<MODEL>?.(item: MODEL) -> List<MODEL>?
        get() = atFirstPositionInserter()

    override suspend fun updateItem(id: ID, data: MODEL) {
        updateById(id, data)
    }

    override suspend fun addItem(data: MODEL) {
        updateCache { cachedList -> cachedList.newItemInsert(data) }
    }

    override suspend fun removeItem(id: ID) {
        updateCache { cachedList ->
            cachedList?.toMutableList()?.apply {
                removeIf { it.isSameId(id) }
            }
        }
    }

    private suspend fun updateById(id: ID, data: MODEL) = updateCache { old ->
        old?.map {
            if (it.isSameId(id)) {
                data
            } else {
                it
            }
        } ?: listOf(data)
    }

    suspend fun updateCache(update: (List<MODEL>?) -> List<MODEL>?)
}

fun <MODEL> atFirstPositionInserter(): List<MODEL>?.(MODEL) -> MutableList<MODEL> = {
    orEmpty().toMutableList().apply {
        add(0, it)
    }
}

fun <MODEL> atLastPositionInserter(): List<MODEL>?.(MODEL) -> MutableList<MODEL> = {
    orEmpty().toMutableList().apply {
        add(it)
    }
}

fun <MODEL, R : Comparable<R>> sortByInserter(
    byDescending: Boolean = false,
    selector: (MODEL) -> R?
): List<MODEL>?.(MODEL) -> MutableList<MODEL> = {
    orEmpty().toMutableList().apply {
        add(it)
        if (byDescending) {
            sortByDescending(selector)
        } else {
            sortBy(selector)
        }
    }
}
