package com.marzec.cache

/**
 * Adds page merging to the list cache used by both paging and CRUD.
 *
 * The merge runs through the wrapped ListCacheSaver's update operation so cache-specific
 * synchronization remains the responsibility of the underlying saver.
 */
class PagingCacheSaver<ID, MODEL>(
    private val saver: ListCacheSaver<ID, MODEL>,
    private val mergePages: (List<MODEL>?, List<MODEL>) -> List<MODEL>
) : ListCacheSaver<ID, MODEL> by saver {

    suspend fun mergePage(items: List<MODEL>): List<MODEL> {
        var mergedItems: List<MODEL>? = null
        saver.updateCache { cached ->
            mergePages(cached, items).also { mergedItems = it }
        }
        return checkNotNull(mergedItems)
    }
}
