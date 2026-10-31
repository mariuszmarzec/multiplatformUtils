package com.marzec.repository

import com.marzec.cache.CacheSaver
import com.marzec.cache.PagingCacheSaver
import com.marzec.content.Content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Decorates a CRUD repository with cursor paging while sharing its exact cache saver.
 *
 * [loadPage] returns a snapshot of all item IDs recorded in the persisted paging session, not
 * just the raw response page. Paging progress is persisted separately from the model cache.
 * Each page is merged into the shared CRUD cache before its continuation state is committed.
 */
class PagingCrudRepository<
    ID,
    MODEL : Any,
    CREATE : Any,
    UPDATE : Any,
    MODEL_DTO : Any,
    CREATE_DTO : Any,
    UPDATE_DTO : Any,
    CURSOR
>(
    private val crudRepository: CrudRepository<ID, MODEL, CREATE, UPDATE, MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val cacheSaver: PagingCacheSaver<ID, MODEL>,
    private val pagingStateSaver: CacheSaver<PagingState<ID, CURSOR>>,
    private val idOf: MODEL.() -> ID,
    private val pageLoader: PageLoader<CURSOR, MODEL>,
    private val pagingPolicy: Paging = Paging.OnDemand
) {

    private val pageLock = Mutex()

    suspend fun loadPage(cursor: CURSOR?): Page<CURSOR, MODEL> = pageLock.withLock {
        loadPageWithPolicy(cursor)
    }

    /**
     * Loads the next page using the continuation state owned by this repository.
     * The caller decides when to request more data but never supplies a cursor.
     */
    suspend fun loadNextPage(): Page<CURSOR, MODEL> = pageLock.withLock {
        var page = loadNextPageLocked()
        if (pagingPolicy == Paging.Eagerly) {
            while (page.nextCursor != null) {
                page = loadNextPageLocked()
            }
        }
        page
    }

    private suspend fun loadNextPageLocked(): Page<CURSOR, MODEL> {
        val storedState = pagingStateSaver.get()
        if (storedState?.isComplete == true) {
            val cachedItems = cacheSaver.get().orEmpty()
            val cachedIds = cachedItems.map { it.idOf() }.toSet()
            if (storedState.loadedItemIds.all { it in cachedIds }) {
                return Page(
                    items = cachedItems.filter { it.idOf() in storedState.loadedItemIds },
                    nextCursor = null
                )
            }
        }

        return loadPageLocked(storedState?.nextCursor)
    }

    private suspend fun loadPageWithPolicy(cursor: CURSOR?): Page<CURSOR, MODEL> {
        var page = loadPageLocked(cursor)
        if (pagingPolicy == Paging.Eagerly) {
            while (page.nextCursor != null) {
                page = loadNextPageLocked()
            }
        }
        return page
    }

    private suspend fun loadPageLocked(cursor: CURSOR?): Page<CURSOR, MODEL> {
        val context = preparePageLoad(cursor)
        val page = pageLoader.load(context.requestedCursor)
        validatePage(page, context.visitedCursors)
        return commitPage(context, page)
    }

    private suspend fun preparePageLoad(cursor: CURSOR?): PageLoadContext<ID, CURSOR> {
        val storedState = pagingStateSaver.get()
        var requestedCursor = cursor
        var state = storedState ?: PagingState<ID, CURSOR>()

        if (cursor != null && storedState == null) {
            requestedCursor = null
        } else if (cursor != null) {
            check(!state.isComplete && cursor == state.nextCursor) {
                "Unexpected paging cursor: $cursor (expected ${state.nextCursor})"
            }

            val cachedIds = cacheSaver.get().orEmpty().map { it.idOf() }.toSet()
            if (!state.loadedItemIds.all { it in cachedIds }) {
                requestedCursor = null
            }
        }

        // A null cursor starts a new session. Roll back to a clean state if committing its
        // metadata fails, rather than restoring an old session's cursor.
        if (requestedCursor == null) {
            state = PagingState()
        }

        return PageLoadContext(
            rollbackState = state,
            requestedCursor = requestedCursor,
            visitedCursors = state.visitedCursors.toMutableSet(),
            loadedItemIds = state.loadedItemIds.toMutableSet()
        )
    }

    private suspend fun commitPage(
        context: PageLoadContext<ID, CURSOR>,
        page: Page<CURSOR, MODEL>
    ): Page<CURSOR, MODEL> {
        // Write models before advancing the cursor. Retrying a page after a failed metadata
        // write is safe because the cache merge replaces items with matching IDs.
        val mergedItems = cacheSaver.mergePage(page.items)
        context.loadedItemIds.addAll(page.items.map { it.idOf() })
        page.nextCursor?.let(context.visitedCursors::add)

        val nextState = PagingState(
            nextCursor = page.nextCursor,
            isComplete = page.nextCursor == null,
            visitedCursors = context.visitedCursors.toList(),
            loadedItemIds = context.loadedItemIds.toList()
        )
        try {
            pagingStateSaver.saveCache(nextState)
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                try {
                    pagingStateSaver.saveCache(context.rollbackState)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            }
            throw failure
        }

        val snapshot = mergedItems.filter { it.idOf() in context.loadedItemIds }
        return Page(items = snapshot, nextCursor = page.nextCursor)
    }

    private data class PageLoadContext<ID, CURSOR>(
        val rollbackState: PagingState<ID, CURSOR>,
        val requestedCursor: CURSOR?,
        val visitedCursors: MutableSet<CURSOR>,
        val loadedItemIds: MutableSet<ID>
    )

    private fun validatePage(page: Page<CURSOR, MODEL>, requestedCursors: Set<CURSOR>) {
        check(page.nextCursor == null || page.items.isNotEmpty()) {
            "Paging returned an empty page with a next cursor"
        }
        val nextCursor = page.nextCursor
        check(nextCursor == null || nextCursor !in requestedCursors) {
            "Paging returned a repeating cursor: $nextCursor"
        }
    }

    /**
     * Emits cached items immediately as an incomplete result, then explicitly scans the complete
     * collection. Every page uses the same persisted state, validation, cache merge, and commit
     * path as [loadPage], and each successful page emits the accumulated session snapshot.
     *
     * The scan starts at a null cursor and resets the active paging session for this query.
     * The final Content.Data includes only IDs observed by the completed scan, excluding unrelated
     * or stale cached entries. The next-page cursor is always recovered from persisted state.
     */
    suspend fun observeAllPages(): Flow<Content<List<MODEL>>> = flow {
        val cachedItems = try {
            cacheSaver.get().orEmpty()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            emit(Content.Error(exception))
            return@flow
        }
        emit(Content.Loading(cachedItems.takeIf { it.isNotEmpty() }))

        try {
            pageLock.withLock {
                var page = loadPageLocked(null)
                if (page.nextCursor != null) {
                    emit(Content.Loading(page.items))
                }

                while (page.nextCursor != null) {
                    page = loadNextPageLocked()
                    if (page.nextCursor != null) {
                        emit(Content.Loading(page.items))
                    }
                }

                emit(Content.Data(page.items))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            emit(Content.Error(exception))
        }
    }

    suspend fun observeById(id: ID): Flow<Content<MODEL>> = crudRepository.observeById(id)

    // Mutations through this paging API must not invoke the CRUD delegate's non-paginated
    // getAll() refresh. Full-collection reads intentionally are not exposed by this wrapper.

    suspend fun remove(id: ID): Flow<Content<Unit>> =
        crudRepository.remove(id, CrudRepository.RefreshPolicy.NO_REFRESH)

    suspend fun update(id: ID, model: UPDATE): Flow<Content<MODEL>> =
        crudRepository.update(id, model, CrudRepository.RefreshPolicy.NO_REFRESH)

    suspend fun create(create: CREATE): Flow<Content<MODEL>> =
        crudRepository.create(create, CrudRepository.RefreshPolicy.NO_REFRESH)
}
