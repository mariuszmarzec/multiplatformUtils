package com.marzec.screen.pickitemscreen

import androidx.compose.runtime.Composable
import com.marzec.content.Content
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.delegate.SelectionDelegate
import com.marzec.delegate.SelectionDelegateImpl
import com.marzec.mvi.delegates
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

data class PickItemOptions<ITEM : Any>(
    val loadData: suspend () -> Flow<Content<List<ITEM>>>,
    val mapItemToId: (ITEM) -> String,
    val itemRow: @Composable (item: ITEM, onItemClick: (ITEM) -> Unit) -> Unit,
    val onAddNavigationAction: (() -> NavigationAction)? = null,
    val multipleChoice: Boolean = false,
    val returnIdOnly: Boolean = true,
    val selected: Set<String> = emptySet(),
    val stringsToCompare: ((ITEM) -> List<String>)? = null,
    val groupByHeader: ((ITEM) -> String)? = null,
    /**
     * Enables incremental results from sources that emit Content.Loading(data).
     * Leave disabled for legacy sources that emit the complete collection at once.
     */
    val supportsPaging: Boolean = false
)

/**
 * Applies one content emission to the picker state.
 *
 * The legacy path deliberately delegates to the original reducer unchanged. Incremental
 * loading is enabled only for callers that explicitly opt in via [PickItemOptions.supportsPaging].
 */
internal fun <ITEM : Any> reducePickItemContent(
    state: State<PickItemData<ITEM>>,
    content: Content<List<ITEM>>,
    options: PickItemOptions<ITEM>
): State<PickItemData<ITEM>> {
    if (!options.supportsPaging) {
        return state.reduceDataWithContent(content) {
            this?.copy(items = it) ?: PickItemData.default(options)
        }
    }

    val currentData = state.data ?: PickItemData.default(options)
    return when (content) {
        is Content.Loading -> {
            val partialItems = content.data
            when {
                partialItems != null -> State.Data(
                    currentData.copy(
                        items = partialItems,
                        isComplete = false,
                        error = null
                    )
                )
                currentData.items.isNotEmpty() -> State.Data(
                    currentData.copy(isComplete = false, error = null)
                )
                else -> State.Loading(currentData.copy(isComplete = false, error = null))
            }
        }
        is Content.Data -> State.Data(
            currentData.copy(
                items = content.data,
                isComplete = true,
                error = null
            )
        )
        is Content.Error -> {
            val message = content.exception.message.orEmpty()
            if (currentData.items.isNotEmpty() && !currentData.isComplete) {
                State.Data(currentData.copy(error = message))
            } else {
                State.Error(currentData, message)
            }
        }
    }
}

fun <ITEM : Any> providePickItemStore(
    navigationStore: NavigationStore,
    options: PickItemOptions<ITEM>,
    scope: CoroutineScope,
    stateCache: StateCache,
    cacheKey: String
): PickItemDataStore<ITEM> = PickItemDataStore(
    options = options,
    scope = scope,
    navigationStore = navigationStore,
    stateCache = stateCache,
    initialState = PickItemData.initial(options),
    cacheKey = cacheKey,
    selectionDelegate = SelectionDelegateImpl<String, PickItemData<ITEM>>(),
    searchDelegate = SearchDelegateImpl<PickItemData<ITEM>>()
)

class PickItemDataStore<ITEM : Any>(
    private val options: PickItemOptions<ITEM>,
    private val navigationStore: NavigationStore,
    scope: CoroutineScope,
    private val stateCache: StateCache,
    initialState: State<PickItemData<ITEM>>,
    private val cacheKey: String,
    private val selectionDelegate: SelectionDelegate<String>,
    private val searchDelegate: SearchDelegate
) : Store4Impl<State<PickItemData<ITEM>>>(
    scope, stateCache.get(cacheKey) ?: initialState
), SelectionDelegate<String> by selectionDelegate,
    SearchDelegate by searchDelegate {

    init {
        delegates(selectionDelegate, searchDelegate)
    }

    override val identifier: Any
        get() = cacheKey

    fun load() = intent<Content<List<ITEM>>> {
        onTrigger {
            options.loadData()
        }

        reducer {
            reducePickItemContent(state, resultNonNull(), options)
        }
    }

    fun onItemClick(item: ITEM) {
        sideEffectIntent {
            if (options.returnIdOnly) {
                navigationStore.goBack(options.mapItemToId(item))
            } else {
                state.ifDataAvailable {
                    navigationStore.goBack(item)
                }
            }
        }
    }

    fun onConfirmClick() = sideEffectIntent {
        state.ifDataAvailable {
            if (options.returnIdOnly) {
                navigationStore.goBack(selected.toList())
            } else {
                val selectedItems = items.filter { options.mapItemToId(it) in selected }
                navigationStore.goBack(selectedItems)
            }
        }
    }

    fun onAddButtonClick() = options.onAddNavigationAction?.let { action ->
        sideEffectIntent {
            navigationStore.next(action())
        }
    }

    override suspend fun onNewState(newState: State<PickItemData<ITEM>>) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}
