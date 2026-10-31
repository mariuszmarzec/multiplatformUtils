package com.marzec.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.marzec.mvi.State

@Composable
fun <T> ScreenWithLoading(
    state: State<T>,
    onReloadClick: (() -> Unit)? = null,
    dataContent: @Composable (State.Data<T>) -> Unit
) {
    when (state) {
        is State.Data -> {
            dataContent(state)
        }
        is State.Loading -> {
            Text(text = "Loading")
        }
        is State.Error -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(text = state.message)
                onReloadClick?.let {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                        Button(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = {
                                onReloadClick()
                            }) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }
}
