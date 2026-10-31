package com.marzec.repository

import com.marzec.cache.CacheSaver
import com.marzec.cache.CompositeUpdatableCacheSaver
import com.marzec.cache.FileCacheSaver
import com.marzec.cache.FileCacheImpl
import com.marzec.cache.MemoryCache
import com.marzec.cache.MemoryCacheSaver
import com.marzec.cache.MemoryListCacheSaver
import com.marzec.cache.PagingCacheSaver
import com.marzec.content.Content
import com.marzec.datasource.CrudDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PagingCrudRepositoryTest {

    private fun repository(
        cache: MemoryCache,
        pages: Map<Int?, Page<Int, Int>>,
        dataSource: CrudDataSource<Int, Int, Int, Int> = mockk(relaxed = true),
        pagingStateSaver: CacheSaver<PagingState<Int, Int>> = MemoryCacheSaver(
            key = "paging-state",
            memoryCache = cache
        ),
        pageLoader: PageLoader<Int, Int> = PageLoader { cursor ->
            pages[cursor] ?: error("Unexpected cursor: $cursor")
        },
        pagingPolicy: Paging = Paging.OnDemand
    ): PagingCrudRepository<Int, Int, Int, Int, Int, Int, Int, Int> {
        val baseSaver = MemoryListCacheSaver<Int, Int>(
            key = "items",
            memoryCache = cache,
            isSameId = { id -> this == id }
        )
        val pagingSaver = PagingCacheSaver(baseSaver) { cached, incoming ->
            val incomingIds = incoming.toSet()
            (cached.orEmpty().filterNot { it in incomingIds } + incoming).sortedDescending()
        }
        val crudRepository = CrudRepository<Int, Int, Int, Int, Int, Int, Int>(
            dataSource = dataSource,
            dispatcher = UnconfinedTestDispatcher(),
            cacheSaver = pagingSaver,
            toDomain = { this },
            updateToDto = { this },
            createToDto = { this },
            updaterCoroutineScope = CoroutineScope(SupervisorJob())
        )
        return PagingCrudRepository(
            crudRepository = crudRepository,
            cacheSaver = pagingSaver,
            pagingStateSaver = pagingStateSaver,
            idOf = { this },
            pageLoader = pageLoader,
            pagingPolicy = pagingPolicy
        )
    }

    @Test
    fun `loadNextPage reads the continuation cursor from persisted state`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                when (cursor) {
                    null -> Page(items = listOf(10, 9), nextCursor = 8)
                    8 -> Page(items = listOf(8, 5), nextCursor = null)
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val first = repository.loadPage(null)
        val second = repository.loadNextPage()

        assertEquals(listOf<Int?>(null, 8), requestedCursors)
        assertEquals(listOf(10, 9), first.items)
        assertEquals(listOf(10, 9, 8, 5), second.items)
        assertEquals(null, second.nextCursor)
    }

    @Test
    fun `eager policy loads all pages from a first-page request`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pagingPolicy = Paging.Eagerly,
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                when (cursor) {
                    null -> Page(items = listOf(10, 9), nextCursor = 8)
                    8 -> Page(items = listOf(8, 5), nextCursor = null)
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val page = repository.loadPage(null)

        assertEquals(listOf<Int?>(null, 8), requestedCursors)
        assertEquals(listOf(10, 9, 8, 5), page.items)
        assertEquals(null, page.nextCursor)
        assertEquals(true, cache.get<PagingState<Int, Int>>("paging-state")?.isComplete)
    }

    @Test
    fun `eager policy resumes from persisted progress after a later page fails`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        var failLaterPage = true
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pagingPolicy = Paging.Eagerly,
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                when (cursor) {
                    null -> Page(items = listOf(10, 9), nextCursor = 8)
                    8 -> {
                        if (failLaterPage) {
                            failLaterPage = false
                            error("network down")
                        }
                        Page(items = listOf(8, 5), nextCursor = null)
                    }
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        assertFailsWith<IllegalStateException> { repository.loadPage(null) }
        val resumed = repository.loadNextPage()

        assertEquals(listOf<Int?>(null, 8, 8), requestedCursors)
        assertEquals(listOf(10, 9, 8, 5), resumed.items)
        assertEquals(null, resumed.nextCursor)
        assertEquals(true, cache.get<PagingState<Int, Int>>("paging-state")?.isComplete)
    }

    @Test
    fun `repository accumulates loaded pages and passes cursor through unchanged`() = runTest {
        val cache = MemoryCache()
        val repository = repository(
            cache = cache,
            pages = mapOf(
                null to Page(items = listOf(10, 9), nextCursor = 8),
                8 to Page(items = listOf(8, 5), nextCursor = null)
            )
        )

        val first = repository.loadPage(null)
        val second = repository.loadPage(8)

        assertEquals(listOf(10, 9), first.items)
        assertEquals(8, first.nextCursor)
        assertEquals(listOf(10, 9, 8, 5), second.items)
        assertEquals(null, second.nextCursor)
    }

    @Test
    fun `paging state restores next cursor and loaded item IDs when repository is recreated`() = runTest {
        val cache = MemoryCache()
        val pages: Map<Int?, Page<Int, Int>> = mapOf(
            null to Page(items = listOf(10, 9), nextCursor = 8),
            8 to Page(items = listOf(8, 5), nextCursor = null)
        )
        val firstRepository = repository(cache = cache, pages = pages)

        val firstPage = firstRepository.loadPage(null)
        val recreatedRepository = repository(cache = cache, pages = pages)
        val secondPage = recreatedRepository.loadPage(checkNotNull(firstPage.nextCursor))
        val persistedState = cache.get<PagingState<Int, Int>>("paging-state")

        assertEquals(listOf(10, 9), firstPage.items)
        assertEquals(listOf(10, 9, 8, 5), secondPage.items)
        assertEquals(null, secondPage.nextCursor)
        assertEquals(true, persistedState?.isComplete)
        assertEquals(listOf(10, 9, 8, 5), persistedState?.loadedItemIds)
        assertEquals(listOf(8), persistedState?.visitedCursors)
    }

    @Test
    fun `missing cached models reset persisted paging to the first page`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        val pages: Map<Int?, Page<Int, Int>> = mapOf(
            null to Page(items = listOf(10, 9), nextCursor = 8),
            8 to Page(items = listOf(8, 5), nextCursor = null)
        )
        val firstRepository = repository(
            cache = cache,
            pages = pages,
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                pages[cursor] ?: error("Unexpected cursor: $cursor")
            }
        )
        val firstPage = firstRepository.loadPage(null)
        cache.remove("items")

        val recreatedRepository = repository(
            cache = cache,
            pages = pages,
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                pages[cursor] ?: error("Unexpected cursor: $cursor")
            }
        )
        val recovered = recreatedRepository.loadPage(checkNotNull(firstPage.nextCursor))

        assertEquals(listOf<Int?>(null, null), requestedCursors)
        assertEquals(listOf(10, 9), recovered.items)
        assertEquals(8, recovered.nextCursor)
        assertEquals(listOf(10, 9), cache.get<PagingState<Int, Int>>("paging-state")?.loadedItemIds)
    }

    @Test
    fun `composite cache hydrates models from file after memory cache starts empty`() = runTest {
        val file = File.createTempFile("paged-models", ".json")
        val persistedModels = listOf(10, 9)
        try {
            val serializer = serializer(typeOf<List<Int>>()) as KSerializer<List<Int>>
            val fileCache = FileCacheImpl(file.absolutePath, Json {}, MemoryCache())
            val fileSaver = FileCacheSaver("models", fileCache, serializer)
            fileSaver.saveCache(persistedModels)

            val primaryMemory = MemoryCache()
            val primarySaver = MemoryCacheSaver<List<Int>>(key = "models", memoryCache = primaryMemory)
            val compositeSaver = CompositeUpdatableCacheSaver(listOf(primarySaver, fileSaver))

            assertEquals(persistedModels, compositeSaver.get())
            assertEquals(persistedModels, primaryMemory.get<List<Int>>("models"))

            compositeSaver.updateCache { cached -> cached.orEmpty() + 8 }

            assertEquals(listOf(10, 9, 8), primaryMemory.get<List<Int>>("models"))
            assertEquals(listOf(10, 9, 8), fileSaver.get())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `paging state saver restores metadata from file cache`() = runTest {
        val file = File.createTempFile("paging-state", ".json")
        try {
            val state = PagingState<Int, Int>(
                nextCursor = 8,
                isComplete = false,
                visitedCursors = listOf(8),
                loadedItemIds = listOf(10, 9)
            )
            val stateSerializer = serializer(typeOf<PagingState<Int, Int>>()) as KSerializer<PagingState<Int, Int>>
            val otherState = PagingState<Int, Int>(
                nextCursor = 2,
                isComplete = false,
                visitedCursors = listOf(2),
                loadedItemIds = listOf(4)
            )
            val firstFileCache = FileCacheImpl(file.absolutePath, Json {}, MemoryCache())
            FileCacheSaver("query-a::paging-state", firstFileCache, stateSerializer).saveCache(state)
            FileCacheSaver("query-b::paging-state", firstFileCache, stateSerializer).saveCache(otherState)

            val reopenedFileCache = FileCacheImpl(file.absolutePath, Json {}, MemoryCache())
            val restored = FileCacheSaver("query-a::paging-state", reopenedFileCache, stateSerializer).get()
            val restoredOther = FileCacheSaver("query-b::paging-state", reopenedFileCache, stateSerializer).get()

            assertEquals(state, restored)
            assertEquals(otherState, restoredOther)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `failed paging state write can retry the same page idempotently`() = runTest {
        val cache = MemoryCache()
        val delegate = MemoryCacheSaver<PagingState<Int, Int>>(key = "paging-state", memoryCache = cache)
        var failFirstWrite = true
        val stateSaver = object : CacheSaver<PagingState<Int, Int>> by delegate {
            override suspend fun saveCache(data: PagingState<Int, Int>) {
                delegate.saveCache(data)
                if (failFirstWrite) {
                    failFirstWrite = false
                    error("paging state write failed after changing the backing cache")
                }
            }
        }
        var pageRequests = 0
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pagingStateSaver = stateSaver,
            pageLoader = PageLoader { cursor ->
                check(cursor == null)
                pageRequests += 1
                Page(items = listOf(10, 9), nextCursor = 8)
            }
        )

        assertFailsWith<IllegalStateException> { repository.loadPage(null) }
        assertEquals(PagingState<Int, Int>(), cache.get<PagingState<Int, Int>>("paging-state"))
        assertEquals(listOf(10, 9), cache.get<List<Int>>("items"))

        val retriedPage = repository.loadPage(null)

        assertEquals(listOf(10, 9), retriedPage.items)
        assertEquals(8, cache.get<PagingState<Int, Int>>("paging-state")?.nextCursor)
        assertEquals(listOf(10, 9), cache.get<List<Int>>("items"))
        assertEquals(2, pageRequests)
    }

    @Test
    fun `loadPage accepts opaque cursors without ordering assumptions`() = runTest {
        val cache = MemoryCache()
        val baseSaver = MemoryListCacheSaver<String, String>(
            key = "opaque-items",
            memoryCache = cache,
            isSameId = { id -> this == id }
        )
        val pagingSaver = PagingCacheSaver(baseSaver) { cached, incoming ->
            val incomingIds = incoming.toSet()
            (cached.orEmpty().filterNot { it in incomingIds } + incoming).distinct()
        }
        val crudRepository = CrudRepository<String, String, String, String, String, String, String>(
            dataSource = mockk<CrudDataSource<String, String, String, String>>(relaxed = true),
            dispatcher = UnconfinedTestDispatcher(),
            cacheSaver = pagingSaver,
            toDomain = { this },
            updateToDto = { this },
            createToDto = { this },
            updaterCoroutineScope = CoroutineScope(SupervisorJob())
        )
        val repository = PagingCrudRepository<String, String, String, String, String, String, String, String>(
            crudRepository = crudRepository,
            cacheSaver = pagingSaver,
            pagingStateSaver = MemoryCacheSaver<PagingState<String, String>>(
                key = "opaque-paging-state",
                memoryCache = cache
            ),
            idOf = { this },
            pageLoader = PageLoader { cursor ->
                when (cursor) {
                    null -> Page(items = listOf("first"), nextCursor = "token-a")
                    "token-a" -> Page(items = listOf("second"), nextCursor = "token-z")
                    "token-z" -> Page(items = listOf("third"), nextCursor = null)
                    else -> error("Unexpected opaque cursor: $cursor")
                }
            }
        )

        val first = repository.loadPage(null)
        val second = repository.loadPage(first.nextCursor)
        val third = repository.loadPage(second.nextCursor)

        assertEquals(listOf("first"), first.items)
        assertEquals("token-a", first.nextCursor)
        assertEquals(listOf("first", "second"), second.items)
        assertEquals("token-z", second.nextCursor)
        assertEquals(listOf("first", "second", "third"), third.items)
        assertEquals(null, third.nextCursor)
    }

    @Test
    fun `loadPage rejects a cursor cycle that spans multiple pages`() = runTest {
        val cache = MemoryCache()
        val pages: Map<Int?, Page<Int, Int>> = mapOf(
            null to Page(items = listOf(30, 20), nextCursor = 10),
            10 to Page(items = listOf(10), nextCursor = 20),
            20 to Page(items = listOf(5), nextCursor = 10)
        )
        val repository = repository(cache = cache, pages = pages)

        repository.loadPage(null)
        repository.loadPage(10)

        assertFailsWith<IllegalStateException> { repository.loadPage(20) }
        assertEquals(20, cache.get<PagingState<Int, Int>>("paging-state")?.nextCursor)
        assertEquals(listOf(30, 20, 10), cache.get<List<Int>>("items"))
    }

    @Test
    fun `loadPage rejects an empty page that advertises a next cursor`() = runTest {
        val cache = MemoryCache()
        val repository = repository(
            cache = cache,
            pages = mapOf(null to Page(items = emptyList(), nextCursor = 10))
        )

        assertFailsWith<IllegalStateException> {
            repository.loadPage(null)
        }

        assertEquals(null, cache.get<List<Int>>("items"))
    }

    @Test
    fun `page merges preserve older cached items and ordering while replacing duplicates`() = runTest {
        val cache = MemoryCache()
        cache.put("items", listOf(5, 4))
        val repository = repository(
            cache = cache,
            pages = mapOf(
                null to Page(items = listOf(10, 9), nextCursor = 8),
                8 to Page(items = listOf(8, 5), nextCursor = null)
            )
        )

        repository.loadPage(null)
        val cacheAfterFirstPage = cache.get<List<Int>>("items")
        repository.loadPage(8)
        val cacheAfterSecondPage = cache.get<List<Int>>("items")

        assertEquals(listOf(10, 9, 5, 4), cacheAfterFirstPage)
        assertEquals(listOf(10, 9, 8, 5, 4), cacheAfterSecondPage)
    }

    @Test
    fun `invalid cursor does not mutate paging state and can be retried`() = runTest {
        val cache = MemoryCache()
        val baseSaver = MemoryListCacheSaver<Int, Int>(
            key = "retry-items",
            memoryCache = cache,
            isSameId = { id -> this == id }
        )
        val pagingSaver = PagingCacheSaver(baseSaver) { cached, incoming ->
            val incomingIds = incoming.toSet()
            (cached.orEmpty().filterNot { it in incomingIds } + incoming).sortedDescending()
        }
        val crudRepository = CrudRepository<Int, Int, Int, Int, Int, Int, Int>(
            dataSource = mockk(relaxed = true),
            dispatcher = UnconfinedTestDispatcher(),
            cacheSaver = pagingSaver,
            toDomain = { this },
            updateToDto = { this },
            createToDto = { this },
            updaterCoroutineScope = CoroutineScope(SupervisorJob())
        )
        var secondPageAttempts = 0
        val repository = PagingCrudRepository<Int, Int, Int, Int, Int, Int, Int, Int>(
            crudRepository = crudRepository,
            cacheSaver = pagingSaver,
            pagingStateSaver = MemoryCacheSaver<PagingState<Int, Int>>(
                key = "retry-paging-state",
                memoryCache = cache
            ),
            idOf = { this },
            pageLoader = PageLoader { cursor ->
                when (cursor) {
                    null -> Page(items = listOf(20, 10), nextCursor = 10)
                    10 -> {
                        secondPageAttempts += 1
                        if (secondPageAttempts == 1) {
                            Page(items = listOf(10), nextCursor = 10)
                        } else {
                            Page(items = listOf(10, 5), nextCursor = 5)
                        }
                    }
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        repository.loadPage(null)
        kotlin.test.assertFailsWith<IllegalStateException> {
            repository.loadPage(10)
        }
        val retry = repository.loadPage(10)

        assertEquals(listOf(20, 10, 5), retry.items)
        assertEquals(5, retry.nextCursor)
        assertEquals(2, secondPageAttempts)
    }

    @Test
    fun `create does not trigger a full-list refresh but still inserts into the shared cache`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Int, Int, Int>>(relaxed = true)
        coEvery { dataSource.create(any()) } returns 42
        val repository = repository(
            cache = cache,
            pages = mapOf(null to Page(items = listOf(10, 9), nextCursor = 8)),
            dataSource = dataSource
        )

        repository.loadPage(null)
        assertEquals(listOf(10, 9), cache.get<List<Int>>("items"))

        val result = repository.create(42).first { it is Content.Data }

        assertEquals(42, (result as Content.Data).data)
        assertEquals(listOf(42, 10, 9), cache.get<List<Int>>("items"))
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `update does not trigger a full-list refresh but still updates the shared cache`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Int, Int, Int>>(relaxed = true)
        coEvery { dataSource.update(any(), any()) } returns 99
        val repository = repository(
            cache = cache,
            pages = mapOf(null to Page(items = listOf(10, 9), nextCursor = 8)),
            dataSource = dataSource
        )

        repository.loadPage(null)
        assertEquals(listOf(10, 9), cache.get<List<Int>>("items"))

        val result = repository.update(10, 99).first { it is Content.Data }

        assertEquals(99, (result as Content.Data).data)
        assertEquals(listOf(99, 9), cache.get<List<Int>>("items"))
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `remove does not trigger a full-list refresh but still removes from the shared cache`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Int, Int, Int>>(relaxed = true)
        coEvery { dataSource.remove(any()) } returns Unit
        val repository = repository(
            cache = cache,
            pages = mapOf(null to Page(items = listOf(10, 9), nextCursor = 8)),
            dataSource = dataSource
        )

        repository.loadPage(null)
        assertEquals(listOf(10, 9), cache.get<List<Int>>("items"))

        val result = repository.remove(10).first { it is Content.Data }

        assertEquals(Unit, (result as Content.Data).data)
        assertEquals(listOf(9), cache.get<List<Int>>("items"))
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `observeAllPages emits incomplete progress and only emits Data after the final page`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        val dataSource = mockk<CrudDataSource<Int, Int, Int, Int>>(relaxed = true)
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            dataSource = dataSource,
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                when (cursor) {
                    null -> Page(items = listOf(100, 10), nextCursor = 40)
                    40 -> Page(items = listOf(8, 10, 2), nextCursor = 3)
                    3 -> Page(items = listOf(1), nextCursor = null)
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val emissions = repository.observeAllPages().toList()

        assertEquals(listOf(null, 40, 3), requestedCursors)
        assertEquals(4, emissions.size)
        assertTrue(emissions[0] is Content.Loading)
        assertEquals(null, (emissions[0] as Content.Loading<List<Int>>).data)
        assertEquals(listOf(100, 10), (emissions[1] as Content.Loading<List<Int>>).data)
        assertEquals(listOf(100, 10, 8, 2), (emissions[2] as Content.Loading<List<Int>>).data)
        assertEquals(listOf(100, 10, 8, 2, 1), (emissions[3] as Content.Data<List<Int>>).data)
        assertEquals(listOf(100, 10, 8, 2, 1), cache.get<List<Int>>("items"))
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `observeAllPages emits cached values immediately but starts a fresh scan at null cursor`() = runTest {
        val cache = MemoryCache()
        cache.put("items", listOf(99))
        val requestedCursors = mutableListOf<Int?>()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                check(cursor == null)
                Page(items = listOf(10), nextCursor = null)
            }
        )

        val emissions = repository.observeAllPages().toList()

        assertEquals(listOf<Int?>(null), requestedCursors)
        assertEquals(listOf(99), (emissions.first() as Content.Loading<List<Int>>).data)
        assertEquals(listOf(10), (emissions.last() as Content.Data<List<Int>>).data)
    }

    @Test
    fun `observeAllPages persists progress for subsequent page requests`() = runTest {
        val cache = MemoryCache()
        val requestedCursors = mutableListOf<Int?>()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader { cursor ->
                requestedCursors.add(cursor)
                when (cursor) {
                    null -> Page(items = listOf(20, 10), nextCursor = 7)
                    7 -> Page(items = listOf(5, 1), nextCursor = null)
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val firstPage = repository.loadPage(null)
        val fullScan = repository.observeAllPages().toList()
            .last() as Content.Data<List<Int>>
        val nextPage = repository.loadNextPage()

        assertEquals(listOf(20, 10), firstPage.items)
        assertEquals(listOf(20, 10, 5, 1), fullScan.data)
        assertEquals(listOf(20, 10, 5, 1), nextPage.items)
        assertEquals(null, nextPage.nextCursor)
        assertEquals(listOf<Int?>(null, null, 7), requestedCursors)
    }

    @Test
    fun `observeAllPages publishes partial data and reports an error when a later page fails`() = runTest {
        val cache = MemoryCache()
        cache.put("items", listOf(99))
        var failSecondPage = true
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader { cursor ->
                when (cursor) {
                    null -> Page(items = listOf(10), nextCursor = 5)
                    5 -> {
                        if (failSecondPage) {
                            failSecondPage = false
                            error("Temporary page failure")
                        }
                        Page(items = listOf(3), nextCursor = null)
                    }
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val failedEmissions = repository.observeAllPages().toList()

        assertEquals(listOf(99), (failedEmissions.first() as Content.Loading<List<Int>>).data)
        assertEquals(listOf(10), (failedEmissions[1] as Content.Loading<List<Int>>).data)
        assertTrue(failedEmissions.last() is Content.Error)
        assertEquals(listOf(99, 10), cache.get<List<Int>>("items"))

        val retryEmissions = repository.observeAllPages().toList()
        assertEquals(listOf(10, 3), (retryEmissions.last() as Content.Data<List<Int>>).data)
        assertEquals(listOf(99, 10, 3), cache.get<List<Int>>("items"))
    }

    @Test
    fun `observeAllPages rejects repeating cursors and keeps the last partial cache result`() = runTest {
        val cache = MemoryCache()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader { cursor ->
                when (cursor) {
                    null -> Page(items = listOf(20), nextCursor = 10)
                    10 -> Page(items = listOf(8), nextCursor = 10)
                    else -> error("Unexpected cursor: $cursor")
                }
            }
        )

        val emissions = repository.observeAllPages().toList()

        assertEquals(listOf(20), (emissions[1] as Content.Loading<List<Int>>).data)
        assertTrue(emissions.last() is Content.Error)
        assertEquals(listOf(20), cache.get<List<Int>>("items"))
    }

    @Test
    fun `observeAllPages rejects an empty page with a next cursor`() = runTest {
        val cache = MemoryCache()
        val repository = repository(
            cache = cache,
            pages = emptyMap(),
            pageLoader = PageLoader {
                Page(items = emptyList(), nextCursor = 10)
            }
        )

        val emissions = repository.observeAllPages().toList()

        assertTrue(emissions.first() is Content.Loading)
        assertTrue(emissions.last() is Content.Error)
        assertEquals(null, cache.get<List<Int>>("items"))
    }

}
