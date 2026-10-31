package com.marzec.screen.featuretoggle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.content.mapData
import com.marzec.delegate.DialogState
import com.marzec.model.FeatureToggle
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import com.marzec.featuretoggle.FeatureToggleRepository
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.screen.pickitemscreen.PickItemScreen
import com.marzec.screen.pickitemscreen.providePickItemStore
import com.marzec.view.ActionBarProvider
import com.marzec.view.Dialog
import com.marzec.view.DialogBox
import com.marzec.view.TextFieldStateful
import kotlinx.coroutines.flow.mapLatest

data class FeatureToggles(val options: PickItemOptions<FeatureToggle>) : Destination

data class FeatureToggleDetails(
    val id: Int? = null,
    val name: String = "",
    val value: String = ""
) : Destination

@Composable
fun provideFeatureTogglesScreen(
    actionBarProvider: ActionBarProvider,
    destination: FeatureToggles,
    cacheKey: String,
    navigationStore: NavigationStore,
    stateCache: StateCache,
) =
    PickItemScreen(
        options = destination.options,
        store = providePickItemStore(
            options = destination.options,
            scope = rememberCoroutineScope(),
            cacheKey = cacheKey,
            navigationStore = navigationStore,
            stateCache = stateCache
        ),
        actionBarProvider = actionBarProvider
    )

fun featureTogglePickOptions(
    featureToggleRepository: FeatureToggleRepository,
    navigationStore: NavigationStore
) = PickItemOptions(
    loadData = {
        featureToggleRepository.observeAll()
            .mapLatest { content ->
                content.mapData { featureToggles -> featureToggles.sortedBy { it.name } }
            }
    },
    mapItemToId = { it.id.toString() },
    itemRow = { item: FeatureToggle, _: (FeatureToggle) -> Unit ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    navigationStore.next(NavigationAction(FeatureToggleDetails(item.id)))
                }
                .padding(16.dp)
        ) {
            Text(item.name)
            Spacer(Modifier.weight(1f))
            Text(item.value)
        }

    },
    onAddNavigationAction = { NavigationAction(FeatureToggleDetails()) },
    stringsToCompare = { listOf(it.name) },
    groupByHeader = { featureToggle ->
        featureToggle.name.split(".").takeIf { it.size >= 2 }?.get(0) ?: ""
    }
)

@Composable
fun FeatureToggleScreen(
    store: FeatureToggleStore,
    actionBarProvider: ActionBarProvider
) {

    val state: State<FeatureToggleState> by store.collectState {
        store.initialLoad()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide {
                state.data?.toggle?.let {
                    IconButton({
                        store.showRemoveDialog()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove"
                        )
                    }
                }
            }
        }
    ) {
        when (val state = state) {
            is State.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextFieldStateful(state.data.name, {
                            store.onNameChanged(it)
                        })
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextFieldStateful(state.data.value, {
                            store.onValueChanged(it)
                        })
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ store.save() }) {
                            Text(
                                if (state.data.id != null) {
                                    "Update"
                                } else {
                                    "Create"
                                }
                            )
                        }
                    }
                }
                ShowDialog(store, state)
            }

            is State.Loading -> {
                Text(text = "Loading")
            }

            is State.Error -> {
                Text(text = state.message)
            }
        }
    }
}

@Composable
private fun ShowDialog(store: FeatureToggleStore, state: State.Data<FeatureToggleState>) {
    when (val dialog = state.data.dialog) {
        is DialogState.RemoveDialog -> {
            DialogBox(
                Dialog.TwoOptionsDialog(
                    title = "Remove selected tasks",
                    message = "Do you really want to remove selected tasks?",
                    confirmButton = "Yes",
                    dismissButton = "No",
                    onDismiss = { store.closeDialog() },
                    onConfirm = { store.remove(dialog.idsToRemove) }
                ))
        }

        else -> Unit
    }
}
