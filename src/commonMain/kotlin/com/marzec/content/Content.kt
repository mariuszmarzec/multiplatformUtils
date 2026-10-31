package com.marzec.content

import com.marzec.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class Content<T> {

    data class Data<T>(val data: T) : Content<T>()
    data class Loading<T>(val data: T? = null) : Content<T>()
    data class Error<T>(val exception: Throwable) : Content<T>()
}

suspend fun <T> asContent(request: suspend () -> T): Content<T> {
    return try {
        Content.Data(request())
    } catch (expected: CancellationException) {
        throw expected
    } catch (expected: Exception) {
        Logger.log("Content", expected.message.toString(), expected)
        Content.Error(expected)
    }
}

suspend fun <T> flowContent(request: suspend () -> Flow<Content<T>>): Flow<Content<T>> {
    return try {
        request()
    } catch (expected: Exception) {
        flowOf(Content.Error(expected))
    }
}

fun <T> asContentFlow(request: suspend () -> T): Flow<Content<T>> {
    return flow {
        emit(Content.Loading())
        try {
            emit(Content.Data(request()))
        } catch (expected: Exception) {
            Logger.log("Content", expected.message.toString(), expected)
            emit(Content.Error<T>(expected))
        }
    }
}

fun <T, R> Content<T>.mapData(mapper: (T) -> R) = when (this) {
    is Content.Data -> try {
        Content.Data(mapper(this.data))
    } catch (expected: Exception) {
        Content.Error(expected)
    }

    is Content.Loading -> Content.Loading(this.data?.let(mapper))
    is Content.Error -> Content.Error(this.exception)
}

fun <T> Flow<Content<T>>.toContentUnit() = map { result -> result.mapData { Unit } }

suspend fun <T> Content<T>.ifDataSuspend(action: suspend Content.Data<T>.() -> Unit) =
    (this as? Content.Data)?.action()

suspend fun <T> Content<T>.ifFinished(action: suspend Content<T>.() -> Unit) {
    if (this !is Content.Loading) {
        action()
    }
}

fun <T> Content.Error<T>.getMessage(): String = exception.message.orEmpty()

fun <R> combineContents(vararg contents: Content<*>, mapData: (List<*>) -> R): Content<R> =
    with(contents.toList()) {
        val errorContent = firstOrNull { it is Content.Error<*> } as? Content.Error
        if (errorContent != null) {
            return Content.Error(errorContent.exception)
        }
        val loadingContent = firstOrNull { it is Content.Loading<*> } as? Content.Loading
        if (loadingContent != null) {
            return Content.Loading()
        }
        return Content.Data(mapData(filterIsInstance<Content.Data<*>>().map { it.data }))
    }

@Suppress("unchecked_cast", "MagicNumber")
fun <T1, T2, R> combineContentsFlows(
    flow: Flow<Content<T1>>,
    flow2: Flow<Content<T2>>,
    mapData: (T1, T2) -> R
) = combine(
    flow = flow,
    flow2 = flow2,
    transform = { content1, content2 ->
        combineContents(content1, content2) { dataList ->
            mapData(
                dataList[0] as T1,
                dataList[1] as T2
            )
        }
    }
)

@Suppress("unchecked_cast", "MagicNumber")
fun <T1, T2, T3, R> combineContentsFlows(
    flow: Flow<Content<T1>>,
    flow2: Flow<Content<T2>>,
    flow3: Flow<Content<T3>>,
    mapData: (T1, T2, T3) -> R
) = combine(
    flow = flow,
    flow2 = flow2,
    flow3 = flow3,
    transform = { content1, content2, content3 ->
        combineContents(content1, content2, content3) { dataList ->
            mapData(
                dataList[0] as T1,
                dataList[1] as T2,
                dataList[2] as T3
            )
        }
    }
)

@Suppress("unchecked_cast", "MagicNumber")
fun <T1, T2, T3, T4, R> combineContentsFlows(
    flow: Flow<Content<T1>>,
    flow2: Flow<Content<T2>>,
    flow3: Flow<Content<T3>>,
    flow4: Flow<Content<T4>>,
    mapData: (T1, T2, T3, T4) -> R
) = combine(
    flow = flow,
    flow2 = flow2,
    flow3 = flow3,
    flow4 = flow4,
    transform = { content1, content2, content3, content4 ->
        combineContents(content1, content2, content3, content4) { dataList ->
            mapData(
                dataList[0] as T1,
                dataList[1] as T2,
                dataList[2] as T3,
                dataList[3] as T4
            )
        }
    }
)

@Suppress("unchecked_cast", "MagicNumber")
fun <T1, T2, T3, T4, T5, R> combineContentsFlows(
    flow: Flow<Content<T1>>,
    flow2: Flow<Content<T2>>,
    flow3: Flow<Content<T3>>,
    flow4: Flow<Content<T4>>,
    flow5: Flow<Content<T5>>,
    mapData: (T1, T2, T3, T4, T5) -> R
) = combine(
    flow = flow,
    flow2 = flow2,
    flow3 = flow3,
    flow4 = flow4,
    flow5 = flow5,
    transform = { content1, content2, content3, content4, content5 ->
        combineContents(content1, content2, content3, content4, content5) { dataList ->
            mapData(
                dataList[0] as T1,
                dataList[1] as T2,
                dataList[2] as T3,
                dataList[3] as T4,
                dataList[4] as T5
            )
        }
    }
)

