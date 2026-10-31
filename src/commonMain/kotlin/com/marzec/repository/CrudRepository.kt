package com.marzec.repository

import com.marzec.cache.Cache
import com.marzec.cache.CompositeUpdatableCacheSaver
import com.marzec.cache.FileCache
import com.marzec.cache.FileCacheSaver
import com.marzec.cache.ListCacheSaver
import com.marzec.cache.ManyItemsCacheSaver
import com.marzec.cache.PagingCacheSaver
import com.marzec.cache.MemoryCacheSaver
import com.marzec.cache.atFirstPositionInserter
import com.marzec.cache.map
import com.marzec.cache.toReversed
import com.marzec.cache.withInserter
import com.marzec.cache.synchronized
import com.marzec.cache.UpdatableCacheSaver
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.asContentFlow
import com.marzec.datasource.CrudDataSource
import com.marzec.datasource.CrudDataSourceImpl
import com.marzec.datasource.EndpointProviderImpl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext
import kotlin.reflect.typeOf

/**
 * Emitted by [CrudRepository.observeById] when an item that was present in the local cache
 * disappears from that cache.
 */
class CachedItemRemovedException(id: Any?) :
    NoSuchElementException("Item with id '$id' was removed from the local cache")

class CrudRepository<ID, MODEL : Any, CREATE : Any, UPDATE : Any, MODEL_DTO : Any, CREATE_DTO : Any, UPDATE_DTO : Any>(
    private val dataSource: CrudDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val dispatcher: CoroutineDispatcher,
    private val cacheSaver: ManyItemsCacheSaver<ID, MODEL>,
    private val toDomain: MODEL_DTO.() -> MODEL,
    private val updateToDto: UPDATE.() -> UPDATE_DTO,
    private val createToDto: CREATE.() -> CREATE_DTO,
    private val updaterCoroutineScope: CoroutineScope,
    private val onUpdate: List<suspend () -> Unit> = emptyList(),
    private val cacheExpirationMode: CacheExpirationMode = CacheExpirationMode.AlwaysExpired
) {
    enum class RefreshPolicy {
        NO_REFRESH, SEPARATE_DISPATCHER, BLOCKING
    }

    /**
     * Observes the local collection and synchronizes it from the API.
     *
     * The cache is the only source of model emissions: a successful response is persisted first,
     * then appears through the cache observer. A failed sync is emitted as Content.Error without
     * clearing cached models.
     */
    suspend fun observeAll(): Flow<Content<List<MODEL>>> = observeLocalAndSync(
        getLocal = { cacheSaver.get() },
        observeLocal = { cacheSaver.observeCached() },
        refresh = { refreshAll() }
    )

    /**
     * Observes one locally cached model and synchronizes that ID from the API. The API response
     * is saved into the shared collection cache before it can be emitted as model data.
     */
    suspend fun observeById(id: ID): Flow<Content<MODEL>> = observeLocalAndSync(
        getLocal = { cacheSaver.getById(id) },
        observeLocal = { cacheSaver.observeCachedById(id) },
        refresh = { refreshById(id) },
        missingError = { CachedItemRemovedException(id) }
    )

    /**
     * Fetches the current remote collection and persists it before returning Data.
     * A failed refresh never clears the existing cache.
     */
    suspend fun refreshAll(): Content<List<MODEL>> = refreshWithCacheFallback(
        request = { loadAll().also { cacheSaver.saveCache(it) } },
        cached = { cacheSaver.get() }
    )

    /**
     * Fetches one model and merges it into the collection cache without replacing other cached
     * items. This is safe for repositories backed by paged collections.
     */
    suspend fun refreshById(id: ID): Content<MODEL> = refreshWithCacheFallback(
        request = {
            dataSource.getById(id).toDomain().also { model ->
                if (cacheSaver.getById(id) == null) {
                    cacheSaver.addItem(model)
                } else {
                    cacheSaver.updateItem(id, model)
                }
            }
        },
        cached = { cacheSaver.getById(id) }
    )

    private fun <T : Any> observeLocalAndSync(
        getLocal: suspend () -> T?,
        observeLocal: suspend () -> Flow<T?>,
        refresh: suspend () -> Content<T>,
        missingError: (() -> Throwable)? = null
    ): Flow<Content<T>> = channelFlow {
        // Publish local state before starting the request so cached data is immediately available.
        var hasObservedData = false
        var missingContent: Content<T>? = null
        val initial = getLocal()
        if (initial != null) {
            hasObservedData = true
        }
        send(initial.toLocalContent())

        launch {
            val syncResult = refresh()
            if (syncResult is Content.Error) {
                send(Content.Error<T>(syncResult.exception))
            }
        }

        observeLocal().collect { value ->
            val content = when {
                value != null -> {
                    hasObservedData = true
                    missingContent = null
                    Content.Data(value)
                }

                hasObservedData && missingError != null ->
                    missingContent ?: Content.Error<T>(missingError()).also { missingContent = it }

                else -> Content.Loading()
            }
            send(content)
        }
    }.distinctUntilChanged().flowOn(dispatcher)

    private fun <T : Any> T?.toLocalContent(): Content<T> =
        this?.let { Content.Data(it) } ?: Content.Loading()

    private suspend fun <T : Any> refreshWithCacheFallback(
        request: suspend () -> T,
        cached: suspend () -> T?
    ): Content<T> {
        val result = asContent(request)
        if (result is Content.Error && cacheExpirationMode == CacheExpirationMode.AlwaysUpToDate) {
            val cachedResult = asContent(cached)
            if (cachedResult is Content.Data && cachedResult.data != null) {
                return Content.Data(cachedResult.data)
            }
        }
        return result
    }

    suspend fun remove(
        id: ID, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<Unit>> = asContentFlow {
        dataSource.remove(id)
        cacheSaver.removeItem(id)
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun update(
        id: ID, model: UPDATE, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<MODEL>> = asContentFlow {
        val updatedModel = dataSource.update(id, model.updateToDto()).toDomain()
        cacheSaver.updateItem(id, updatedModel)
        updatedModel
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun create(
        create: CREATE, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<MODEL>> = asContentFlow {
        val createdModel = dataSource.create(create.createToDto()).toDomain()
        cacheSaver.addItem(createdModel)
        createdModel
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun <T> Flow<Content<T>>.triggerUpdateIfNeeded(policy: RefreshPolicy): Flow<Content<T>> =
        onEach {
            if (it is Content.Data) {
                when (policy) {
                    RefreshPolicy.NO_REFRESH -> Unit
                    RefreshPolicy.SEPARATE_DISPATCHER -> updaterCoroutineScope.launch {
                        runRefreshing()
                    }

                    RefreshPolicy.BLOCKING -> runRefreshing()
                }
            }
        }

    private suspend fun runRefreshing() = with(CoroutineScope(SupervisorJob() + coroutineContext)) {
        val updaters = listOf<suspend () -> Unit> { refreshAll() } + onUpdate

        updaters.forEach {
            launch {
                it()
            }
        }
    }

    private suspend fun loadAll() = dataSource.getAll().map(toDomain)
}

inline fun <
        ID,
        reified MODEL : Any,
        CREATE : Any,
        UPDATE : Any,
        reified MODEL_DTO : Any,
        reified CREATE_DTO : Any,
        reified UPDATE_DTO : Any
        > fileAndMemoryCacheCrudRepository(
    dispatcher: CoroutineDispatcher,
    client: HttpClient,
    updaterCoroutineScope: CoroutineScope,
    endpoint: String,
    fileCache: FileCache,
    memoryCache: Cache,
    reversed: Boolean = false,
    noinline isSameId: MODEL.(id: ID) -> Boolean,
    noinline toDomain: MODEL_DTO.() -> MODEL,
    noinline toDto: MODEL.() -> MODEL_DTO,
    noinline createToDto: CREATE.() -> CREATE_DTO,
    noinline updateToDto: UPDATE.() -> UPDATE_DTO,
    noinline inserter: List<MODEL>?.(MODEL) -> MutableList<MODEL> = atFirstPositionInserter(),
    dataSource: CrudDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>? = null,
    cacheExpirationMode: CacheExpirationMode = CacheExpirationMode.AlwaysExpired
): CrudRepository<ID, MODEL, CREATE, UPDATE, MODEL_DTO, CREATE_DTO, UPDATE_DTO> {
    val cacheSaver = createFileAndMemoryCacheSaver(
        endpoint = endpoint,
        fileCache = fileCache,
        memoryCache = memoryCache,
        reversed = reversed,
        isSameId = isSameId,
        toDomain = toDomain,
        toDto = toDto,
        inserter = inserter
    )
    return CrudRepository(
        dataSource = dataSource ?: createDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>(
            endpoint,
            client
        ),
        cacheSaver = cacheSaver,
        dispatcher = dispatcher,
        toDomain = toDomain,
        createToDto = createToDto,
        updateToDto = updateToDto,
        updaterCoroutineScope = updaterCoroutineScope,
        cacheExpirationMode = cacheExpirationMode
    )
}

@PublishedApi
internal inline fun <reified MODEL : Any, reified MODEL_DTO : Any> createFileAndMemoryUpdatableCacheSaver(
    endpoint: String,
    fileCache: FileCache,
    memoryCache: Cache,
    noinline toDomain: MODEL_DTO.() -> MODEL,
    noinline toDto: MODEL.() -> MODEL_DTO
): UpdatableCacheSaver<List<MODEL>> =
    CompositeUpdatableCacheSaver(
        savers = listOf(
            MemoryCacheSaver(
                key = endpoint,
                memoryCache = memoryCache
            ),
            FileCacheSaver(
                key = endpoint,
                fileCache = fileCache,
                serializer = serializer(typeOf<List<MODEL_DTO>>()) as KSerializer<List<MODEL_DTO>>
            ).map(
                toNested = { map { it.toDto() } },
                toTarget = { map { it.toDomain() } }
            )
        )
    )

@PublishedApi
internal inline fun <ID, reified MODEL : Any, reified MODEL_DTO : Any> createFileAndMemoryCacheSaver(
    endpoint: String,
    fileCache: FileCache,
    memoryCache: Cache,
    reversed: Boolean,
    noinline isSameId: MODEL.(id: ID) -> Boolean,
    noinline toDomain: MODEL_DTO.() -> MODEL,
    noinline toDto: MODEL.() -> MODEL_DTO,
    noinline inserter: List<MODEL>?.(MODEL) -> MutableList<MODEL>
): ManyItemsCacheSaver<ID, MODEL> =
    ListCacheSaver(
        cacheSaver = createFileAndMemoryUpdatableCacheSaver(
            endpoint = endpoint,
            fileCache = fileCache,
            memoryCache = memoryCache,
            toDomain = toDomain,
            toDto = toDto
        ),
        isSameId = isSameId
    ).withInserter(inserter)
        .let {
            if (reversed) {
                it.toReversed()
            } else {
                it
            }
        }.synchronized()

/**
 * Creates a paging repository and its CRUD delegate around one shared cache saver.
 *
 * [mergePageItems] explicitly defines duplicate replacement and ordering for a whole page.
 * It is separate from [inserter], which applies only to individual CRUD creates.
 * Each page merge uses the same underlying cache saver as the CRUD delegate. [cacheKey] identifies
 * the collection/query and must differ for different filters, sort orders, or account scopes.
 * The paging state is stored separately under a derived key.
 */
@Suppress("LongParameterList", "detekt.LongParameterList")
inline fun <
        reified ID,
        reified MODEL : Any,
        CREATE : Any,
        UPDATE : Any,
        reified MODEL_DTO : Any,
        reified CREATE_DTO : Any,
        reified UPDATE_DTO : Any,
        reified CURSOR
        > fileAndMemoryCachePagingCrudRepository(
    dispatcher: CoroutineDispatcher,
    client: HttpClient,
    updaterCoroutineScope: CoroutineScope,
    endpoint: String,
    cacheKey: String = endpoint,
    fileCache: FileCache,
    memoryCache: Cache,
    noinline isSameId: MODEL.(id: ID) -> Boolean,
    noinline idOf: MODEL.() -> ID,
    noinline toDomain: MODEL_DTO.() -> MODEL,
    noinline toDto: MODEL.() -> MODEL_DTO,
    noinline createToDto: CREATE.() -> CREATE_DTO,
    noinline updateToDto: UPDATE.() -> UPDATE_DTO,
    noinline mergePageItems: (List<MODEL>?, List<MODEL>) -> List<MODEL>,
    pageLoader: PageLoader<CURSOR, MODEL>,
    noinline inserter: List<MODEL>?.(MODEL) -> MutableList<MODEL> = atFirstPositionInserter(),
    dataSource: CrudDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>? = null,
    pagingPolicy: Paging = Paging.OnDemand,
    cacheExpirationMode: CacheExpirationMode = CacheExpirationMode.AlwaysExpired
): PagingCrudRepository<ID, MODEL, CREATE, UPDATE, MODEL_DTO, CREATE_DTO, UPDATE_DTO, CURSOR> {
    val baseCacheSaver = ListCacheSaver(
        cacheSaver = createFileAndMemoryUpdatableCacheSaver(
            endpoint = cacheKey,
            fileCache = fileCache,
            memoryCache = memoryCache,
            toDomain = toDomain,
            toDto = toDto
        ),
        isSameId = isSameId
    ).withInserter(inserter)
    val pagingCacheSaver = PagingCacheSaver(
        saver = baseCacheSaver,
        mergePages = mergePageItems
    )
    val pagingStateSaver = FileCacheSaver(
        key = "$cacheKey::paging-state",
        fileCache = fileCache,
        serializer = serializer(typeOf<PagingState<ID, CURSOR>>()) as KSerializer<PagingState<ID, CURSOR>>
    )
    val crudRepository = CrudRepository(
        dataSource = dataSource ?: createDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>(
            endpoint,
            client
        ),
        cacheSaver = pagingCacheSaver,
        dispatcher = dispatcher,
        toDomain = toDomain,
        createToDto = createToDto,
        updateToDto = updateToDto,
        updaterCoroutineScope = updaterCoroutineScope,
        cacheExpirationMode = cacheExpirationMode
    )
    return PagingCrudRepository(
        crudRepository = crudRepository,
        cacheSaver = pagingCacheSaver,
        pagingStateSaver = pagingStateSaver,
        idOf = idOf,
        pageLoader = pageLoader,
        pagingPolicy = pagingPolicy
    )
}

inline fun <ID, reified MODEL_DTO : Any, reified CREATE_DTO : Any, reified UPDATE_DTO : Any> createDataSource(
    endpoint: String,
    client: HttpClient,
    json: Json = Json { ignoreUnknownKeys = true }
): CrudDataSourceImpl<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO> =
    CrudDataSource(
        endpointProvider = EndpointProviderImpl(endpoint),
        client = client,
        json = json
    )
