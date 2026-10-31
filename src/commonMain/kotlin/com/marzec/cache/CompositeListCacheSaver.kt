package com.marzec.cache

class CompositeListCacheSaver<ID, MODEL>(
    private val saver: ListCacheSaver<ID, MODEL>,
    override val newItemInsert: List<MODEL>?.(item: MODEL) -> List<MODEL>? = atFirstPositionInserter()
) : ListCacheSaver<ID, MODEL> by saver {

    override suspend fun addItem(data: MODEL) {
        super.addItem(data)
    }

    override suspend fun removeItem(id: ID) {
        super.removeItem(id)
    }

    override suspend fun updateItem(id: ID, data: MODEL) {
        super.updateItem(id, data)
    }
}

fun <ID, MODEL> ListCacheSaver<ID, MODEL>.withAtLastInsert(): ManyItemsCacheSaver<ID, MODEL> {
    val inserter = atLastPositionInserter<MODEL>()
    return withInserter(inserter)
}

fun <ID, MODEL> ListCacheSaver<ID, MODEL>.withInserter(inserter: List<MODEL>?.(MODEL) -> MutableList<MODEL>) =
    CompositeListCacheSaver(
        this,
        inserter
    )
