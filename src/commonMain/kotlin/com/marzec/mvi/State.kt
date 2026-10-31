package com.marzec.mvi

import com.marzec.extensions.asInstance
import com.marzec.content.Content

sealed class State<T>(open val data: T?) {

    data class Data<T>(override val data: T) : State<T>(data)
    data class Loading<T>(override val data: T? = null) : State<T>(data)
    data class Error<T>(override val data: T? = null, val message: String) : State<T>(data)

    inline fun asData(action: T.() -> Unit) =
        asInstance<Data<T>> {
            action(data)
        }

    inline fun <R> ifDataAvailable(
        blockOnLoading: Boolean = true,
        action: T.() -> R
    ) = data?.takeIf { !blockOnLoading || this !is Loading }?.let(action)
}

fun <T> State<T>.reduceData(
    reducer: T.() -> T
): State<T> =
    when (this) {
        is State.Data -> State.Data(this.data.reducer())
        is State.Loading -> copy(this.data?.reducer())
        is State.Error -> copy(this.data?.reducer())
    }

fun <T, R> State<T>.reduceWithResult(
    result: Content<R>?,
    defaultData: T? = null,
    reducer: T.(Content.Data<R>) -> T
): State<T> =
    when (result) {
        is Content.Data -> {
            State.Data((this.data ?: defaultData ?: throw IllegalStateException("State data is null")).reducer(result))
        }
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(
            data,
            result.exception.message.orEmpty()
        )

        else -> this
    }

fun <T, R> State<T>.reduceDataWithContent(
    result: Content<R>,
    reducer: T?.(R) -> T
): State<T> =
    when (result) {
        is Content.Data -> State.Data((this.data).reducer(result.data))
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(data, result.exception.message.orEmpty())
    }

fun <T, R> State<T>.reduceContentAsSideAction(
    result: Content<R>,
    reducer: T.() -> T = { this }
): State<T> =
    when (result) {
        is Content.Loading -> State.Loading(data?.let { it.reducer() })
        else -> {
            val data = data
            if (data != null) {
                State.Data(data.reducer())
            } else {
                State.Error(data, message = "No data available")
            }
        }
    }

fun <T, R> State<T>.reduceContentToLoadingWithNoChanges(result: Content<R>?): State<T> =
    when (result) {
        is Content.Data -> State.Loading(data)
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(
            data,
            result.exception.message.orEmpty()
        )
        else -> this
    }

fun <Data : Any, Result : Any> Intent3<Data, Result>.mapToState(
    stateReducer: IntentContext<State<Data>, Result>.(state: Data) -> State<Data> = {
        state.reduceData { it }
    },
    stateMapper: (State<Data>) -> Data? = { it.data },
    setUp: IntentBuilder<State<Data>, Result>.(innerIntent: Intent3<Data, Result>) -> Unit = { }
): Intent3<State<Data>, Result> = map(
    stateReducer = stateReducer, stateMapper = stateMapper, setUp = setUp
)
