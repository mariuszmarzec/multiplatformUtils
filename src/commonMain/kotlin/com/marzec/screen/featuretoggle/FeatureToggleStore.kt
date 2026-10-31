package com.marzec.screen.featuretoggle

import com.marzec.content.Content
import com.marzec.delegate.DialogDelegate
import com.marzec.extensions.asInstance
import com.marzec.model.FeatureToggle
import com.marzec.model.NewFeatureToggle
import com.marzec.model.UpdateFeatureToggle
import com.marzec.model.toUpdate
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.delegates
import com.marzec.mvi.postSideEffect
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.mvi.reduceWithResult
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import com.marzec.featuretoggle.FeatureToggleRepository
import kotlinx.coroutines.CoroutineScope

class FeatureToggleStore(
    scope: CoroutineScope,
    private val args: FeatureToggleDetails,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val initialState: State<FeatureToggleState>,
    private val repository: FeatureToggleRepository,
    private val dialogDelegate: DialogDelegate<Int>
) : Store4Impl<State<FeatureToggleState>>(
    scope, stateCache.get(cacheKey) ?: initialState
), DialogDelegate<Int> by dialogDelegate {

    init {
        delegates(dialogDelegate)
    }

    fun initialLoad() = (stateCache.get(cacheKey) ?: initialState)
        .asInstance<State.Loading<FeatureToggleState>> {
            intent<Content<FeatureToggle>>("load") {
                onTrigger {
                    state.ifDataAvailable(blockOnLoading = false) {
                        id?.let {
                            repository.observeById(it)
                        }
                    }
                }
                reducer {
                    state.reduceWithResult(
                        result = result
                    ) { result ->
                        copy(
                            id = result.data.id,
                            name = result.data.name,
                            value = result.data.value,
                            toggle = result.data
                        )
                    }
                }
            }
        }
    
    fun onNameChanged(name: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(name = name) }
        }
    }

    fun onValueChanged(value: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(value = value) }
        }
    }

    fun save() = intent<Content<FeatureToggle>>("save") {
        onTrigger {
            state.ifDataAvailable {
                if (toggle != null) {
                    repository.update(
                        id = toggle.id,
                        UpdateFeatureToggle(
                            name = name.toUpdate(toggle.name),
                            value = value.toUpdate(toggle.value)
                        )
                    )
                } else {
                    repository.create(
                        NewFeatureToggle(name, value)
                    )
                }
            }
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        postSideEffect {
            navigationStore.goBack()
        }
    }

    fun showRemoveDialog() = sideEffectIntent {
        state.data?.id?.let {
            dialogDelegate.showRemoveDialog(listOf(it))
        }
    }

    fun remove(idsToRemove: List<Int>) = intent<Content<Unit>> {
        onTrigger {
            repository.remove(idsToRemove.first())
        }

        reducer { state.reduceContentToLoadingWithNoChanges(result) }

        postSideEffect { navigationStore.goBack() }
    }

    override suspend fun onNewState(newState: State<FeatureToggleState>) {
        stateCache.set(cacheKey, newState)
    }
}
