package com.marzec.cache

import com.marzec.content.Content
import com.marzec.content.asContentFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

suspend fun <T : Any> cacheCall(
    key: String,
    dispatcher: CoroutineDispatcher,
    memoryCache: Cache,
    call: suspend () -> Content<T>,
    ignoreNetworkResult: Boolean = false
): Flow<Content<T>> =
    GetWithCacheCall(
        dispatcher = dispatcher,
        cacheSaver = MemoryCacheSaver(key, memoryCache),
        call = call,
        ignoreNetworkResult = ignoreNetworkResult
    ).run()

class GetWithCacheCall<T>(
    private val dispatcher: CoroutineDispatcher,
    private val cacheSaver: CacheSaver<T>,
    private val call: suspend () -> Content<T>,
    private val ignoreNetworkResult: Boolean = false
) {

    suspend fun run() = withContext(dispatcher) {
        val cached = cacheSaver.get()
        val initial: Content<T> = if (cached != null) {
            Content.Data(cached)
        } else {
            Content.Loading()
        }
        merge(
            flow {
                emit(initial)
                val callResult = call()
                if (callResult is Content.Error && !ignoreNetworkResult) {
                    emit(callResult)
                } else if (callResult is Content.Data) {
                    cacheSaver.saveCache(callResult.data)
                }
            },
            cacheSaver.observeCached()
                .filterNotNull()
                .map { Content.Data(it) as Content<T> },
        )
    }
        .distinctUntilChanged()
        .flowOn(dispatcher)
}

fun asContentWithListUpdate(
    dispatcher: CoroutineDispatcher,
    refreshCallback: suspend () -> Unit,
    request: suspend () -> Unit
) =
    asContentFlow(request)
        .onEach {
            if (it is Content.Data) {
                refreshCallback()
            }
        }.flowOn(dispatcher)
