package com.marzec.mvi

import com.marzec.content.Content
import com.marzec.content.getMessage

data class StateData<T>(
    val data: T,
    val loading: Boolean = false,
    val error: String? = null
)

fun <T, R> StateData<T>.reduceContentToLoadingWithNoChanges(result: Content<R>): StateData<T> =
    reduceDataWithContent(result) { this }

fun <T> StateData<T>.reduceData(
    reducer: T.() -> T
): StateData<T> = copy(this.data.reducer())

fun <T, R> StateData<T>.reduceDataWithContent(
    result: Content<R>,
    reducer: T.(Content.Data<R>) -> T
): StateData<T> =
    when (result) {
        is Content.Data -> copy(data = data.reducer(result), loading = false, error = null)
        is Content.Loading -> copy(loading = true, error = null)
        is Content.Error -> copy(loading = false, error = result.getMessage())
    }

fun <T, R> StateData<T>.mapData(mapper: (T) -> R) = mapper(this.data)

fun <T, R> StateData<T>.reduceContentAsSideAction(
    result: Content<R>,
    reducer: T.() -> T = { this }
): StateData<T> =
    when (result) {
        is Content.Data -> copy(data = data.reducer(), loading = false, error = null)
        is Content.Loading -> copy(loading = false, error = null)
        is Content.Error -> copy(loading = false, error = result.getMessage())
    }
