package com.marzec.repository

import com.marzec.cache.MemoryCache
import com.marzec.cache.MemoryListCacheSaver
import com.marzec.content.Content
import com.marzec.logger.Logger
import com.marzec.datasource.CrudDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CrudRepositoryTest {

    private data class Model(val id: Int, val value: String)

    private fun repository(
        dataSource: CrudDataSource<Int, Model, Model, Model>,
        cache: MemoryCache,
        cacheExpirationMode: CacheExpirationMode = CacheExpirationMode.AlwaysExpired
    ): CrudRepository<Int, Model, Model, Model, Model, Model, Model> {
        Logger.logger = mockk(relaxed = true)
        val saver = MemoryListCacheSaver<Int, Model>(
            key = "models",
            memoryCache = cache,
            isSameId = { id -> this.id == id }
        )
        return CrudRepository(
            dataSource = dataSource,
            dispatcher = UnconfinedTestDispatcher(),
            cacheSaver = saver,
            toDomain = { this },
            updateToDto = { this },
            createToDto = { this },
            updaterCoroutineScope = CoroutineScope(SupervisorJob()),
            cacheExpirationMode = cacheExpirationMode
        )
    }

    @Test
    fun `observeAll emits local cache first then the synchronized cache value`() = runTest {
        val cache = MemoryCache()
        val initial = listOf(Model(1, "local"))
        val remote = listOf(Model(2, "remote"), Model(3, "new"))
        cache.put("models", initial)
        val refreshGate = CompletableDeferred<List<Model>>()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getAll() } coAnswers { refreshGate.await() }
        val repository = repository(dataSource, cache)

        val emissions = async { repository.observeAll().take(2).toList() }
        runCurrent()
        refreshGate.complete(remote)
        advanceUntilIdle()

        assertEquals(listOf(Content.Data(initial), Content.Data(remote)), emissions.await())
        assertEquals(remote, cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.getAll() }
    }

    @Test
    fun `observeById emits cached state then persists the canonical API model`() = runTest {
        val cache = MemoryCache()
        val old = Model(7, "local")
        val remote = Model(7, "remote")
        cache.put("models", listOf(old, Model(8, "other")))
        val refreshGate = CompletableDeferred<Model>()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getById(7) } coAnswers { refreshGate.await() }
        val repository = repository(dataSource, cache)

        val emissions = async { repository.observeById(7).take(2).toList() }
        runCurrent()
        refreshGate.complete(remote)
        advanceUntilIdle()

        assertEquals(listOf(Content.Data(old), Content.Data(remote)), emissions.await())
        assertEquals(listOf(remote, Model(8, "other")), cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.getById(7) }
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `refreshAll saves successful server data before returning it`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val remote = listOf(Model(3, "remote"), Model(4, "new"))
        coEvery { dataSource.getAll() } returns remote
        val repository = repository(dataSource, cache)

        val result = repository.refreshAll()

        assertEquals(Content.Data(remote), result)
        assertEquals(remote, cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.getAll() }
    }

    @Test
    fun `observeAll reports sync failure without clearing cached models`() = runTest {
        val cache = MemoryCache()
        val cached = listOf(Model(1, "cached"))
        cache.put("models", cached)
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getAll() } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache)

        val emissions = repository.observeAll().take(2).toList()

        assertEquals(Content.Data(cached), emissions.first())
        assertIs<Content.Error<List<Model>>>(emissions.last())
        assertEquals(cached, cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.getAll() }
    }

    @Test
    fun `failed refresh reports error under AlwaysExpired and preserves cached data`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val cached = listOf(Model(1, "cached"))
        cache.put("models", cached)
        coEvery { dataSource.getAll() } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache, CacheExpirationMode.AlwaysExpired)

        val result = repository.refreshAll()

        assertIs<Content.Error<List<Model>>>(result)
        assertEquals(cached, cache.get<List<Model>>("models"))
    }

    @Test
    fun `failed refresh returns cached data under AlwaysUpToDate`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val cached = listOf(Model(1, "cached"))
        cache.put("models", cached)
        coEvery { dataSource.getAll() } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache, CacheExpirationMode.AlwaysUpToDate)

        val result = repository.refreshAll()

        assertEquals(Content.Data(cached), result)
        assertEquals(cached, cache.get<List<Model>>("models"))
    }

    @Test
    fun `refreshById updates only the requested local model`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val old = Model(7, "stale")
        val other = Model(8, "unchanged")
        val remote = Model(7, "fresh")
        cache.put("models", listOf(old, other))
        coEvery { dataSource.getById(7) } returns remote
        val repository = repository(dataSource, cache)

        val result = repository.refreshById(7)

        assertEquals(Content.Data(remote), result)
        assertEquals(listOf(remote, other), cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.getById(7) }
        coVerify(exactly = 0) { dataSource.getAll() }
    }

    @Test
    fun `create stores and returns the canonical server model`() = runTest {
        val cache = MemoryCache()
        val other = Model(2, "other")
        cache.put("models", listOf(other))
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val request = Model(0, "request")
        val created = Model(1, "canonical")
        coEvery { dataSource.create(request) } returns created
        val repository = repository(dataSource, cache)

        val emissions = repository.create(request, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(listOf(Content.Loading(), Content.Data(created)), emissions)
        assertEquals(listOf(created, other), cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.create(request) }
    }


    @Test
    fun `failed create leaves cache unchanged`() = runTest {
        val cache = MemoryCache()
        val cached = listOf(Model(2, "cached"))
        cache.put("models", cached)
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val request = Model(0, "request")
        coEvery { dataSource.create(request) } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache)

        val emissions = repository.create(request, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(Content.Loading(), emissions.first())
        assertIs<Content.Error<Model>>(emissions.last())
        assertEquals(cached, cache.get<List<Model>>("models"))
    }

    @Test
    fun `update stores and returns the canonical server model`() = runTest {
        val cache = MemoryCache()
        val old = Model(1, "old")
        val other = Model(2, "other")
        cache.put("models", listOf(old, other))
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val request = Model(1, "request")
        val updated = Model(1, "canonical")
        coEvery { dataSource.update(1, request) } returns updated
        val repository = repository(dataSource, cache)

        val emissions = repository.update(1, request, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(listOf(Content.Loading(), Content.Data(updated)), emissions)
        assertEquals(listOf(updated, other), cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.update(1, request) }
    }


    @Test
    fun `failed update leaves cached model unchanged`() = runTest {
        val cache = MemoryCache()
        val cached = listOf(Model(1, "cached"), Model(2, "other"))
        cache.put("models", cached)
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        val request = Model(1, "request")
        coEvery { dataSource.update(1, request) } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache)

        val emissions = repository.update(1, request, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(Content.Loading(), emissions.first())
        assertIs<Content.Error<Model>>(emissions.last())
        assertEquals(cached, cache.get<List<Model>>("models"))
    }

    @Test
    fun `remove mutates cache only after server success`() = runTest {
        val cache = MemoryCache()
        val removed = Model(1, "remove")
        val other = Model(2, "keep")
        cache.put("models", listOf(removed, other))
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.remove(1) } returns Unit
        val repository = repository(dataSource, cache)

        val emissions = repository.remove(1, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(listOf(Content.Loading(), Content.Data(Unit)), emissions)
        assertEquals(listOf(other), cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.remove(1) }
    }

    @Test
    fun `failed remove leaves cache unchanged`() = runTest {
        val cache = MemoryCache()
        val cached = listOf(Model(1, "cached"), Model(2, "other"))
        cache.put("models", cached)
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.remove(1) } throws IllegalStateException("offline")
        val repository = repository(dataSource, cache)

        val emissions = repository.remove(1, CrudRepository.RefreshPolicy.NO_REFRESH).toList()

        assertEquals(Content.Loading(), emissions.first())
        assertIs<Content.Error<Unit>>(emissions.last())
        assertEquals(cached, cache.get<List<Model>>("models"))
        coVerify(exactly = 1) { dataSource.remove(1) }
    }


    @Test
    fun `observeById treats an initial cache miss as loading`() = runTest {
        val cache = MemoryCache()
        val remote = Model(1, "remote")
        val refreshGate = CompletableDeferred<Model>()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getById(1) } coAnswers { refreshGate.await() }
        val repository = repository(dataSource, cache)

        val observations = async { repository.observeById(1).take(2).toList() }
        runCurrent()
        refreshGate.complete(remote)
        advanceUntilIdle()

        assertEquals(listOf(Content.Loading(), Content.Data(remote)), observations.await())
        assertEquals(listOf(remote), cache.get<List<Model>>("models"))
    }

    @Test
    fun `active detail observer receives missing error when cached item is removed`() = runTest {
        val cache = MemoryCache()
        val old = Model(1, "old")
        val remote = Model(1, "remote")
        cache.put("models", listOf(old, Model(2, "other")))
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getById(1) } returns remote
        coEvery { dataSource.remove(1) } returns Unit
        val repository = repository(dataSource, cache)

        val observations = async {
            repository.observeById(1).take(3).toList()
        }
        runCurrent()
        repository.remove(1, CrudRepository.RefreshPolicy.NO_REFRESH).toList()
        advanceUntilIdle()

        val emissions = observations.await()
        assertEquals(Content.Data(old), emissions[0])
        assertEquals(Content.Data(remote), emissions[1])
        val missing = assertIs<Content.Error<Model>>(emissions[2])
        assertIs<CachedItemRemovedException>(missing.exception)
        assertEquals(listOf(Model(2, "other")), cache.get<List<Model>>("models"))
    }

    @Test
    fun `refreshAll rethrows cancellation`() = runTest {
        val cache = MemoryCache()
        val dataSource = mockk<CrudDataSource<Int, Model, Model, Model>>(relaxed = true)
        coEvery { dataSource.getAll() } throws CancellationException("cancelled")
        val repository = repository(dataSource, cache)

        assertFailsWith<CancellationException> { repository.refreshAll() }
    }
}
