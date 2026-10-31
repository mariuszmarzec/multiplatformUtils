package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore

class ActionBarProvider(private val store: NavigationStore) {

    @Composable
    fun provide(
        title: String = "",
        backgroundColor: Color = Color.Transparent,
        backButtonShow: (NavigationState) -> Boolean = { it.backStack.size > 1 },
        rightContent: @Composable () -> Unit = { }
    ) {
        ActionBar(
            backgroundColor = backgroundColor,
            store = store,
            title = title,
            backButtonShow,
            rightContent
        )
    }
}

@Composable
private fun ActionBar(
    backgroundColor: Color = Color.Transparent,
    store: NavigationStore,
    title: String,
    backButtonShow: (NavigationState) -> Boolean = { it.backStack.size > 1 },
    rightContent: @Composable () -> Unit = { },
) {
    val state by store.state.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth().background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (backButtonShow(state)) {
            IconButton({
                store.goBack()
            }) {
                if (state.backStack.size > 1) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                } else {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = title,
        )

        Row(
            horizontalArrangement = Arrangement.End
        ) {
            rightContent()
        }
    }
}
