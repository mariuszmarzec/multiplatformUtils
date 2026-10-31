package com.marzec.cache

class ReversedManyItemsCacheSaver<ID, MODEL>(
    private val saver: ManyItemsCacheSaver<ID, MODEL>
) : ManyItemsCacheSaver<ID, MODEL> by saver {

    override suspend fun saveCache(data: List<MODEL>) {
        saver.saveCache(data.reversed())
    }
}

fun <ID, MODEL> ManyItemsCacheSaver<ID, MODEL>.toReversed(): ManyItemsCacheSaver<ID, MODEL> =
    ReversedManyItemsCacheSaver(this)
